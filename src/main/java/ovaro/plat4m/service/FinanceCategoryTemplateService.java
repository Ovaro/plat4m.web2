package ovaro.plat4m.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ovaro.plat4m.domain.FinanceCategory;
import ovaro.plat4m.domain.User;
import ovaro.plat4m.repository.FinanceCategoryRepository;

@Service
public class FinanceCategoryTemplateService {

    private final FinanceCategoryRepository categoryRepository;

    public FinanceCategoryTemplateService(FinanceCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public void ensureDefaultCategoriesForUser(User user) {
        String userGuid = user.getGuid().toString();
        List<FinanceCategory> templates = this.categoryRepository.findAllByTemplateCategoryTrueOrderByLevelAscNameAsc();
        if (templates.isEmpty()) {
            return;
        }

        List<FinanceCategory> accessibleCategories = this.categoryRepository.findAllByUserGuidOrSystem(userGuid);
        Map<String, FinanceCategory> userCategoriesByKnownKey = new HashMap<>();
        Map<UUID, FinanceCategory> accessibleCategoriesById = new HashMap<>();

        for (FinanceCategory category : accessibleCategories) {
            accessibleCategoriesById.put(category.getId(), category);
            if (userGuid.equals(category.getUserGuid()) && category.getKnownCategoryKey() != null) {
                userCategoriesByKnownKey.putIfAbsent(category.getKnownCategoryKey(), category);
            }
        }

        Map<UUID, FinanceCategory> templateCopiesByTemplateId = new HashMap<>();
        List<FinanceCategory> categoriesToSave = new ArrayList<>();

        for (FinanceCategory template : templates) {
            FinanceCategory existing = userCategoriesByKnownKey.get(template.getKnownCategoryKey());
            if (existing != null) {
                boolean changed = false;
                if (!Boolean.TRUE.equals(existing.getPlatformDefaultCategory())) {
                    existing.setPlatformDefaultCategory(true);
                    changed = true;
                }
                if (!Objects.equals(existing.getTemplateCategoryId(), template.getId())) {
                    existing.setTemplateCategoryId(template.getId());
                    changed = true;
                }
                templateCopiesByTemplateId.put(template.getId(), existing);
                if (changed) {
                    categoriesToSave.add(existing);
                }
                continue;
            }

            FinanceCategory copy = new FinanceCategory();
            copy.setUserGuid(userGuid);
            copy.setName(template.getName());
            copy.setClassificationId(template.getClassificationId());
            copy.setLevel(template.getLevel());
            copy.setComment(template.getComment());
            copy.setSystemCategory(false);
            copy.setKnownCategoryKey(template.getKnownCategoryKey());
            copy.setPlatformDefaultCategory(true);
            copy.setTemplateCategory(false);
            copy.setTemplateCategoryId(template.getId());
            copy.setHidden(false);

            FinanceCategory templateParent = template.getParent();
            if (templateParent != null) {
                FinanceCategory parentCopy = templateCopiesByTemplateId.get(templateParent.getId());
                if (parentCopy == null) {
                    parentCopy = accessibleCategoriesById.get(templateParent.getId());
                }
                copy.setParent(parentCopy);
            }

            FinanceCategory saved = this.categoryRepository.save(copy);
            templateCopiesByTemplateId.put(template.getId(), saved);
            userCategoriesByKnownKey.put(saved.getKnownCategoryKey(), saved);
        }

        if (!categoriesToSave.isEmpty()) {
            this.categoryRepository.saveAll(categoriesToSave);
        }
    }
}
