package io.harness.perpetualtask.internal;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.govern.IgnoreThrowable.ignoredOnPurpose;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.IRREGULAR_SKIP_MISSED;
import static java.lang.String.format;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.alert.AlertType.PerpetualTaskAlert;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;

import io.harness.beans.DelegateTask;
import io.harness.delegate.Capability;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.NoAvailableDelegatesException;
import io.harness.delegate.beans.NoInstalledDelegatesException;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
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
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.internal.PerpetualTaskRecord.PerpetualTaskRecordKeys;
import io.harness.serializer.KryoSerializer;
import io.harness.workers.background.AccountStatusBasedEntityProcessController;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.TaskType;
import software.wings.service.InstanceSyncConstants;
import software.wings.service.impl.PerpetualTaskCapabilityCheckResponse;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.perpetualtask.PerpetualTaskCrudObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PerpetualTaskRecordHandler implements Handler<PerpetualTaskRecord>, PerpetualTaskCrudObserver {
  public static final String NO_DELEGATE_AVAILABLE_TO_HANDLE_PERPETUAL_TASK =
      "No delegate available to handle perpetual task of %s task type";

  public static final String NO_DELEGATES_INSTALLED_TO_HANDLE_PERPETUAL_TASK =
      "No delegates found. Tasks require a delegate to be installed";

  public static final String NO_ELIGIBLE_DELEGATE_TO_HANDLE_PERPETUAL_TASK =
      "No eligible delegate to handle perpetual task of %s task type";

  public static final String FAIL_TO_ASSIGN_ANY_DELEGATE_TO_PERPETUAL_TASK =
      "Failed to assign any Delegate to perpetual task of %s task type";

  public static final String PERPETUAL_TASK_FAILED_TO_BE_ASSIGNED_TO_ANY_DELEGATE =
      "Perpetual task of %s task type failed to be assigned to any Delegate";

  private static final int PERPETUAL_TASK_ASSIGNMENT_INTERVAL_MINUTE = 1;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private DelegateService delegateService;
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private PerpetualTaskServiceClientRegistry clientRegistry;
  @Inject private MorphiaPersistenceProvider<PerpetualTaskRecord> persistenceProvider;
  @Inject private transient AlertService alertService;
  @Inject private AccountService accountService;
  @Inject private KryoSerializer kryoSerializer;

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
            .filterExpander(query
                -> query.field(PerpetualTaskRecordKeys.delegateId)
                       .equal("")
                       .field(PerpetualTaskRecordKeys.state)
                       .notEqual(PerpetualTaskState.TASK_PAUSED))
            .entityProcessController(new AccountStatusBasedEntityProcessController<>(accountService))
            .schedulingType(IRREGULAR_SKIP_MISSED)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(PerpetualTaskRecord taskRecord) {
    try (AutoLogContext ignore0 = new AccountLogContext(taskRecord.getAccountId(), OVERRIDE_ERROR)) {
      String taskId = taskRecord.getUuid();
      logger.info("Assigning Delegate to the inactive {} perpetual task with id={}.", taskRecord.getPerpetualTaskType(),
          taskId);
      DelegateTask validationTask = getValidationTask(taskRecord);

      try {
        DelegateResponseData response = delegateService.executeTask(validationTask);

        if (response instanceof DelegateTaskNotifyResponseData) {
          if (response instanceof PerpetualTaskCapabilityCheckResponse) {
            boolean isAbleToExecutePerpetualTask =
                ((PerpetualTaskCapabilityCheckResponse) response).isAbleToExecutePerpetualTask();
            if (!isAbleToExecutePerpetualTask) {
              perpetualTaskService.setTaskState(taskId, PerpetualTaskState.NO_ELIGIBLE_DELEGATES);

              raiseAlert(
                  taskRecord, format(NO_ELIGIBLE_DELEGATE_TO_HANDLE_PERPETUAL_TASK, taskRecord.getPerpetualTaskType()));

              return;
            }
          }

          String delegateId = ((DelegateTaskNotifyResponseData) response).getDelegateMetaInfo().getId();
          logger.info("Delegate {} is assigned to the inactive {} perpetual task with id={}.", delegateId,
              taskRecord.getPerpetualTaskType(), taskId);
          perpetualTaskService.appointDelegate(
              taskRecord.getAccountId(), taskId, delegateId, System.currentTimeMillis());

          alertService.closeAlert(taskRecord.getAccountId(), null, PerpetualTaskAlert,
              software.wings.beans.alert.PerpetualTaskAlert.builder()
                  .accountId(taskRecord.getAccountId())
                  .perpetualTaskType(taskRecord.getPerpetualTaskType())
                  .build());

        } else if ((response instanceof RemoteMethodReturnValueData)
            && (((RemoteMethodReturnValueData) response).getException() instanceof InvalidRequestException)) {
          perpetualTaskService.setTaskState(taskId, PerpetualTaskState.NO_ELIGIBLE_DELEGATES);

          raiseAlert(
              taskRecord, format(NO_ELIGIBLE_DELEGATE_TO_HANDLE_PERPETUAL_TASK, taskRecord.getPerpetualTaskType()));

          logger.error("Invalid request exception: ", ((RemoteMethodReturnValueData) response).getException());
        } else {
          logger.error(format(
              "Assignment for perpetual task id=%s got unexpected delegate response %s", taskId, response.toString()));
        }
      } catch (NoInstalledDelegatesException exception) {
        ignoredOnPurpose(exception);
        perpetualTaskService.setTaskState(taskId, PerpetualTaskState.NO_DELEGATE_INSTALLED);
        raiseAlert(taskRecord, NO_DELEGATES_INSTALLED_TO_HANDLE_PERPETUAL_TASK);
      } catch (NoAvailableDelegatesException exception) {
        ignoredOnPurpose(exception);
        perpetualTaskService.setTaskState(taskId, PerpetualTaskState.NO_DELEGATE_AVAILABLE);
        raiseAlert(
            taskRecord, format(NO_DELEGATE_AVAILABLE_TO_HANDLE_PERPETUAL_TASK, taskRecord.getPerpetualTaskType()));
      } catch (WingsException exception) {
        raiseAlert(taskRecord,
            format(PERPETUAL_TASK_FAILED_TO_BE_ASSIGNED_TO_ANY_DELEGATE, taskRecord.getPerpetualTaskType()));
        ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
      } catch (Exception e) {
        raiseAlert(
            taskRecord, format(FAIL_TO_ASSIGN_ANY_DELEGATE_TO_PERPETUAL_TASK, taskRecord.getPerpetualTaskType()));

        logger.error("Failed to assign any Delegate to perpetual task {} ", taskId, e);
      }
    }
  }

  protected DelegateTask getValidationTask(PerpetualTaskRecord taskRecord) {
    if (isNotEmpty(taskRecord.getClientContext().getClientParams())) {
      PerpetualTaskServiceClient client = clientRegistry.getClient(taskRecord.getPerpetualTaskType());
      return client.getValidationTask(taskRecord.getClientContext(), taskRecord.getAccountId());
    }

    PerpetualTaskExecutionBundle perpetualTaskExecutionBundle = null;
    try {
      if (taskRecord.getClientContext().getExecutionBundle() == null) {
        return null;
      }
      perpetualTaskExecutionBundle =
          PerpetualTaskExecutionBundle.parseFrom(taskRecord.getClientContext().getExecutionBundle());

    } catch (InvalidProtocolBufferException e) {
      logger.error("Failed to parse perpetual task execution bundle", e);
      return null;
    }

    List<ExecutionCapability> executionCapabilityList = new ArrayList<>();
    List<Capability> capabilityList = perpetualTaskExecutionBundle.getCapabilitiesList();
    if (isNotEmpty(capabilityList)) {
      executionCapabilityList = capabilityList.stream()
                                    .map(capability
                                        -> (ExecutionCapability) kryoSerializer.asInflatedObject(
                                            capability.getKryoCapability().toByteArray()))
                                    .collect(toList());
    }

    return DelegateTask.builder()
        .accountId(taskRecord.getAccountId())
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.CAPABILITY_VALIDATION.name())
                  .parameters(new Object[] {executionCapabilityList.toArray()})
                  .timeout(TimeUnit.MINUTES.toMillis(InstanceSyncConstants.VALIDATION_TIMEOUT_MINUTES))
                  .build())
        .build();
  }

  private void raiseAlert(PerpetualTaskRecord taskRecord, String message) {
    alertService.openAlert(taskRecord.getAccountId(), null, PerpetualTaskAlert,
        software.wings.beans.alert.PerpetualTaskAlert.builder()
            .accountId(taskRecord.getAccountId())
            .perpetualTaskType(taskRecord.getPerpetualTaskType())
            .message(message)
            .description(taskRecord.getTaskDescription())
            .build());
  }

  @Override
  public void onPerpetualTaskCreated() {
    if (iterator != null) {
      iterator.wakeup();
    }
  }
}
