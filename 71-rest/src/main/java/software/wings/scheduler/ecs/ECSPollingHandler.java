package software.wings.scheduler.ecs;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ecs.model.Service;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ecs.ECSPollingJobEntity;
import software.wings.beans.ecs.ECSPollingJobEntity.ECSPollingJobEntityKeys;
import software.wings.service.impl.aws.model.AwsEcsRequest.AwsEcsRequestType;
import software.wings.service.intfc.CloudWatchService;

import java.util.List;

@Slf4j
@Singleton
public class ECSPollingHandler implements Handler<ECSPollingJobEntity> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private CloudWatchService cloudWatchService;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder().name("ECSPollingHandler").poolSize(5).interval(ofHours(2)).build(),
        ECSPollingHandler.class,
        MongoPersistenceIterator.<ECSPollingJobEntity>builder()
            .clazz(ECSPollingJobEntity.class)
            .fieldName(ECSPollingJobEntityKeys.nextIteration)
            .targetInterval(ofMinutes(60))
            .acceptableNoAlertDelay(ofMinutes(1))
            .handler(this)
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(ECSPollingJobEntity entity) {
    try {
      logger.info("Inside ECS polling job entity");
      if (entity.getAwsEcsRequestType() == AwsEcsRequestType.LIST_CLUSTER_SERVICES) {
        List<Service> ecsClusterServices =
            cloudWatchService.getECSClusterServices(entity.getSettingId(), entity.getRegion(), entity.getClusterName());
        logger.info("List cluster services {}", ecsClusterServices.toString());
      }

    } catch (Exception ex) {
      logger.error("Inside Error ", ex);
    }
  }
}
