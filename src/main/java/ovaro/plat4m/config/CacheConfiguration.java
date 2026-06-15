package ovaro.plat4m.config;

import java.time.Duration;
import org.ehcache.config.builders.*;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.jhipster.config.JHipsterProperties;

@Configuration
@EnableCaching
public class CacheConfiguration {

    public static final String FINANCE_TRANSACTION_EDITOR_CATEGORY_OPTIONS = "financeTransactionEditorCategoryOptions";
    public static final String FINANCE_TRANSACTION_EDITOR_CATEGORY_TREE_OPTIONS = "financeTransactionEditorCategoryTreeOptions";
    public static final String FINANCE_TRANSACTION_EDITOR_WHO_OPTIONS = "financeTransactionEditorWhoOptions";
    public static final String FINANCE_TRANSACTION_EDITOR_WHO_TREE_OPTIONS = "financeTransactionEditorWhoTreeOptions";
    public static final String FINANCE_TRANSACTION_EDITOR_PAYEE_OPTIONS = "financeTransactionEditorPayeeOptions";

    private final javax.cache.configuration.Configuration<Object, Object> jcacheConfiguration;

    public CacheConfiguration(JHipsterProperties jHipsterProperties) {
        var ehcache = jHipsterProperties.getCache().getEhcache();

        jcacheConfiguration = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Object.class,
                Object.class,
                ResourcePoolsBuilder.heap(ehcache.getMaxEntries())
            )
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ehcache.getTimeToLiveSeconds())))
                .build()
        );
    }

    @Bean
    public JCacheManagerCustomizer cacheManagerCustomizer() {
        return cm -> {
            createCache(cm, ovaro.plat4m.repository.UserRepository.USERS_BY_LOGIN_CACHE);
            createCache(cm, ovaro.plat4m.repository.UserRepository.USERS_BY_EMAIL_CACHE);
            createCache(cm, ovaro.plat4m.domain.Authority.class.getName());
            createCache(cm, FINANCE_TRANSACTION_EDITOR_CATEGORY_OPTIONS);
            createCache(cm, FINANCE_TRANSACTION_EDITOR_CATEGORY_TREE_OPTIONS);
            createCache(cm, FINANCE_TRANSACTION_EDITOR_WHO_OPTIONS);
            createCache(cm, FINANCE_TRANSACTION_EDITOR_WHO_TREE_OPTIONS);
            createCache(cm, FINANCE_TRANSACTION_EDITOR_PAYEE_OPTIONS);
            // jhipster-needle-ehcache-add-entry
        };
    }

    private void createCache(javax.cache.CacheManager cm, String cacheName) {
        javax.cache.Cache<Object, Object> cache = cm.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        } else {
            cm.createCache(cacheName, jcacheConfiguration);
        }
    }
}
