package software.wings.service.impl.trigger;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static software.wings.beans.trigger.Condition.Type.SCHEDULED;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.mongo.MongoPersistenceIterator;
import io.harness.mongo.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.DeploymentTrigger.DeploymentTriggerKeys;
import software.wings.service.intfc.trigger.DeploymentTriggerService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;

@Singleton
@Slf4j
public class ScheduleTriggerHandler implements Handler<DeploymentTrigger> {
  @Inject private DeploymentTriggerService deploymentTriggerService;
  private static int POOL_SIZE = 3;
  private static final ScheduleTriggerHandler handler = new ScheduleTriggerHandler();
  private static ExecutorService executor = Executors.newSingleThreadExecutor();
  private static final ScheduledThreadPoolExecutor executorService = new ScheduledThreadPoolExecutor(
      POOL_SIZE, new ThreadFactoryBuilder().setNameFormat("Iterator-ScheduleTriggerThread").build());

  private static PersistenceIterator<DeploymentTrigger> iterator =
      MongoPersistenceIterator.<DeploymentTrigger>builder()
          .clazz(DeploymentTrigger.class)
          .fieldName("nextIterations")
          .acceptableDelay(ofSeconds(5))
          .maximumDelayForCheck(ofMinutes(30))
          .executorService(executorService)
          .semaphore(new Semaphore(10))
          .handler(handler)
          .filterExpander(query -> query.filter(DeploymentTriggerKeys.type, SCHEDULED))
          .regular(false)
          .build();

  public static void registerIterators(Injector injector) {
    injector.injectMembers(handler);
    injector.injectMembers(iterator);

    executor.submit(() -> iterator.process(ProcessMode.LOOP));
  }

  public void wakeup() {
    iterator.wakeup();
  }
  @Override
  public void handle(DeploymentTrigger entity) {
    try {
      deploymentTriggerService.triggerScheduledExecutionAsync(entity);
    } catch (Exception ex) {
      logger.error("Failed to execute schedule trigger {} from app {}", entity.getName(), entity.getAppId(), ex);
    }
  }
}
