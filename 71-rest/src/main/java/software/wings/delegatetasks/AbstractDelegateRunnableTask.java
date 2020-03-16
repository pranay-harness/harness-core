package software.wings.delegatetasks;

import static io.harness.exception.WingsException.ExecutionContext.DELEGATE;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.DelegateTaskResponse.DelegateTaskResponseBuilder;
import io.harness.delegate.beans.DelegateTaskResponse.ResponseCode;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData.ErrorNotifyResponseDataBuilder;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.exception.DelegateRetryableException;
import io.harness.delegate.task.DelegateRunnableTask;
import io.harness.delegate.task.TaskLogContext;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.FailureType;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.AccountLogContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.impl.ThirdPartyApiCallLog;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public abstract class AbstractDelegateRunnableTask implements DelegateRunnableTask {
  private String delegateHostname;
  @Getter private String delegateId;
  @Getter private String accountId;
  @Getter private String appId;
  @Getter private String taskId;
  @Getter private String taskType;
  @Getter private boolean isAsync;
  @Getter private Object[] parameters;
  private Consumer<DelegateTaskResponse> consumer;
  private Supplier<Boolean> preExecute;

  @Inject private DataCollectionExecutorService dataCollectionService;

  public AbstractDelegateRunnableTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    this.delegateId = delegateId;
    this.taskId = delegateTask.getUuid();
    this.parameters = delegateTask.getData().getParameters();
    this.accountId = delegateTask.getAccountId();
    this.appId = delegateTask.getAppId();
    this.consumer = consumer;
    this.preExecute = preExecute;
    this.taskType = delegateTask.getData().getTaskType();
    this.isAsync = delegateTask.isAsync();
  }

  @Override
  @SuppressWarnings("PMD")
  public void run() {
    try (TaskLogContext ignore = new TaskLogContext(this.taskId, OVERRIDE_ERROR);
         AccountLogContext ignore2 = new AccountLogContext(this.accountId, OVERRIDE_ERROR)) {
      runDelegateTask();
    } catch (Throwable e) {
      logger.error("Unexpected error executing delegate taskId: [{}] in accountId: [{}]", taskId, accountId, e);
    }
  }

  @SuppressWarnings("PMD")
  private void runDelegateTask() {
    if (!preExecute.get()) {
      logger.info("Pre-execute returned false for task {}", taskId);
      return;
    }

    DelegateMetaInfo delegateMetaInfo = DelegateMetaInfo.builder().hostName(delegateHostname).id(delegateId).build();

    DelegateTaskResponseBuilder taskResponse =
        DelegateTaskResponse.builder().accountId(accountId).responseCode(ResponseCode.OK);

    ErrorNotifyResponseDataBuilder errorNotifyResponseDataBuilder =
        ErrorNotifyResponseData.builder().delegateMetaInfo(delegateMetaInfo);
    try {
      logger.info("Started executing task {}", taskId);

      ResponseData result = parameters.length == 1 && parameters[0] instanceof TaskParameters
          ? run((TaskParameters) parameters[0])
          : run(parameters);

      if (result != null) {
        if (result instanceof DelegateTaskNotifyResponseData) {
          ((DelegateTaskNotifyResponseData) result).setDelegateMetaInfo(delegateMetaInfo);
        } else if (result instanceof RemoteMethodReturnValueData) {
          RemoteMethodReturnValueData returnValueData = (RemoteMethodReturnValueData) result;
          if (returnValueData.getException() instanceof DelegateRetryableException) {
            taskResponse.responseCode(ResponseCode.RETRY_ON_OTHER_DELEGATE);
          }
        } else {
          logger.error("{} does not implement DelegateTaskNotifyResponseData", result.getClass().getName());
        }
        taskResponse.response(result);
      } else {
        String errorMessage = "No response from delegate task " + taskId;
        logger.error(errorMessage);
        taskResponse.response(errorNotifyResponseDataBuilder.failureTypes(EnumSet.of(FailureType.APPLICATION_ERROR))
                                  .errorMessage(errorMessage)
                                  .build());
        taskResponse.responseCode(ResponseCode.FAILED);
      }
      logger.info("Completed executing task {}", taskId);
    } catch (DelegateRetryableException exception) {
      ExceptionLogger.logProcessedMessages(exception, DELEGATE, logger);
      taskResponse.response(errorNotifyResponseDataBuilder.failureTypes(ExceptionUtils.getFailureTypes(exception))
                                .errorMessage(ExceptionUtils.getMessage(exception))
                                .build());
      taskResponse.responseCode(ResponseCode.RETRY_ON_OTHER_DELEGATE);
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, DELEGATE, logger);
      taskResponse.response(errorNotifyResponseDataBuilder.failureTypes(ExceptionUtils.getFailureTypes(exception))
                                .errorMessage(ExceptionUtils.getMessage(exception))
                                .build());
      taskResponse.responseCode(ResponseCode.FAILED);
    } catch (Throwable exception) {
      logger.error(
          format("Unexpected error while executing delegate taskId: [%s] in accountId: [%s]", taskId, accountId),
          exception);
      taskResponse.response(errorNotifyResponseDataBuilder.failureTypes(ExceptionUtils.getFailureTypes(exception))
                                .errorMessage(ExceptionUtils.getMessage(exception))
                                .build());
      taskResponse.responseCode(ResponseCode.FAILED);
    } finally {
      if (consumer != null) {
        consumer.accept(taskResponse.build());
      }
    }
  }

  protected <T> List<Optional<T>> executeParallel(List<Callable<T>> callables) {
    return dataCollectionService.executeParrallel(callables);
  }

  public ThirdPartyApiCallLog createApiCallLog(String stateExecutionId) {
    return ThirdPartyApiCallLog.builder()
        .accountId(getAccountId())
        .delegateId(getDelegateId())
        .delegateTaskId(getTaskId())
        .stateExecutionId(stateExecutionId)
        .build();
  }

  @Override
  public String toString() {
    return "DelegateRunnableTask - " + getTaskType() + " - " + getTaskId() + " - " + (isAsync() ? "async" : "sync");
  }

  public void setDelegateHostname(String delegateHostname) {
    this.delegateHostname = delegateHostname;
  }
}
