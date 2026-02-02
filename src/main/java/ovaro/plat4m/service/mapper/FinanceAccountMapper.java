package ovaro.plat4m.service.mapper;

import java.util.*;
import java.util.stream.Collectors;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;
import ovaro.plat4m.domain.Authority;
import ovaro.plat4m.domain.FinanceAccount;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.service.dto.AdminUserDTO;
import ovaro.plat4m.service.dto.FinanceAccountDTO;
import ovaro.plat4m.service.dto.UserDTO;

/**
 * Mapper for the entity {@link Finance} and its DTO called {@link UserDTO}.
 *
 * Normal mappers are generated using MapStruct, this one is hand-coded as MapStruct
 * support is still in beta, and requires a manual step with an IDE.
 */
@Service
public class FinanceAccountMapper {

    public List<FinanceAccountDTO> accountToAccountDTOs(List<FinanceAccount> financeAccounts) {
        return financeAccounts
            .stream()
            .filter(Objects::nonNull)
            .map(this::financeAccountsToFinanceAccountsDTO)
            .collect(Collectors.toList());
    }

    public FinanceAccountDTO financeAccountsToFinanceAccountsDTO(FinanceAccount financeAccount) {
        return new FinanceAccountDTO(financeAccount);
    }
}
