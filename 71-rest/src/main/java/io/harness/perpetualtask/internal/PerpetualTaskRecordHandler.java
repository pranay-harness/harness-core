package io.harness.perpetualtask.internal;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.internal.PerpetualTaskRecord.PerpetualTaskRecordKeys;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.intfc.DelegateService;

@Slf4j
public class PerpetualTaskRecordHandler implements Handler<PerpetualTaskRecord> {
  private static final int PERPETUAL_TASK_ASSIGNMENT_INTERVAL_MINUTE = 1;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject DelegateService delegateService;
  @Inject PerpetualTaskService perpetualTaskService;
  @Inject PerpetualTaskServiceClientRegistry clientRegistry;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder()
            .name("PerpetualTaskRecordProcessor")
            .poolSize(3)
            .interval(ofMinutes(PERPETUAL_TASK_ASSIGNMENT_INTERVAL_MINUTE))
            .build(),
        PerpetualTaskRecordHandler.class,
        MongoPersistenceIterator.<PerpetualTaskRecord>builder()
            .clazz(PerpetualTaskRecord.class)
            .fieldName(PerpetualTaskRecordKeys.assignerIteration)
            .targetInterval(ofMinutes(PERPETUAL_TASK_ASSIGNMENT_INTERVAL_MINUTE))
            .acceptableNoAlertDelay(ofSeconds(45))
            .handler(this)
            .filterExpander(query -> query.field(PerpetualTaskRecordKeys.delegateId).equal(""))
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(PerpetualTaskRecord taskRecord) {
    String taskId = taskRecord.getUuid();
    PerpetualTaskType taskType = taskRecord.getPerpetualTaskType();
    logger.info("Assigning Delegate to the inactive {} perpetual task with id={}.", taskType, taskId);
    PerpetualTaskServiceClient client = clientRegistry.getClient(taskRecord.getPerpetualTaskType());
    DelegateTask validationTask = client.getValidationTask(taskRecord.getClientContext(), taskRecord.getAccountId());
    try {
      DelegateTaskNotifyResponseData response = delegateService.executeTask(validationTask);
      String delegateId = response.getDelegateMetaInfo().getId();
      logger.info(
          "Delegate {} is assigned to the inactive {} perpetual task with id={}.", delegateId, taskType, taskId);
      perpetualTaskService.setDelegateId(taskId, delegateId);
    } catch (Exception e) {
      // TODO: add more granular exception handling
      // TODO: add exponential backoff retries
      logger.error("Failed to assign any Delegate to perpetual task {} ", taskId, e);
    }
  }
}
