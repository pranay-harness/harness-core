package io.harness.perpetualtask.internal;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.lang.String.format;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
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
import software.wings.delegatetasks.RemoteMethodReturnValueData;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.perpetualtask.PerpetualTaskCrudObserver;

import javax.ws.rs.ServiceUnavailableException;

@Slf4j
public class PerpetualTaskRecordHandler implements Handler<PerpetualTaskRecord>, PerpetualTaskCrudObserver {
  private static final int PERPETUAL_TASK_ASSIGNMENT_INTERVAL_MINUTE = 1;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private DelegateService delegateService;
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private PerpetualTaskServiceClientRegistry clientRegistry;
  @Inject private MorphiaPersistenceProvider<PerpetualTaskRecord> persistenceProvider;

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
            .fieldName(PerpetualTaskRecordKeys.assignerIteration)
            .targetInterval(ofMinutes(PERPETUAL_TASK_ASSIGNMENT_INTERVAL_MINUTE))
            .acceptableNoAlertDelay(ofSeconds(45))
            .acceptableExecutionTime(ofSeconds(30))
            .handler(this)
            .filterExpander(query -> query.field(PerpetualTaskRecordKeys.delegateId).equal(""))
            .schedulingType(REGULAR)
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
        } else if ((response instanceof RemoteMethodReturnValueData)
            && (((RemoteMethodReturnValueData) response).getException() instanceof InvalidRequestException)) {
          perpetualTaskService.setTaskState(taskId, PerpetualTaskState.NO_ELIGIBLE_DELEGATES.name());
          logger.error("Invalid request exception: ", ((RemoteMethodReturnValueData) response).getException());
        } else {
          logger.error(format(
              "Assignment for perpetual task id=%s got unexpected delegate response %s", taskId, response.toString()));
        }
      } catch (ServiceUnavailableException sue) {
        if (sue.getMessage().contains("Delegates are not available")) {
          perpetualTaskService.setTaskState(taskId, PerpetualTaskState.NO_DELEGATE_AVAILABLE.name());
          logger.warn(sue.getMessage());
        } else {
          logger.error("Failed to assign any Delegate to perpetual task {} ", taskId, sue);
        }

        // TODO: we should not log errors for delegates not being assigned, we need a red bell alert for that.
      } catch (WingsException exception) {
        ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
      } catch (Exception e) {
        logger.error("Failed to assign any Delegate to perpetual task {} ", taskId, e);
      }
    }
  }

  @Override
  public void onPerpetualTaskCreated() {
    if (iterator != null) {
      iterator.wakeup();
    }
  }
}
