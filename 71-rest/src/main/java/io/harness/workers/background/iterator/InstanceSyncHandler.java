package io.harness.workers.background.iterator;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static software.wings.service.impl.instance.InstanceSyncFlow.ITERATOR;

import com.google.inject.Inject;

import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.workers.background.AccountStatusBasedEntityProcessController;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMapping.InfrastructureMappingKeys;
import software.wings.service.impl.InfraMappingLogContext;
import software.wings.service.impl.instance.InstanceHelper;
import software.wings.service.intfc.AccountService;

/**
 * Handler class that syncs all the instances of an inframapping.
 */

@Slf4j
public class InstanceSyncHandler implements Handler<InfrastructureMapping> {
  @Inject private AccountService accountService;
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private InstanceHelper instanceHelper;
  @Inject private MorphiaPersistenceProvider<InfrastructureMapping> persistenceProvider;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder().name("InstanceSync").poolSize(10).interval(ofSeconds(30)).build(),
        InstanceSyncHandler.class,
        MongoPersistenceIterator.<InfrastructureMapping, MorphiaFilterExpander<InfrastructureMapping>>builder()
            .clazz(InfrastructureMapping.class)
            .fieldName(InfrastructureMappingKeys.nextIteration)
            .targetInterval(ofMinutes(10))
            .acceptableNoAlertDelay(ofMinutes(10))
            .acceptableExecutionTime(ofSeconds(30))
            .handler(this)
            .entityProcessController(new AccountStatusBasedEntityProcessController<>(accountService))
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(InfrastructureMapping infrastructureMapping) {
    try (AutoLogContext ignore1 = new AccountLogContext(infrastructureMapping.getAccountId(), OVERRIDE_ERROR);
         AutoLogContext ignore2 = new InfraMappingLogContext(infrastructureMapping.getUuid(), OVERRIDE_ERROR)) {
      if (instanceHelper.shouldSkipIteratorInstanceSync(infrastructureMapping)) {
        log.info("Skipping Instance Sync for Infrastructure Mapping {}", infrastructureMapping.getUuid());
        return;
      }

      try {
        instanceHelper.syncNow(infrastructureMapping.getAppId(), infrastructureMapping, ITERATOR);
      } catch (Exception ex) {
        log.error("Error while syncing instances for Infrastructure Mapping {}", infrastructureMapping.getUuid(), ex);
      }
    }
  }
}
