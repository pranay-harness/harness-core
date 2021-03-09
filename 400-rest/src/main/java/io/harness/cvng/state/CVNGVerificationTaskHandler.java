package io.harness.cvng.state;

import static io.harness.cvng.state.CVNGVerificationTask.Status.DONE;
import static io.harness.cvng.state.CVNGVerificationTask.Status.TIMED_OUT;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofMinutes;

import io.harness.beans.ExecutionStatus;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.client.CVNGService;
import io.harness.cvng.state.CVNGVerificationTask.CVNGVerificationTaskKeys;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.sm.states.CVNGState.CVNGStateResponseData;
import software.wings.sm.states.CVNGState.CVNGStateResponseData.CVNGStateResponseDataBuilder;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class CVNGVerificationTaskHandler implements MongoPersistenceIterator.Handler<CVNGVerificationTask> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<CVNGVerificationTask> persistenceProvider;
  @Inject private CVNGService cvngService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private CVNGVerificationTaskService cvngVerificationTaskService;
  @Inject private Clock clock;

  public void registerIterator() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("cvngVerificationTask")
            .poolSize(3)
            .interval(Duration.ofSeconds(30))
            .build(),
        CVNGVerificationTaskHandler.class,
        MongoPersistenceIterator.<CVNGVerificationTask, MorphiaFilterExpander<CVNGVerificationTask>>builder()
            .clazz(CVNGVerificationTask.class)
            .fieldName(CVNGVerificationTaskKeys.cvngVerificationTaskIteration)
            .targetInterval(ofMinutes(1))
            .acceptableNoAlertDelay(ofMinutes(1))
            .handler(this)
            .filterExpander(
                query -> query.filter(CVNGVerificationTaskKeys.status, CVNGVerificationTask.Status.IN_PROGRESS))
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }
  @Override
  public void handle(CVNGVerificationTask entity) {
    ActivityStatusDTO activityStatusDTO = cvngService.getActivityStatus(entity.getAccountId(), entity.getActivityId());
    CVNGStateResponseDataBuilder cvngStateResponseData = CVNGStateResponseData.builder()
                                                             .correlationId(entity.getCorrelationId())
                                                             .activityId(entity.getActivityId())
                                                             .activityStatusDTO(activityStatusDTO);
    Instant endTime = entity.getStartTime().plus(Duration.ofMillis(activityStatusDTO.getDurationMs()));
    Instant timeoutCutoff = endTime.plus(Duration.ofMinutes(30));
    if (clock.instant().isAfter(timeoutCutoff)) {
      cvngStateResponseData.status(TIMED_OUT);
      waitNotifyEngine.doneWith(
          entity.getCorrelationId(), cvngStateResponseData.executionStatus(ExecutionStatus.EXPIRED).build());
      cvngVerificationTaskService.markTimedOut(entity.getUuid());
    } else if (ActivityVerificationStatus.getFinalStates().contains(activityStatusDTO.getStatus())) {
      cvngStateResponseData.status(DONE);
      waitNotifyEngine.doneWith(entity.getCorrelationId(), cvngStateResponseData.build());
      cvngVerificationTaskService.markDone(entity.getUuid());
    }
  }
}
