package ovaro.plat4m.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ovaro.plat4m.config.CacheConfiguration;
import ovaro.plat4m.domain.FinanceCategory;
import ovaro.plat4m.domain.FinancePayee;
import ovaro.plat4m.repository.FinanceCategoryRepository;
import ovaro.plat4m.repository.FinancePayeeRepository;
import ovaro.plat4m.service.dto.FinanceResourceDTO;
import ovaro.plat4m.service.dto.FinanceTreeNodeDTO;

@Service
public class FinanceTransactionEditorLookupCacheService {

    private static final Integer EDITOR_CATEGORY_CLASSIFICATION_ID = 0;
    private static final Integer EDITOR_WHO_CLASSIFICATION_ID = 1;

    private final FinanceCategoryRepository categoryRepository;
    private final FinancePayeeRepository payeeRepository;

    public FinanceTransactionEditorLookupCacheService(
        FinanceCategoryRepository categoryRepository,
        FinancePayeeRepository payeeRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.payeeRepository = payeeRepository;
    }

    @Cacheable(cacheNames = CacheConfiguration.FINANCE_TRANSACTION_EDITOR_CATEGORY_OPTIONS, key = "#userGuid")
    public List<FinanceResourceDTO> getCategoryOptions(String userGuid) {
        return getCategoryOptionsByClassification(userGuid, EDITOR_CATEGORY_CLASSIFICATION_ID);
    }

    @Cacheable(cacheNames = CacheConfiguration.FINANCE_TRANSACTION_EDITOR_CATEGORY_TREE_OPTIONS, key = "#userGuid")
    public List<FinanceTreeNodeDTO> getCategoryTreeOptions(String userGuid) {
        List<FinanceTreeNodeDTO> nodes = getCategoryTreeOptionsByClassification(userGuid, EDITOR_CATEGORY_CLASSIFICATION_ID);
        nodes.forEach(node -> node.setSelectable(false));
        return nodes;
    }

    @Cacheable(cacheNames = CacheConfiguration.FINANCE_TRANSACTION_EDITOR_WHO_OPTIONS, key = "#userGuid")
    public List<FinanceResourceDTO> getWhoOptions(String userGuid) {
        return getCategoryOptionsByClassification(userGuid, EDITOR_WHO_CLASSIFICATION_ID);
    }

    @Cacheable(cacheNames = CacheConfiguration.FINANCE_TRANSACTION_EDITOR_WHO_TREE_OPTIONS, key = "#userGuid")
    public List<FinanceTreeNodeDTO> getWhoTreeOptions(String userGuid) {
        return getCategoryTreeOptionsByClassification(userGuid, EDITOR_WHO_CLASSIFICATION_ID);
    }

    @Cacheable(cacheNames = CacheConfiguration.FINANCE_TRANSACTION_EDITOR_PAYEE_OPTIONS, key = "#userGuid")
    public List<FinanceResourceDTO> getPayeeOptions(String userGuid) {
        List<FinancePayee> payees = this.payeeRepository.findVisibleByUserGuid(userGuid);
        List<FinanceResourceDTO> options = new ArrayList<>(payees.size());

        for (FinancePayee payee : payees) {
            options.add(new FinanceResourceDTO(payee.getId().toString(), payee.getName()));
        }

        return options;
    }

    @CacheEvict(cacheNames = CacheConfiguration.FINANCE_TRANSACTION_EDITOR_CATEGORY_OPTIONS, key = "#userGuid")
    public void invalidateCategoryOptions(String userGuid) {
        // Cache eviction handled by annotation.
    }

    @CacheEvict(cacheNames = CacheConfiguration.FINANCE_TRANSACTION_EDITOR_CATEGORY_TREE_OPTIONS, key = "#userGuid")
    public void invalidateCategoryTreeOptions(String userGuid) {
        // Cache eviction handled by annotation.
    }

    @CacheEvict(cacheNames = CacheConfiguration.FINANCE_TRANSACTION_EDITOR_WHO_OPTIONS, key = "#userGuid")
    public void invalidateWhoOptions(String userGuid) {
        // Cache eviction handled by annotation.
    }

    @CacheEvict(cacheNames = CacheConfiguration.FINANCE_TRANSACTION_EDITOR_WHO_TREE_OPTIONS, key = "#userGuid")
    public void invalidateWhoTreeOptions(String userGuid) {
        // Cache eviction handled by annotation.
    }

    @CacheEvict(cacheNames = CacheConfiguration.FINANCE_TRANSACTION_EDITOR_PAYEE_OPTIONS, key = "#userGuid")
    public void invalidatePayeeOptions(String userGuid) {
        // Cache eviction handled by annotation.
    }

    private void flattenCategory(
        List<FinanceResourceDTO> output,
        FinanceCategory current,
        String parentPath,
        List<FinanceCategory> allCategories
    ) {
        String path = parentPath == null ? current.getName() : parentPath + " / " + current.getName();
        if (current.getParent() != null) {
            output.add(new FinanceResourceDTO(current.getId().toString(), path));
        }

        for (FinanceCategory category : allCategories) {
            if (category.getParent() != null && category.getParent().getId().equals(current.getId())) {
                flattenCategory(output, category, path, allCategories);
            }
        }
    }

    private List<FinanceResourceDTO> getCategoryOptionsByClassification(String userGuid, Integer classificationId) {
        List<FinanceCategory> categories = this.categoryRepository.findAllByClassificationIdAndUserGuid(classificationId, userGuid);
        categories.sort(Comparator.comparing(FinanceCategory::getName, String.CASE_INSENSITIVE_ORDER));

        List<FinanceResourceDTO> flattenedCategories = new ArrayList<>();
        for (FinanceCategory category : categories) {
            if (category.getParent() == null) {
                flattenCategory(flattenedCategories, category, null, categories);
            }
        }

        return flattenedCategories;
    }

    private List<FinanceTreeNodeDTO> getCategoryTreeOptionsByClassification(String userGuid, Integer classificationId) {
        List<FinanceCategory> categories = this.categoryRepository.findAllByClassificationIdAndUserGuid(classificationId, userGuid);
        categories.sort(Comparator.comparing(FinanceCategory::getName, String.CASE_INSENSITIVE_ORDER));

        return categories
            .stream()
            .filter(category -> category.getParent() == null)
            .map(category -> buildTreeNode(category, categories))
            .collect(Collectors.toList());
    }

    private FinanceTreeNodeDTO buildTreeNode(FinanceCategory category, List<FinanceCategory> allCategories) {
        FinanceTreeNodeDTO node = new FinanceTreeNodeDTO(category.getId().toString(), category.getName());
        List<FinanceTreeNodeDTO> children = allCategories
            .stream()
            .filter(candidate -> candidate.getParent() != null && candidate.getParent().getId().equals(category.getId()))
            .sorted(Comparator.comparing(FinanceCategory::getName, String.CASE_INSENSITIVE_ORDER))
            .map(candidate -> buildTreeNode(candidate, allCategories))
            .collect(Collectors.toList());

        node.setChildren(children);
        node.setLeaf(children.isEmpty());
        return node;
    }
}
