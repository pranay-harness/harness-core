package software.wings.scheduler.instance;

import static java.time.Duration.ofMinutes;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.mongo.MongoPersistenceIterator;
import io.harness.mongo.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.impl.instance.InstanceHelper;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Handler class that syncs all the instances of an inframapping.
 * @author rktummala on 02/13/19
 */
@Slf4j
public class InstanceSyncHandler implements Handler<InfrastructureMapping> {
  @Inject private InstanceHelper instanceHelper;

  public static class InstanceSyncExecutor {
    static int POOL_SIZE = 10;
    public static void registerIterators(Injector injector) {
      final ScheduledThreadPoolExecutor executor =
          new ScheduledThreadPoolExecutor(POOL_SIZE, new ThreadFactoryBuilder().setNameFormat("InstanceSync").build());

      final InstanceSyncHandler handler = new InstanceSyncHandler();
      injector.injectMembers(handler);

      PersistenceIterator iterator = MongoPersistenceIterator.<InfrastructureMapping>builder()
                                         .clazz(InfrastructureMapping.class)
                                         .fieldName(ArtifactStream.NEXT_ITERATION_KEY)
                                         .targetInterval(ofMinutes(10))
                                         .acceptableDelay(ofMinutes(10))
                                         .executorService(executor)
                                         .semaphore(new Semaphore(POOL_SIZE))
                                         .handler(handler)
                                         .redistribute(true)
                                         .build();

      injector.injectMembers(iterator);
      executor.scheduleAtFixedRate(() -> iterator.process(ProcessMode.PUMP), 0, 30, TimeUnit.SECONDS);
    }
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
