package software.wings.scheduler.instance;

import static io.harness.mongo.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.MongoPersistenceIterator;
import io.harness.mongo.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMapping.InfrastructureMappingKeys;
import software.wings.service.impl.instance.InstanceHelper;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Handler class that syncs all the instances of an inframapping.
 */

@Slf4j
public class InstanceSyncHandler implements Handler<InfrastructureMapping> {
  private static final int POOL_SIZE = 10;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private InstanceHelper instanceHelper;

  public void registerIterators() {
    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
        POOL_SIZE, new ThreadFactoryBuilder().setNameFormat("Iterator-InstanceSync").build());

    PersistenceIterator iterator =
        persistenceIteratorFactory.create(MongoPersistenceIterator.<InfrastructureMapping>builder()
                                              .clazz(InfrastructureMapping.class)
                                              .fieldName(InfrastructureMappingKeys.nextIteration)
                                              .targetInterval(ofMinutes(10))
                                              .acceptableNoAlertDelay(ofMinutes(10))
                                              .acceptableExecutionTime(ofSeconds(30))
                                              .executorService(executor)
                                              .semaphore(new Semaphore(POOL_SIZE))
                                              .handler(this)
                                              .schedulingType(REGULAR)
                                              .redistribute(true));

    executor.scheduleAtFixedRate(() -> iterator.process(ProcessMode.PUMP), 0, 30, TimeUnit.SECONDS);
  }

  @Override
  public void handle(InfrastructureMapping infrastructureMapping) {
    logger.info("Performing instance sync for infra mapping {}", infrastructureMapping.getUuid());
    try {
      instanceHelper.syncNow(infrastructureMapping.getAppId(), infrastructureMapping);
      logger.info("Performed instance sync for infra mapping {}", infrastructureMapping.getUuid());
    } catch (Exception ex) {
      logger.error("Error while syncing instances for infra mapping {}", infrastructureMapping.getUuid(), ex);
    }
  }
}
