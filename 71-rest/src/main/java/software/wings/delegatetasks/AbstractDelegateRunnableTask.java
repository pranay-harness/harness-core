
package software.wings.delegatetasks;

import static io.harness.exception.WingsException.ExecutionContext.DELEGATE;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.task.DelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.DelegateRetryableException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.waiter.ErrorNotifyResponseData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.DelegateTaskResponse.DelegateTaskResponseBuilder;
import software.wings.beans.DelegateTaskResponse.ResponseCode;
import software.wings.service.impl.ThirdPartyApiCallLog;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public abstract class AbstractDelegateRunnableTask implements DelegateRunnableTask {
  private String delegateId;
  private String accountId;
  private String appId;
  private String taskId;
  private String taskType;
  private boolean isAsync;
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
    try (TaskLogContext ignore = new TaskLogContext(this.taskId)) {
      runDelegateTask();
    } catch (Throwable e) {
      logger.error(format("Unexpected error executing delegate taskId: [%s] in accountId: [%s]", taskId, accountId), e);
    }
  }

  @SuppressWarnings("PMD")
  private void runDelegateTask() {
    if (!preExecute.get()) {
      logger.info("Pre-execute returned false for task {}", taskId);
      return;
    }

    DelegateTaskResponseBuilder taskResponse =
        DelegateTaskResponse.builder().accountId(accountId).responseCode(ResponseCode.OK);

    try {
      logger.info("Started executing task {}", taskId);

      ResponseData result = parameters.length == 1 && parameters[0] instanceof TaskParameters
          ? run((TaskParameters) parameters[0])
          : run(parameters);

      if (result != null) {
        taskResponse.response(result);
        if (result instanceof RemoteMethodReturnValueData) {
          RemoteMethodReturnValueData returnValueData = (RemoteMethodReturnValueData) result;
          if (returnValueData.getException() instanceof DelegateRetryableException) {
            taskResponse.responseCode(ResponseCode.RETRY_ON_OTHER_DELEGATE);
          }
        }
      } else {
        String errorMessage = "No response from delegate task " + taskId;
        logger.error(errorMessage);
        taskResponse.response(ErrorNotifyResponseData.builder().errorMessage(errorMessage).build());
        taskResponse.responseCode(ResponseCode.FAILED);
      }
      logger.info("Completed executing task {}", taskId);
    } catch (DelegateRetryableException exception) {
      exception.addContext(DelegateTask.class, taskId);
      ExceptionLogger.logProcessedMessages(exception, DELEGATE, logger);
      taskResponse.response(
          ErrorNotifyResponseData.builder().errorMessage(ExceptionUtils.getMessage(exception)).build());
      taskResponse.responseCode(ResponseCode.RETRY_ON_OTHER_DELEGATE);
    } catch (WingsException exception) {
      exception.addContext(DelegateTask.class, taskId);
      ExceptionLogger.logProcessedMessages(exception, DELEGATE, logger);
      taskResponse.response(
          ErrorNotifyResponseData.builder().errorMessage(ExceptionUtils.getMessage(exception)).build());
      taskResponse.responseCode(ResponseCode.FAILED);
    } catch (Throwable exception) {
      logger.error(
          format("Unexpected error while executing delegate taskId: [%s] in accountId: [%s]", taskId, accountId),
          exception);
      taskResponse.response(
          ErrorNotifyResponseData.builder().errorMessage(ExceptionUtils.getMessage(exception)).build());
      taskResponse.responseCode(ResponseCode.FAILED);
    } finally {
      if (consumer != null) {
        consumer.accept(taskResponse.build());
      }
    }
  }

  protected <T> List<Optional<T>> executeParrallel(List<Callable<T>> callables) throws IOException {
    return dataCollectionService.executeParrallel(callables);
  }

  public String getDelegateId() {
    return delegateId;
  }

  public String getTaskId() {
    return taskId;
  }

  public String getAccountId() {
    return accountId;
  }

  public String getAppId() {
    return appId;
  }

  public String getTaskType() {
    return taskType;
  }

  public boolean isAsync() {
    return isAsync;
  }

  public void setAsync(boolean async) {
    isAsync = async;
  }

  public ThirdPartyApiCallLog createApiCallLog(String stateExecutionId) {
    return ThirdPartyApiCallLog.builder()
        .accountId(getAccountId())
        .appId(getAppId())
        .delegateId(getDelegateId())
        .delegateTaskId(getTaskId())
        .stateExecutionId(stateExecutionId)
        .appId(getAppId())
        .build();
  }

  @Override
  public String toString() {
    return "DelegateRunnableTask - " + getTaskType() + " - " + getTaskId() + " - " + (isAsync() ? "async" : "sync");
  }
}
