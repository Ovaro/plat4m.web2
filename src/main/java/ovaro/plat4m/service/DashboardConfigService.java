package ovaro.plat4m.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovaro.plat4m.domain.DashboardPanelDefinition;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.domain.UserDashboardConfig;
import ovaro.plat4m.repository.DashboardPanelDefinitionRepository;
import ovaro.plat4m.repository.FinanceAccountRepository;
import ovaro.plat4m.repository.FinanceCustomPortfolioRepository;
import ovaro.plat4m.repository.FinanceUserSecurityRepository;
import ovaro.plat4m.repository.UserDashboardConfigRepository;
import ovaro.plat4m.repository.UserRepository;
import ovaro.plat4m.security.SecurityUtils;
import ovaro.plat4m.service.dto.DashboardConfigDTO;
import ovaro.plat4m.service.dto.DashboardLayoutUpdateDTO;
import ovaro.plat4m.service.dto.DashboardPanelDefinitionDTO;
import ovaro.plat4m.service.dto.DashboardWidgetInstanceDTO;
import ovaro.plat4m.service.dto.DataTypeSpecDTO;
import ovaro.plat4m.web.rest.errors.BadRequestAlertException;

@Service
@Transactional
public class DashboardConfigService {

    private static final String ENTITY_NAME = "dashboard";
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    private static final TypeReference<List<DataTypeSpecDTO>> DATA_TYPE_SPEC_LIST = new TypeReference<>() {};
    private static final TypeReference<List<DashboardWidgetInstanceDTO>> WIDGET_LIST = new TypeReference<>() {};

    private final DashboardPanelDefinitionRepository panelDefinitionRepository;
    private final UserDashboardConfigRepository dashboardConfigRepository;
    private final UserRepository userRepository;
    private final FinanceAccountRepository accountRepository;
    private final FinanceCustomPortfolioRepository customPortfolioRepository;
    private final FinanceUserSecurityRepository userSecurityRepository;
    private final ObjectMapper objectMapper;

    public DashboardConfigService(
        DashboardPanelDefinitionRepository panelDefinitionRepository,
        UserDashboardConfigRepository dashboardConfigRepository,
        UserRepository userRepository,
        FinanceAccountRepository accountRepository,
        FinanceCustomPortfolioRepository customPortfolioRepository,
        FinanceUserSecurityRepository userSecurityRepository,
        ObjectMapper objectMapper
    ) {
        this.panelDefinitionRepository = panelDefinitionRepository;
        this.dashboardConfigRepository = dashboardConfigRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.customPortfolioRepository = customPortfolioRepository;
        this.userSecurityRepository = userSecurityRepository;
        this.objectMapper = objectMapper;
    }

    public List<DashboardPanelDefinitionDTO> getPanels() {
        ensureGlobalPanelDefinitions();
        String login = currentLogin();
        return panelDefinitionRepository
            .findByScopeOrUserLoginOrderByNameAsc(DashboardPanelDefinition.SCOPE_GLOBAL, login)
            .stream()
            .map(this::toDto)
            .toList();
    }

    public DashboardConfigDTO getConfig() {
        ensureGlobalPanelDefinitions();
        String login = currentLogin();
        User user = currentUser(login);
        UserDashboardConfig config = dashboardConfigRepository
            .findFirstByUserLoginAndActiveTrueOrderByLastModifiedDateDesc(login)
            .orElseGet(() -> createDefaultDashboard(user));
        config = repairSparseDefaultDashboard(config);
        DashboardConfigDTO dto = toConfigDto(config);
        dto.setPanelDefinitions(getPanels());
        return dto;
    }

    public DashboardConfigDTO updateLayout(DashboardLayoutUpdateDTO update) {
        String login = currentLogin();
        UserDashboardConfig config = dashboardConfigRepository
            .findFirstByUserLoginAndActiveTrueOrderByLastModifiedDateDesc(login)
            .orElseGet(() -> createDefaultDashboard(currentUser(login)));
        List<DashboardWidgetInstanceDTO> widgets = update.getWidgets() == null ? List.of() : update.getWidgets();
        for (DashboardWidgetInstanceDTO widget : widgets) {
            if (widget.getId() == null || widget.getPanelDefinitionId() == null) {
                throw new BadRequestAlertException("Dashboard widget ids are required", ENTITY_NAME, "invalidwidget");
            }
            validateReadablePanel(login, widget.getPanelDefinitionId());
        }
        config.setLayoutJson(writeLayoutJson(widgets));
        DashboardConfigDTO dto = toConfigDto(dashboardConfigRepository.save(config));
        dto.setPanelDefinitions(getPanels());
        return dto;
    }

    public DashboardPanelDefinitionDTO createPanel(DashboardPanelDefinitionDTO dto) {
        validatePanelPayload(dto);
        User user = currentUser(currentLogin());
        DashboardPanelDefinition entity = new DashboardPanelDefinition();
        entity.setId(UUID.randomUUID());
        entity.setScope(DashboardPanelDefinition.SCOPE_USER);
        entity.setUser(user);
        applyPanelPayload(entity, dto);
        return toDto(panelDefinitionRepository.save(entity));
    }

    public DashboardPanelDefinitionDTO updatePanel(UUID id, DashboardPanelDefinitionDTO dto) {
        validatePanelPayload(dto);
        String login = currentLogin();
        Optional<DashboardPanelDefinition> userPanel = panelDefinitionRepository.findOneByIdAndUserLogin(id, login);
        if (userPanel.isPresent()) {
            DashboardPanelDefinition entity = userPanel.get();
            applyPanelPayload(entity, dto);
            return toDto(panelDefinitionRepository.save(entity));
        }

        DashboardPanelDefinition globalPanel = panelDefinitionRepository
            .findById(id)
            .filter(panel -> DashboardPanelDefinition.SCOPE_GLOBAL.equals(panel.getScope()))
            .orElseThrow(() -> new BadRequestAlertException("Dashboard panel not found", ENTITY_NAME, "panelnotfound"));

        DashboardPanelDefinition fork = new DashboardPanelDefinition();
        fork.setId(UUID.randomUUID());
        fork.setScope(DashboardPanelDefinition.SCOPE_USER);
        fork.setUser(currentUser(login));
        fork.setName(dto.getName() != null ? dto.getName().trim() : globalPanel.getName());
        fork.setPanelType(dto.getPanelType() != null ? normalizePanelType(dto.getPanelType()) : globalPanel.getPanelType());
        fork.setConfigJson(writeConfigJson(dto.getDataTypeSpecs()));
        return toDto(panelDefinitionRepository.save(fork));
    }

    private UserDashboardConfig createDefaultDashboard(User user) {
        UserDashboardConfig config = new UserDashboardConfig();
        config.setId(UUID.randomUUID());
        config.setUser(user);
        config.setName("Default");
        config.setActive(true);
        config.setLayoutJson(writeLayoutJson(defaultDashboardWidgets()));
        return dashboardConfigRepository.save(config);
    }

    private void ensureGlobalPanelDefinitions() {
        List<DashboardPanelDefinition> existing = panelDefinitionRepository.findByScopeOrderByNameAsc(
            DashboardPanelDefinition.SCOPE_GLOBAL
        );
        Map<String, DashboardPanelDefinition> existingByName = existing
            .stream()
            .collect(Collectors.toMap(DashboardPanelDefinition::getName, Function.identity(), (left, right) -> left));

        List<DashboardPanelDefinition> panels = new ArrayList<>();
        addGlobalPanelIfMissing(
            existingByName,
            panels,
            globalPanel(
                "Liabilities Summary",
                "wide",
                List.of(
                    spec("All Liabilities", List.of("sumAccountType-loan", "sumAccountType-credit"), "sum", true, false),
                    spec("Mortgage", List.of("sumAccountType-loan"), "sum", true, false),
                    spec("Credit", List.of("sumAccountType-credit"), "sum", true, false)
                )
            )
        );
        addGlobalPanelIfMissing(
            existingByName,
            panels,
            globalPanel(
                "All Banks",
                "standard",
                spec("All Banks", List.of("sumAccountType-bank", "sumAccountType-cash"), "sum", false, false)
            )
        );
        addGlobalPanelIfMissing(
            existingByName,
            panels,
            globalPanel(
                "All Liabilities",
                "standard",
                spec("All Liabilities", List.of("sumAccountType-loan", "sumAccountType-credit"), "sum", true, false)
            )
        );
        addGlobalPanelIfMissing(
            existingByName,
            panels,
            globalPanel("Home Loan", "standard", spec("Home Loan", List.of("sumAccountType-loan"), "sum", true, false))
        );
        addGlobalPanelIfMissing(
            existingByName,
            panels,
            globalPanel("Credit", "standard", spec("Credit", List.of("sumAccountType-credit"), "sum", true, false))
        );
        addGlobalPanelIfMissing(
            existingByName,
            panels,
            globalPanel("Investments", "standard", spec("Investments", List.of("rg/AccountType-investment"), "sum", false, true))
        );
        addGlobalPanelIfMissing(
            existingByName,
            panels,
            globalPanel("Investments Summary", "wide", spec("Investments", List.of("rg/AccountType-investment"), "sum", false, true))
        );
        addGlobalPanelIfMissing(
            existingByName,
            panels,
            globalPanel("All Assets", "wide", spec("All Assets", List.of("sumAccountType-asset"), "sum", false, false))
        );
        addGlobalPanelIfMissing(
            existingByName,
            panels,
            globalPanel("Accounts By Type", "standard", spec("Accounts By Type", List.of("rg/AccountsByType"), "sum", false, true))
        );
        addGlobalPanelIfMissing(
            existingByName,
            panels,
            globalPanel(
                "Netwealth",
                "standard",
                spec(
                    "Netwealth",
                    List.of(
                        "sumAccountType-investment",
                        "sumAccountType-asset",
                        "sumAccountType-cash",
                        "sumAccountType-bank",
                        "sumAccountType-loan",
                        "sumAccountType-credit",
                        "sumAccountType-liability"
                    ),
                    "sum",
                    false,
                    false
                )
            )
        );
        addGlobalPanelIfMissing(
            existingByName,
            panels,
            globalPanel("Netwealth - Detailed", "standard", spec("Netwealth - Detailed", List.of("rg/AccountsByType"), "sum", false, true))
        );

        if (panels.isEmpty()) {
            return;
        }
        panelDefinitionRepository.saveAll(panels);
    }

    private DashboardPanelDefinition globalPanel(String name, String panelType, DataTypeSpecDTO spec) {
        return globalPanel(name, panelType, List.of(spec));
    }

    private DashboardPanelDefinition globalPanel(String name, String panelType, List<DataTypeSpecDTO> specs) {
        DashboardPanelDefinition panel = new DashboardPanelDefinition();
        panel.setId(UUID.randomUUID());
        panel.setScope(DashboardPanelDefinition.SCOPE_GLOBAL);
        panel.setName(name);
        panel.setPanelType(panelType);
        panel.setConfigJson(writeConfigJson(specs));
        return panel;
    }

    private void addGlobalPanelIfMissing(
        Map<String, DashboardPanelDefinition> existingByName,
        List<DashboardPanelDefinition> panels,
        DashboardPanelDefinition panel
    ) {
        if (!existingByName.containsKey(panel.getName())) {
            panels.add(panel);
            existingByName.put(panel.getName(), panel);
        }
    }

    private UserDashboardConfig repairSparseDefaultDashboard(UserDashboardConfig config) {
        List<DashboardWidgetInstanceDTO> widgets = readLayoutJson(config.getLayoutJson());
        boolean hasUserWidget = widgets
            .stream()
            .map(DashboardWidgetInstanceDTO::getPanelDefinitionId)
            .map(panelDefinitionRepository::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .anyMatch(panel -> DashboardPanelDefinition.SCOPE_USER.equals(panel.getScope()));
        boolean sparseBridgeLayout =
            "Default".equals(config.getName()) &&
            !hasUserWidget &&
            (widgets.size() < 10 || widgets.stream().noneMatch(widget -> safeInt(widget.getW(), 1) > 1));
        if (!sparseBridgeLayout) {
            return config;
        }
        config.setLayoutJson(writeLayoutJson(defaultDashboardWidgets()));
        return dashboardConfigRepository.save(config);
    }

    private List<DashboardWidgetInstanceDTO> defaultDashboardWidgets() {
        Map<String, DashboardPanelDefinition> panelsByName = panelDefinitionRepository
            .findByScopeOrderByNameAsc(DashboardPanelDefinition.SCOPE_GLOBAL)
            .stream()
            .collect(Collectors.toMap(DashboardPanelDefinition::getName, Function.identity(), (left, right) -> left));
        List<DashboardWidgetInstanceDTO> widgets = new ArrayList<>();
        addWidget(widgets, panelsByName, "Liabilities Summary", 0, 0, 2, 1, "Liabilities");
        addWidget(widgets, panelsByName, "All Banks", 2, 0, 1, 1, "Banking & Cash");
        addWidget(widgets, panelsByName, "Investments Summary", 4, 0, 2, 1, "Investments");
        addWidget(widgets, panelsByName, "All Assets", 8, 0, 2, 1, "Assets");
        addWidget(widgets, panelsByName, "All Liabilities", 0, 1, 1, 1, "Liabilities");
        addWidget(widgets, panelsByName, "Home Loan", 1, 1, 1, 1, "");
        addWidget(widgets, panelsByName, "Credit", 2, 1, 1, 1, "");
        addWidget(widgets, panelsByName, "Investments", 6, 1, 1, 1, "Investments");
        addWidget(widgets, panelsByName, "Accounts By Type", 0, 2, 1, 1, "");
        addWidget(widgets, panelsByName, "Netwealth", 1, 2, 1, 1, "Netwealth");
        addWidget(widgets, panelsByName, "Netwealth - Detailed", 3, 2, 1, 1, "");
        return widgets;
    }

    private void addWidget(
        List<DashboardWidgetInstanceDTO> widgets,
        Map<String, DashboardPanelDefinition> panelsByName,
        String panelName,
        int x,
        int y,
        int w,
        int h,
        String section
    ) {
        DashboardPanelDefinition panel = panelsByName.get(panelName);
        if (panel == null) {
            return;
        }
        DashboardWidgetInstanceDTO widget = new DashboardWidgetInstanceDTO();
        widget.setId(UUID.randomUUID());
        widget.setPanelDefinitionId(panel.getId());
        widget.setX(x);
        widget.setY(y);
        widget.setW(w);
        widget.setH(h);
        widget.setSection(section);
        widgets.add(widget);
    }

    private int safeInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private DataTypeSpecDTO spec(
        String title,
        List<String> dataType,
        String compositeDataTypeOperation,
        boolean negativeAsPositive,
        boolean expandGroups
    ) {
        DataTypeSpecDTO spec = new DataTypeSpecDTO();
        spec.setTitle(title);
        spec.setDataType(dataType);
        spec.setCompositeDataTypeOperation(compositeDataTypeOperation);
        spec.setNegativeAsPositive(negativeAsPositive);
        spec.setExpandGroups(expandGroups);
        return spec;
    }

    private void applyPanelPayload(DashboardPanelDefinition entity, DashboardPanelDefinitionDTO dto) {
        entity.setName(normalizeName(dto.getName()));
        entity.setPanelType(normalizePanelType(dto.getPanelType()));
        entity.setConfigJson(writeConfigJson(dto.getDataTypeSpecs()));
    }

    private void validatePanelPayload(DashboardPanelDefinitionDTO dto) {
        if (dto == null) {
            throw new BadRequestAlertException("Dashboard panel is required", ENTITY_NAME, "panelrequired");
        }
        normalizeName(dto.getName());
        normalizePanelType(dto.getPanelType());
        if (dto.getDataTypeSpecs() == null || dto.getDataTypeSpecs().isEmpty()) {
            throw new BadRequestAlertException("Dashboard panel data sources are required", ENTITY_NAME, "datasourcerequired");
        }
        String userGuid = currentUser(currentLogin()).getGuid().toString();
        for (DataTypeSpecDTO spec : dto.getDataTypeSpecs()) {
            if (spec.getDataType() == null || spec.getDataType().isEmpty()) {
                throw new BadRequestAlertException("Dashboard panel data sources are required", ENTITY_NAME, "datasourcerequired");
            }
            if (spec.getSummaryChartSpec() != null && Boolean.TRUE.equals(spec.getSummaryChartSpec().getEnabled())) {
                String chartType = spec.getSummaryChartSpec().getChartType();
                if (chartType != null && !List.of("area", "line", "bar").contains(chartType)) {
                    throw new BadRequestAlertException("Dashboard panel summary chart type is invalid", ENTITY_NAME, "invalidsummarychart");
                }
            }
            for (String ref : spec.getDataType()) {
                validateDataRef(userGuid, ref);
            }
        }
    }

    private void validateDataRef(String userGuid, String ref) {
        if (ref == null || ref.isBlank()) {
            throw new BadRequestAlertException("Dashboard panel data source is required", ENTITY_NAME, "datasourcerequired");
        }
        String normalized = ref.trim();
        if (normalized.startsWith("sumAccountType-") || normalized.startsWith("rg/")) {
            return;
        }
        if (normalized.startsWith("childrenOf:")) {
            String childRef = normalized.substring("childrenOf:".length());
            if (UUID_PATTERN.matcher(childRef).matches()) {
                validateOwnedUuid(userGuid, UUID.fromString(childRef));
            }
            return;
        }
        if (normalized.startsWith("customPortfolio:")) {
            String portfolioRef = normalized.substring("customPortfolio:".length());
            if (UUID_PATTERN.matcher(portfolioRef).matches()) {
                validateOwnedCustomPortfolio(userGuid, UUID.fromString(portfolioRef));
                return;
            }
            throw new BadRequestAlertException("Dashboard panel data source is invalid", ENTITY_NAME, "invaliddatasource");
        }
        if (UUID_PATTERN.matcher(normalized).matches()) {
            validateOwnedUuid(userGuid, UUID.fromString(normalized));
            return;
        }
        throw new BadRequestAlertException("Dashboard panel data source is invalid", ENTITY_NAME, "invaliddatasource");
    }

    private void validateOwnedUuid(String userGuid, UUID id) {
        boolean accountOwned = accountRepository.findByIdAndUserGuid(id, userGuid).isPresent();
        boolean securityOwned = userSecurityRepository.findByIdAndUserGuid(id, userGuid).isPresent();
        if (!accountOwned && !securityOwned) {
            throw new BadRequestAlertException("Dashboard panel references an inaccessible resource", ENTITY_NAME, "inaccessibleresource");
        }
    }

    private void validateOwnedCustomPortfolio(String userGuid, UUID id) {
        if (customPortfolioRepository.findByIdAndUserGuid(id, userGuid).isEmpty()) {
            throw new BadRequestAlertException("Dashboard panel references an inaccessible resource", ENTITY_NAME, "inaccessibleresource");
        }
    }

    private void validateReadablePanel(String login, UUID panelDefinitionId) {
        panelDefinitionRepository
            .findById(panelDefinitionId)
            .filter(
                panel ->
                    DashboardPanelDefinition.SCOPE_GLOBAL.equals(panel.getScope()) ||
                    (panel.getUser() != null && login.equals(panel.getUser().getLogin()))
            )
            .orElseThrow(() -> new BadRequestAlertException("Dashboard panel not found", ENTITY_NAME, "panelnotfound"));
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestAlertException("Dashboard panel name is required", ENTITY_NAME, "namerequired");
        }
        return name.trim();
    }

    private String normalizePanelType(String panelType) {
        if (panelType == null || panelType.trim().isEmpty()) {
            throw new BadRequestAlertException("Dashboard panel type is required", ENTITY_NAME, "paneltyperequired");
        }
        String normalized = panelType.trim().toLowerCase(Locale.ROOT);
        if (!List.of("standard", "wide").contains(normalized)) {
            throw new BadRequestAlertException("Unsupported dashboard panel type", ENTITY_NAME, "invalidpaneltype");
        }
        return normalized;
    }

    private DashboardConfigDTO toConfigDto(UserDashboardConfig entity) {
        DashboardConfigDTO dto = new DashboardConfigDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setActive(entity.isActive());
        dto.setWidgets(readLayoutJson(entity.getLayoutJson()));
        return dto;
    }

    private DashboardPanelDefinitionDTO toDto(DashboardPanelDefinition entity) {
        DashboardPanelDefinitionDTO dto = new DashboardPanelDefinitionDTO();
        dto.setId(entity.getId());
        dto.setScope(entity.getScope());
        dto.setName(entity.getName());
        dto.setPanelType(entity.getPanelType());
        dto.setDataTypeSpecs(readConfigJson(entity.getConfigJson()));
        return dto;
    }

    private List<DataTypeSpecDTO> readConfigJson(String json) {
        try {
            return json == null || json.isBlank() ? new ArrayList<>() : objectMapper.readValue(json, DATA_TYPE_SPEC_LIST);
        } catch (JsonProcessingException e) {
            throw new BadRequestAlertException("Dashboard panel configuration is invalid", ENTITY_NAME, "invalidpanelconfig");
        }
    }

    private String writeConfigJson(List<DataTypeSpecDTO> dataTypeSpecs) {
        try {
            return objectMapper.writeValueAsString(dataTypeSpecs == null ? List.of() : dataTypeSpecs);
        } catch (JsonProcessingException e) {
            throw new BadRequestAlertException("Dashboard panel configuration is invalid", ENTITY_NAME, "invalidpanelconfig");
        }
    }

    private List<DashboardWidgetInstanceDTO> readLayoutJson(String json) {
        try {
            return json == null || json.isBlank() ? new ArrayList<>() : objectMapper.readValue(json, WIDGET_LIST);
        } catch (JsonProcessingException e) {
            throw new BadRequestAlertException("Dashboard layout is invalid", ENTITY_NAME, "invalidlayout");
        }
    }

    private String writeLayoutJson(List<DashboardWidgetInstanceDTO> widgets) {
        try {
            return objectMapper.writeValueAsString(widgets == null ? List.of() : widgets);
        } catch (JsonProcessingException e) {
            throw new BadRequestAlertException("Dashboard layout is invalid", ENTITY_NAME, "invalidlayout");
        }
    }

    private String currentLogin() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow(() ->
            new BadRequestAlertException("Current user login not found", ENTITY_NAME, "usernotfound")
        );
    }

    private User currentUser(String login) {
        return userRepository
            .findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("Current user not found", ENTITY_NAME, "usernotfound"));
    }
}
