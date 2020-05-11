package software.wings.service.impl.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.IRREGULAR_SKIP_MISSED;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static software.wings.beans.trigger.Condition.Type.SCHEDULED;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.DeploymentTrigger.DeploymentTriggerKeys;
import software.wings.service.intfc.trigger.DeploymentTriggerService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class ScheduleTriggerHandler implements Handler<DeploymentTrigger> {
  private static final int POOL_SIZE = 3;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private DeploymentTriggerService deploymentTriggerService;

  PersistenceIterator<DeploymentTrigger> iterator;

  private static ExecutorService executor =
      Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("scheduled-trigger-handler").build());
  private static final ScheduledThreadPoolExecutor executorService = new ScheduledThreadPoolExecutor(
      POOL_SIZE, new ThreadFactoryBuilder().setNameFormat("Iterator-ScheduleTriggerThread").build());

  public void registerIterators() {
    iterator = MongoPersistenceIterator.<DeploymentTrigger>builder()
                   .mode(ProcessMode.LOOP)
                   .clazz(DeploymentTrigger.class)
                   .fieldName("nextIterations")
                   .acceptableNoAlertDelay(ofSeconds(5))
                   .maximumDelayForCheck(ofMinutes(30))
                   .executorService(executorService)
                   .semaphore(new Semaphore(10))
                   .handler(this)
                   .filterExpander(query -> query.filter(DeploymentTriggerKeys.type, SCHEDULED))
                   .schedulingType(IRREGULAR_SKIP_MISSED)
                   .throttleInterval(ofSeconds(45))
                   .build();

    executor.submit(() -> iterator.process());
  }

  public void wakeup() {
    if (iterator != null) {
      iterator.wakeup();
    }
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
