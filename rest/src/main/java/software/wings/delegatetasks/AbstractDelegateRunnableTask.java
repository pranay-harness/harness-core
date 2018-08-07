
package software.wings.delegatetasks;

import static software.wings.exception.WingsException.ExecutionContext.DELEGATE;

import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.exception.WingsException;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.utils.Misc;
import software.wings.waitnotify.ErrorNotifyResponseData;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by peeyushaggarwal on 12/7/16.
 */
public abstract class AbstractDelegateRunnableTask implements DelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(AbstractDelegateRunnableTask.class);

  private String delegateId;
  private String accountId;
  private String appId;
  private String taskId;
  private String taskType;
  private boolean isAsync;
  private Object[] parameters;
  private Consumer<NotifyResponseData> consumer;
  private Supplier<Boolean> preExecute;

  @Inject private DataCollectionExecutorService dataCollectionService;

  public AbstractDelegateRunnableTask(String delegateId, DelegateTask delegateTask,
      Consumer<NotifyResponseData> consumer, Supplier<Boolean> preExecute) {
    this.delegateId = delegateId;
    this.taskId = delegateTask.getUuid();
    this.parameters = delegateTask.getParameters();
    this.accountId = delegateTask.getAccountId();
    this.appId = delegateTask.getAppId();
    this.consumer = consumer;
    this.preExecute = preExecute;
    this.taskType = delegateTask.getTaskType();
    this.isAsync = delegateTask.isAsync();
  }

  @Override
  @SuppressWarnings("PMD")
  public void run() {
    try (TaskLogContext ignore = new TaskLogContext(this.taskId)) {
      if (preExecute.get()) {
        NotifyResponseData result = null;
        try {
          logger.info("Started executing task {}", taskId);
          result = run(parameters);
          logger.info("Completed executing task {}", taskId);

        } catch (WingsException exception) {
          exception.addContext(DelegateTask.class, taskId);
          WingsExceptionMapper.logProcessedMessages(exception, DELEGATE, logger);
          result = ErrorNotifyResponseData.builder().errorMessage(Misc.getMessage(exception)).build();
        } catch (Throwable exception) {
          logger.error("Unexpected error executing delegate task {}", taskId, exception);
          result = ErrorNotifyResponseData.builder().errorMessage(Misc.getMessage(exception)).build();
        } finally {
          if (consumer != null) {
            if (result == null) {
              String errorMessage = "No response from delegate task " + taskId;
              logger.error(errorMessage);
              result = ErrorNotifyResponseData.builder().errorMessage(errorMessage).build();
            }
            consumer.accept(result);
          }
        }
      } else {
        logger.info("Pre-execute returned false for task {}", taskId);
      }
    } catch (Throwable e) {
      logger.error("Unexpected error executing delegate task {}", taskId, e);
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

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Object[] getParameters() {
    return parameters;
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

  ThirdPartyApiCallLog createApiCallLog(String stateExecutionId) {
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
