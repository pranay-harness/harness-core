package io.harness.perpetualtask.internal;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.govern.IgnoreThrowable.ignoredOnPurpose;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.IRREGULAR_SKIP_MISSED;
import static java.lang.String.format;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static software.wings.beans.alert.AlertType.PerpetualTaskAlert;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.NoAvaliableDelegatesException;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.internal.PerpetualTaskRecord.PerpetualTaskRecordKeys;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.perpetualtask.PerpetualTaskCrudObserver;

@Slf4j
public class PerpetualTaskRecordHandler implements Handler<PerpetualTaskRecord>, PerpetualTaskCrudObserver {
  public static final String NO_DELEGATE_AVAILABLE_TO_HANDLE_PERPETUAL_TASK =
      "No delegate available to handle perpetual task";

  public static final String SERVICE_UNAVAILABLE =
      "Service Unavailable, failed to assign any Delegate to perpetual task";

  public static final String FAIL_TO_ASSIGN_ANY_DELEGATE_TO_PERPETUAL_TASK =
      "Failed to assign any Delegate to perpetual task";

  public static final String PERPETUAL_TASK_FAILED_TO_BE_ASSIGNED_TO_ANY_DELEGATE =
      "Perpetual task failed to be assigned to any Delegate";

  private static final int PERPETUAL_TASK_ASSIGNMENT_INTERVAL_MINUTE = 1;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private DelegateService delegateService;
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private PerpetualTaskServiceClientRegistry clientRegistry;
  @Inject private MorphiaPersistenceProvider<PerpetualTaskRecord> persistenceProvider;
  @Inject private transient AlertService alertService;

  PersistenceIterator<PerpetualTaskRecord> iterator;

  public void registerIterators() {
    iterator = persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder()
            .name("PerpetualTaskRecordProcessor")
            .poolSize(5)
            .interval(ofMinutes(PERPETUAL_TASK_ASSIGNMENT_INTERVAL_MINUTE))
            .build(),
        PerpetualTaskRecordHandler.class,
        MongoPersistenceIterator.<PerpetualTaskRecord, MorphiaFilterExpander<PerpetualTaskRecord>>builder()
            .clazz(PerpetualTaskRecord.class)
            .fieldName(PerpetualTaskRecordKeys.assignerIterations)
            .targetInterval(ofMinutes(PERPETUAL_TASK_ASSIGNMENT_INTERVAL_MINUTE))
            .acceptableNoAlertDelay(ofSeconds(45))
            .acceptableExecutionTime(ofSeconds(30))
            .handler(this)
            .filterExpander(query -> query.field(PerpetualTaskRecordKeys.delegateId).equal(""))
            .schedulingType(IRREGULAR_SKIP_MISSED)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(PerpetualTaskRecord taskRecord) {
    try (AutoLogContext ignore0 = new AccountLogContext(taskRecord.getAccountId(), OVERRIDE_ERROR)) {
      String taskId = taskRecord.getUuid();
      String taskType = taskRecord.getPerpetualTaskType();
      logger.info("Assigning Delegate to the inactive {} perpetual task with id={}.", taskType, taskId);
      PerpetualTaskServiceClient client = clientRegistry.getClient(taskRecord.getPerpetualTaskType());
      DelegateTask validationTask = client.getValidationTask(taskRecord.getClientContext(), taskRecord.getAccountId());

      try {
        ResponseData response = delegateService.executeTask(validationTask);

        if (response instanceof DelegateTaskNotifyResponseData) {
          String delegateId = ((DelegateTaskNotifyResponseData) response).getDelegateMetaInfo().getId();
          logger.info(
              "Delegate {} is assigned to the inactive {} perpetual task with id={}.", delegateId, taskType, taskId);
          perpetualTaskService.appointDelegate(
              taskRecord.getAccountId(), taskId, delegateId, System.currentTimeMillis());

          alertService.closeAlert(taskRecord.getAccountId(), null, PerpetualTaskAlert,
              software.wings.beans.alert.PerpetualTaskAlert.builder()
                  .accountId(taskRecord.getAccountId())
                  .taskId(taskId)
                  .build());

        } else if ((response instanceof RemoteMethodReturnValueData)
            && (((RemoteMethodReturnValueData) response).getException() instanceof InvalidRequestException)) {
          perpetualTaskService.setTaskState(taskId, PerpetualTaskState.NO_ELIGIBLE_DELEGATES.name());

          raiseAlert(taskRecord, taskId, taskType,
              format(
                  "No eligible delegate to handle perpetual task of %s task type", taskRecord.getPerpetualTaskType()));

          logger.error("Invalid request exception: ", ((RemoteMethodReturnValueData) response).getException());
        } else {
          logger.error(format(
              "Assignment for perpetual task id=%s got unexpected delegate response %s", taskId, response.toString()));
        }
      } catch (NoAvaliableDelegatesException exception) {
        ignoredOnPurpose(exception);
        perpetualTaskService.setTaskState(taskId, PerpetualTaskState.NO_DELEGATE_AVAILABLE.name());
        raiseAlert(taskRecord, taskId, taskType, NO_DELEGATE_AVAILABLE_TO_HANDLE_PERPETUAL_TASK);
      } catch (WingsException exception) {
        raiseAlert(taskRecord, taskId, taskType, PERPETUAL_TASK_FAILED_TO_BE_ASSIGNED_TO_ANY_DELEGATE);
        ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
      } catch (Exception e) {
        raiseAlert(taskRecord, taskId, taskType, FAIL_TO_ASSIGN_ANY_DELEGATE_TO_PERPETUAL_TASK);
        logger.error("Failed to assign any Delegate to perpetual task {} ", taskId, e);
      }
    }
  }

  private void raiseAlert(PerpetualTaskRecord taskRecord, String taskId, String taskType, String message) {
    alertService.openAlert(taskRecord.getAccountId(), null, PerpetualTaskAlert,
        software.wings.beans.alert.PerpetualTaskAlert.builder()
            .accountId(taskRecord.getAccountId())
            .taskId(taskId)
            .perpetualTaskType(taskType)
            .message(message)
            .build());
  }

  @Override
  public void onPerpetualTaskCreated() {
    if (iterator != null) {
      iterator.wakeup();
    }
  }
}
