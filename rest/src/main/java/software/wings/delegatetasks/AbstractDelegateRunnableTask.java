package software.wings.delegatetasks;

import software.wings.waitnotify.NotifyResponseData;

import java.util.function.Consumer;

/**
 * Created by peeyushaggarwal on 12/7/16.
 */
public abstract class AbstractDelegateRunnableTask<T extends NotifyResponseData> implements DelegateRunnableTask<T> {
  private String taskId;
  private Object[] parameters;
  private Consumer<T> consumer;

  public AbstractDelegateRunnableTask(String taskId, Object[] parameters, Consumer<T> consumer) {
    this.taskId = taskId;
    this.parameters = parameters;
    this.consumer = consumer;
  }

  @Override
  public void run() {
    T result = run(parameters);
    if (consumer != null) {
      consumer.accept(result);
    }
  }
}
