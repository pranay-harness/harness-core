package software.wings.delegatetasks;

import software.wings.beans.DelegateTask;
import software.wings.waitnotify.NotifyResponseData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by peeyushaggarwal on 12/7/16.
 */
public abstract class AbstractDelegateRunnableTask<T extends NotifyResponseData> implements DelegateRunnableTask<T> {
  private String delegateId;
  private String accountId;
  private String taskId;
  private Object[] parameters;
  private Consumer<T> consumer;
  private Supplier<Boolean> preExecute;

  public AbstractDelegateRunnableTask(
      String delegateId, DelegateTask delegateTask, Consumer<T> consumer, Supplier<Boolean> preExecute) {
    this.delegateId = delegateId;
    this.taskId = delegateTask.getUuid();
    this.parameters = delegateTask.getParameters();
    this.accountId = delegateTask.getAccountId();
    this.consumer = consumer;
    this.preExecute = preExecute;
  }

  @Override
  public void run() {
    if (preExecute.get()) {
      T result = run(parameters);
      if (consumer != null) {
        consumer.accept(result);
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

  public Object[] getParameters() {
    return parameters;
  }
}
