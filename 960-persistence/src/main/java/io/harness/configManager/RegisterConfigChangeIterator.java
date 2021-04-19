package io.harness.configManager;

import static io.harness.configManager.Configuration.Builder.aConfiguration;
import static io.harness.configManager.Configuration.MATCH_ALL_VERSION;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofSeconds;

import io.harness.iterator.PersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.persistence.HPersistence;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.mongodb.morphia.query.QueryResults;

public class RegisterConfigChangeIterator {
  public static void registerConfigChangeIterator(Injector injector, HPersistence hPersistence) {
    Configuration configuration = hPersistence.createQuery(Configuration.class).get();
    if (configuration == null) {
      configuration = aConfiguration().withPrimaryVersion(MATCH_ALL_VERSION).build();
      hPersistence.save(configuration);
    }
    ScheduledThreadPoolExecutor configChangeExecutor =
        new ScheduledThreadPoolExecutor(2, new ThreadFactoryBuilder().setNameFormat("config-change-iterator").build());
    ConfigChangeIterator configChangeIterator = injector.getInstance(ConfigChangeIterator.class);

    PersistenceIterator configChangePersistentIterator =
        MongoPersistenceIterator.<Configuration, MorphiaFilterExpander<Configuration>>builder()
            .mode(PersistenceIterator.ProcessMode.PUMP)
            .clazz(Configuration.class)
            .fieldName(Configuration.ConfigurationKeys.nextIteration)
            .targetInterval(ofSeconds(5))
            .acceptableNoAlertDelay(ofSeconds(15))
            .executorService(configChangeExecutor)
            .semaphore(new Semaphore(1))
            .handler(configChangeIterator)
            .schedulingType(REGULAR)
            .filterExpander(QueryResults::get)
            .persistenceProvider(injector.getInstance(MorphiaPersistenceProvider.class))
            .redistribute(true)
            .build();
    injector.injectMembers(configChangePersistentIterator);
    configChangeExecutor.scheduleWithFixedDelay(() -> configChangePersistentIterator.process(), 0, 5, TimeUnit.SECONDS);
  }
}
