package ovaro.plat4m.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import ovaro.plat4m.domain.FinanceAccount;
import ovaro.plat4m.domain.FinanceLot;
import ovaro.plat4m.domain.FinanceUserSecurity;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceAccountRepository;
import ovaro.plat4m.repository.FinanceLotRepository;
import ovaro.plat4m.repository.FinanceUserSecurityRepository;
import ovaro.plat4m.service.dto.FinanceLotGroupDTO;
import ovaro.plat4m.service.dto.FinanceLotViewDTO;

@Service
public class FinanceLotService {

    private final FinanceLotRepository financeLotRepository;
    private final FinanceUserSecurityRepository financeUserSecurityRepository;
    private final FinanceAccountRepository financeAccountRepository;

    public FinanceLotService(
        FinanceLotRepository financeLotRepository,
        FinanceUserSecurityRepository financeUserSecurityRepository,
        FinanceAccountRepository financeAccountRepository
    ) {
        this.financeLotRepository = financeLotRepository;
        this.financeUserSecurityRepository = financeUserSecurityRepository;
        this.financeAccountRepository = financeAccountRepository;
    }

    public List<FinanceLotGroupDTO> getLots(User user, String userSecurityId) {
        FinanceUserSecurity security = this.financeUserSecurityRepository
            .findByIdAndUserGuid(UUID.fromString(userSecurityId), user.getGuid().toString())
            .orElseThrow(() -> new IllegalArgumentException("Investment not found"));

        List<FinanceLot> lots = this.financeLotRepository.findByUserGuidAndSecurityIdOrderByOpenDateAscIdAsc(
            user.getGuid().toString(),
            userSecurityId
        );
        Map<UUID, FinanceLot> lotsById = new HashMap<>();
        for (FinanceLot lot : lots) {
            lotsById.put(lot.getId(), lot);
        }

        Map<String, String> accountNames = new HashMap<>();
        for (FinanceAccount account : this.financeAccountRepository.findAllByUserGuid(user.getGuid().toString())) {
            accountNames.put(account.getId().toString(), account.getName());
        }

        Map<UUID, FinanceLotGroupDTO> groups = new HashMap<>();
        for (FinanceLot lot : lots) {
            FinanceLot root = resolveRootLot(lot, lotsById);
            FinanceLotGroupDTO group = groups.get(root.getId());
            if (group == null) {
                group = new FinanceLotGroupDTO();
                group.setOriginalLot(toView(root, accountNames, security));
                groups.put(root.getId(), group);
            }
            group.getLots().add(toView(lot, accountNames, security));
            if (lot.getCloseDate() == null) {
                FinanceLotViewDTO remaining = toView(lot, accountNames, security);
                if (group.getRemainingLot() == null || compareLotViews(remaining, group.getRemainingLot()) > 0) {
                    group.setRemainingLot(remaining);
                }
            }
        }

        List<FinanceLotGroupDTO> results = new ArrayList<>(groups.values());
        for (FinanceLotGroupDTO group : results) {
            group
                .getLots()
                .sort(
                    Comparator.comparing(FinanceLotViewDTO::getOpenDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(FinanceLotViewDTO::getBuyDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(FinanceLotViewDTO::getId, Comparator.nullsLast(Comparator.naturalOrder()))
                );
        }
        results.sort(
            Comparator.comparing(
                (FinanceLotGroupDTO group) -> group.getOriginalLot().getOpenDate(),
                Comparator.nullsLast(Comparator.naturalOrder())
            )
                .thenComparing(group -> group.getOriginalLot().getBuyDate(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(group -> group.getOriginalLot().getId(), Comparator.nullsLast(Comparator.naturalOrder()))
        );
        return results;
    }

    private int compareLotViews(FinanceLotViewDTO left, FinanceLotViewDTO right) {
        Comparator<FinanceLotViewDTO> comparator = Comparator.comparing(
            FinanceLotViewDTO::getOpenDate,
            Comparator.nullsLast(Comparator.naturalOrder())
        )
            .thenComparing(FinanceLotViewDTO::getBuyDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(FinanceLotViewDTO::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        return comparator.compare(left, right);
    }

    private FinanceLot resolveRootLot(FinanceLot lot, Map<UUID, FinanceLot> lotsById) {
        FinanceLot current = lot;
        while (current.getLotOpenId() != null) {
            FinanceLot parent = lotsById.get(current.getLotOpenId());
            if (parent == null || parent.getId().equals(current.getId())) {
                break;
            }
            current = parent;
        }
        return current;
    }

    private FinanceLotViewDTO toView(FinanceLot lot, Map<String, String> accountNames, FinanceUserSecurity security) {
        FinanceLotViewDTO dto = new FinanceLotViewDTO();
        dto.setId(lot.getId().toString());
        dto.setQuantity(lot.getQuantity());
        dto.setLotType(lot.getLotType());
        dto.setAccountId(lot.getAccountId());
        dto.setAccountName(lot.getAccountId() != null ? accountNames.get(lot.getAccountId()) : null);
        dto.setSecurityId(lot.getSecurityId());
        dto.setSecurityName(security.getName());
        dto.setBuyDate(lot.getBuyDate());
        dto.setSellDate(lot.getSellDate());
        dto.setOpenDate(lot.getOpenDate());
        dto.setCloseDate(lot.getCloseDate());
        dto.setBuyTransactionId(lot.getBuyTransactionId() != null ? lot.getBuyTransactionId().toString() : null);
        dto.setSellTransactionId(lot.getSellTransactionId() != null ? lot.getSellTransactionId().toString() : null);
        dto.setOpenTransactionId(lot.getOpenTransactionId() != null ? lot.getOpenTransactionId().toString() : null);
        dto.setCloseTransactionId(lot.getCloseTransactionId() != null ? lot.getCloseTransactionId().toString() : null);
        return dto;
    }
}
