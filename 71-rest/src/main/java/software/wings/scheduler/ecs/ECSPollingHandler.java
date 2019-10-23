package software.wings.scheduler.ecs;

import static io.harness.mongo.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ecs.model.Service;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.MongoPersistenceIterator;
import io.harness.mongo.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ecs.ECSPollingJobEntity;
import software.wings.beans.ecs.ECSPollingJobEntity.ECSPollingJobEntityKeys;
import software.wings.service.impl.aws.model.AwsEcsRequest.AwsEcsRequestType;
import software.wings.service.intfc.CloudWatchService;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class ECSPollingHandler implements Handler<ECSPollingJobEntity> {
  private static final int POOL_SIZE = 5;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private CloudWatchService cloudWatchService;

  public void registerIterators() {
    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
        POOL_SIZE, new ThreadFactoryBuilder().setNameFormat("Iterator-ECSPollingHandler").build());

    Semaphore semaphore = new Semaphore(POOL_SIZE);
    PersistenceIterator iterator =
        persistenceIteratorFactory.create(MongoPersistenceIterator.<ECSPollingJobEntity>builder()
                                              .clazz(ECSPollingJobEntity.class)
                                              .fieldName(ECSPollingJobEntityKeys.nextIteration)
                                              .targetInterval(ofMinutes(60))
                                              .acceptableNoAlertDelay(ofMinutes(1))
                                              .executorService(executor)
                                              .semaphore(semaphore)
                                              .handler(this)
                                              .schedulingType(REGULAR)
                                              .redistribute(true));

    executor.scheduleAtFixedRate(() -> iterator.process(ProcessMode.PUMP), 0, 120, TimeUnit.MINUTES);
  }

  @Override
  public void handle(ECSPollingJobEntity entity) {
    try {
      logger.info("Inside ECS polling job entity");
      if (entity.getAwsEcsRequestType().equals(AwsEcsRequestType.LIST_CLUSTER_SERVICES)) {
        List<Service> ecsClusterServices =
            cloudWatchService.getECSClusterServices(entity.getSettingId(), entity.getRegion(), entity.getClusterName());
        logger.info("List cluster services {}", ecsClusterServices.toString());
      }

    } catch (Exception ex) {
      logger.error("Inside Error ", ex);
    }
  }
}
