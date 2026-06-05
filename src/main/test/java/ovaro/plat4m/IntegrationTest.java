package ovaro.plat4m;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import ovaro.plat4m.config.AsyncSyncConfiguration;
import ovaro.plat4m.config.DatabaseTestcontainer;
import ovaro.plat4m.config.JacksonConfiguration;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = {
        Plat4MApp.class,
        JacksonConfiguration.class,
        AsyncSyncConfiguration.class,
        ovaro.plat4m.config.JacksonHibernateConfiguration.class,
    }
)
@ImportTestcontainers(DatabaseTestcontainer.class)
public @interface IntegrationTest {}
