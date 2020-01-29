package io.harness.workers.background.iterator;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;

import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.logging.AutoLogContext;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMapping.InfrastructureMappingKeys;
import software.wings.service.impl.InfraMappingLogContext;
import software.wings.service.impl.instance.InstanceHelper;

/**
 * Handler class that syncs all the instances of an inframapping.
 */

@Slf4j
public class InstanceSyncHandler implements Handler<InfrastructureMapping> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private InstanceHelper instanceHelper;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder().name("InstanceSync").poolSize(10).interval(ofSeconds(30)).build(),
        InstanceSyncHandler.class,
        MongoPersistenceIterator.<InfrastructureMapping>builder()
            .clazz(InfrastructureMapping.class)
            .fieldName(InfrastructureMappingKeys.nextIteration)
            .targetInterval(ofMinutes(10))
            .acceptableNoAlertDelay(ofMinutes(10))
            .acceptableExecutionTime(ofSeconds(30))
            .handler(this)
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(InfrastructureMapping infrastructureMapping) {
    try (AutoLogContext ignore1 = new AccountLogContext(infrastructureMapping.getAccountId(), OVERRIDE_ERROR);
         AutoLogContext ignore2 = new InfraMappingLogContext(infrastructureMapping.getUuid(), OVERRIDE_ERROR)) {
      logger.info("Performing instance sync for infra mapping {}", infrastructureMapping.getUuid());
      try {
        instanceHelper.syncNow(infrastructureMapping.getAppId(), infrastructureMapping);
        logger.info("Performed instance sync for infra mapping {}", infrastructureMapping.getUuid());
      } catch (Exception ex) {
        logger.error("Error while syncing instances for infra mapping {}", infrastructureMapping.getUuid(), ex);
      }
    }
  }
}
