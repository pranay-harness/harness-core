package software.wings.delegatetasks;

import static software.wings.waitnotify.ErrorNotifyResponseData.Builder.anErrorNotifyResponseData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.waitnotify.NotifyResponseData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by peeyushaggarwal on 12/7/16.
 */
public abstract class AbstractDelegateRunnableTask implements DelegateRunnableTask {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private String delegateId;
  private String accountId;
  private String appId;
  private String taskId;
  private Object[] parameters;
  private Consumer<NotifyResponseData> consumer;
  private Supplier<Boolean> preExecute;

  public AbstractDelegateRunnableTask(String delegateId, DelegateTask delegateTask,
      Consumer<NotifyResponseData> consumer, Supplier<Boolean> preExecute) {
    this.delegateId = delegateId;
    this.taskId = delegateTask.getUuid();
    this.parameters = delegateTask.getParameters();
    this.accountId = delegateTask.getAccountId();
    this.appId = delegateTask.getAppId();
    this.consumer = consumer;
    this.preExecute = preExecute;
  }

  @Override
  public void run() {
    if (preExecute.get()) {
      NotifyResponseData result = null;
      try {
        result = run(parameters);
      } catch (Throwable t) {
        logger.error("Unexpected error executing delegate task.", t);
        result = anErrorNotifyResponseData().withErrorMessage(t.getMessage()).build();
      } finally {
        if (consumer != null) {
          if (result == null) {
            logger.error("Null result executing delegate task.");
            result = anErrorNotifyResponseData().withErrorMessage("No response from delegate task.").build();
          }
          consumer.accept(result);
        }
      }
    }
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

  public Object[] getParameters() {
    return parameters;
  }
}
