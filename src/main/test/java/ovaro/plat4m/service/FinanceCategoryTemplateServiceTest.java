package ovaro.plat4m.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ovaro.plat4m.domain.FinanceCategory;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceCategoryRepository;

@ExtendWith(MockitoExtension.class)
class FinanceCategoryTemplateServiceTest {

    @Mock
    private FinanceCategoryRepository categoryRepository;

    @Test
    void ensureDefaultCategoriesForUserCreatesUserOwnedCopiesFromTemplates() {
        FinanceCategory systemExpense = category(UUID.fromString("00000000-0000-0000-0000-000000000102"), "EXPENSE", null);
        systemExpense.setSystemCategory(true);

        FinanceCategory templateGroceries = category(UUID.randomUUID(), "Food & Groceries", systemExpense);
        templateGroceries.setKnownCategoryKey("EXPENSE/FOOD & GROCERIES");
        templateGroceries.setPlatformDefaultCategory(true);
        templateGroceries.setTemplateCategory(true);

        User user = user();
        when(categoryRepository.findAllByTemplateCategoryTrueOrderByLevelAscNameAsc()).thenReturn(List.of(templateGroceries));
        when(categoryRepository.findAllByUserGuidOrSystem(user.getGuid().toString())).thenReturn(List.of(systemExpense));
        when(categoryRepository.save(any(FinanceCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new FinanceCategoryTemplateService(categoryRepository).ensureDefaultCategoriesForUser(user);

        ArgumentCaptor<FinanceCategory> captor = ArgumentCaptor.forClass(FinanceCategory.class);
        verify(categoryRepository).save(captor.capture());
        FinanceCategory copy = captor.getValue();
        assertThat(copy.getUserGuid()).isEqualTo(user.getGuid().toString());
        assertThat(copy.getName()).isEqualTo("Food & Groceries");
        assertThat(copy.getParent()).isSameAs(systemExpense);
        assertThat(copy.getKnownCategoryKey()).isEqualTo("EXPENSE/FOOD & GROCERIES");
        assertThat(copy.getPlatformDefaultCategory()).isTrue();
        assertThat(copy.getTemplateCategory()).isFalse();
        assertThat(copy.getTemplateCategoryId()).isEqualTo(templateGroceries.getId());
        assertThat(copy.getHidden()).isFalse();
    }

    @Test
    void ensureDefaultCategoriesForUserPreservesRenamedAndMovedKnownCategories() {
        User user = user();
        FinanceCategory systemIncome = category(UUID.fromString("00000000-0000-0000-0000-000000000101"), "INCOME", null);
        systemIncome.setSystemCategory(true);

        FinanceCategory customParent = category(UUID.randomUUID(), "Investments", systemIncome);
        customParent.setUserGuid(user.getGuid().toString());

        FinanceCategory templateCapitalGains = category(UUID.randomUUID(), "Capital Gains", systemIncome);
        templateCapitalGains.setKnownCategoryKey("INCOME/INVESTMENT INCOME/CAPITAL GAINS");
        templateCapitalGains.setPlatformDefaultCategory(true);
        templateCapitalGains.setTemplateCategory(true);

        FinanceCategory existing = category(UUID.randomUUID(), "Share Sale Profit", customParent);
        existing.setUserGuid(user.getGuid().toString());
        existing.setKnownCategoryKey(templateCapitalGains.getKnownCategoryKey());
        existing.setHidden(true);

        when(categoryRepository.findAllByTemplateCategoryTrueOrderByLevelAscNameAsc()).thenReturn(List.of(templateCapitalGains));
        when(categoryRepository.findAllByUserGuidOrSystem(user.getGuid().toString())).thenReturn(
            List.of(systemIncome, customParent, existing)
        );

        new FinanceCategoryTemplateService(categoryRepository).ensureDefaultCategoriesForUser(user);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FinanceCategory>> captor = ArgumentCaptor.forClass(List.class);
        verify(categoryRepository).saveAll(captor.capture());
        FinanceCategory updated = captor.getValue().getFirst();
        assertThat(updated.getName()).isEqualTo("Share Sale Profit");
        assertThat(updated.getParent()).isSameAs(customParent);
        assertThat(updated.getKnownCategoryKey()).isEqualTo(templateCapitalGains.getKnownCategoryKey());
        assertThat(updated.getPlatformDefaultCategory()).isTrue();
        assertThat(updated.getTemplateCategoryId()).isEqualTo(templateCapitalGains.getId());
        assertThat(updated.getHidden()).isTrue();
    }

    private User user() {
        User user = new User();
        user.setGuid(UUID.randomUUID());
        return user;
    }

    private FinanceCategory category(UUID id, String name, FinanceCategory parent) {
        FinanceCategory category = new FinanceCategory();
        category.setId(id);
        category.setName(name);
        category.setParent(parent);
        category.setClassificationId(0);
        category.setLevel(parent == null ? 0 : parent.getLevel() + 1);
        return category;
    }
}
