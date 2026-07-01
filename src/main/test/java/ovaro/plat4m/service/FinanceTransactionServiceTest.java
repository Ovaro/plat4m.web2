package ovaro.plat4m.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import ovaro.plat4m.domain.FinanceCategory;
import ovaro.plat4m.domain.FinancePayee;
import ovaro.plat4m.domain.FinanceTransaction;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceAccountRepository;
import ovaro.plat4m.repository.FinanceCategoryRepository;
import ovaro.plat4m.repository.FinancePayeeRepository;
import ovaro.plat4m.repository.FinanceTagRepository;
import ovaro.plat4m.repository.FinanceTransactionRepository;
import ovaro.plat4m.repository.FinanceTransactionTagRepository;
import ovaro.plat4m.repository.FinanceTransferLinkRepository;
import ovaro.plat4m.repository.SourceLinkRepository;
import ovaro.plat4m.service.dto.FinanceManagePayeeDTO;
import ovaro.plat4m.service.dto.FinanceResourceDTO;

@ExtendWith(MockitoExtension.class)
class FinanceTransactionServiceTest {

    @Mock
    private FinanceTransactionRepository transactionRepository;

    @Mock
    private FinanceTransferLinkRepository transferLinkRepository;

    @Mock
    private FinanceCategoryRepository categoryRepository;

    @Mock
    private FinanceAccountRepository accountRepository;

    @Mock
    private FinancePayeeRepository payeeRepository;

    @Mock
    private FinanceTagRepository tagRepository;

    @Mock
    private FinanceTransactionTagRepository transactionTagRepository;

    @Mock
    private SourceLinkRepository sourceLinkRepository;

    @Mock
    private FinanceTransactionEditorLookupCacheService editorLookupCacheService;

    private FinanceTransactionService service;

    @BeforeEach
    void setUp() {
        service = new FinanceTransactionService(
            transactionRepository,
            transferLinkRepository,
            categoryRepository,
            accountRepository,
            payeeRepository,
            tagRepository,
            transactionTagRepository,
            sourceLinkRepository,
            editorLookupCacheService
        );
    }

    @Test
    void getManagedPayeesReturnsVariantSummaries() {
        User user = new User();
        UUID userGuid = UUID.randomUUID();
        user.setGuid(userGuid);

        FinancePayee parent = payee(UUID.randomUUID(), userGuid, "Woolworths", null, false);
        FinancePayee childOne = payee(UUID.randomUUID(), userGuid, "Woolies Metro", parent.getId().toString(), false);
        FinancePayee childTwo = payee(UUID.randomUUID(), userGuid, "Woolworths Online", parent.getId().toString(), true);
        FinancePayee sibling = payee(UUID.randomUUID(), userGuid, "Telstra", null, false);

        when(payeeRepository.findByUserGuidOrderByNameAsc(userGuid.toString())).thenReturn(List.of(parent, childOne, childTwo, sibling));

        List<FinanceManagePayeeDTO> payees = service.getManagedPayees(user, true);

        FinanceManagePayeeDTO mappedParent = payees
            .stream()
            .filter(payee -> payee.getId().equals(parent.getId().toString()))
            .findFirst()
            .orElseThrow();
        assertThat(mappedParent.getChildCount()).isEqualTo(2);
        assertThat(mappedParent.getChildNames()).containsExactly("Woolies Metro", "Woolworths Online");
    }

    @Test
    void deleteManagedPayeeRejectsParentsWithActiveVariants() {
        User user = new User();
        UUID userGuid = UUID.randomUUID();
        user.setGuid(userGuid);

        FinancePayee parent = payee(UUID.randomUUID(), userGuid, "Woolworths", null, false);
        when(payeeRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(payeeRepository.existsVisibleChildByUserGuidAndParentId(userGuid.toString(), parent.getId().toString())).thenReturn(true);

        assertThatThrownBy(() -> service.deleteManagedPayee(user, parent.getId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Payee has active variants. Hide or reassign them first.");

        verify(payeeRepository, never()).save(parent);
    }

    @Test
    void deleteManagedPayeeHidesChildVariants() {
        User user = new User();
        UUID userGuid = UUID.randomUUID();
        user.setGuid(userGuid);

        UUID parentId = UUID.randomUUID();
        FinancePayee child = payee(UUID.randomUUID(), userGuid, "Woolies Metro", parentId.toString(), false);
        when(payeeRepository.findById(child.getId())).thenReturn(Optional.of(child));
        when(payeeRepository.existsVisibleChildByUserGuidAndParentId(userGuid.toString(), child.getId().toString())).thenReturn(false);

        service.deleteManagedPayee(user, child.getId());

        assertThat(child.getHidden()).isTrue();
        verify(payeeRepository).save(child);
        verify(editorLookupCacheService).invalidatePayeeOptions(userGuid.toString());
    }

    @Test
    void getLastCategoryForPayeeReturnsMostRecentMatchingCategory() {
        User user = new User();
        UUID userGuid = UUID.randomUUID();
        user.setGuid(userGuid);

        FinancePayee parent = payee(UUID.randomUUID(), userGuid, "Woolworths", null, false);
        FinancePayee child = payee(UUID.randomUUID(), userGuid, "Woolies Metro", parent.getId().toString(), false);
        FinanceCategory parentCategory = new FinanceCategory();
        parentCategory.setId(UUID.randomUUID());
        parentCategory.setName("Living");
        FinanceCategory category = new FinanceCategory();
        category.setId(UUID.randomUUID());
        category.setName("Groceries");
        category.setParent(parentCategory);
        FinanceTransaction transaction = new FinanceTransaction();
        transaction.setPayeeId(child.getId().toString());
        transaction.setCategory(category);
        transaction.setAmount(BigDecimal.valueOf(-42.50));

        when(payeeRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(payeeRepository.findByUserGuidOrderByNameAsc(userGuid.toString())).thenReturn(List.of(parent, child));
        when(
            transactionRepository.findRecentCategorisedByUserGuidAndPayeeIdsAndDirection(
                userGuid.toString(),
                List.of(parent.getId().toString(), child.getId().toString()),
                false,
                Pageable.ofSize(1)
            )
        ).thenReturn(List.of(transaction));

        FinanceResourceDTO suggestion = service.getLastCategoryForPayee(user, parent.getId(), "withdrawal");

        assertThat(suggestion).isNotNull();
        assertThat(suggestion.getId()).isEqualTo(category.getId().toString());
        assertThat(suggestion.getName()).isEqualTo("Living: Groceries");
    }

    private FinancePayee payee(UUID id, UUID userGuid, String name, String parentId, boolean hidden) {
        FinancePayee payee = new FinancePayee();
        payee.setId(id);
        payee.setUserGuid(userGuid.toString());
        payee.setName(name);
        payee.setParentId(parentId);
        payee.setHidden(hidden);
        return payee;
    }
}
