package software.wings.delegatetasks;

import static software.wings.waitnotify.ErrorNotifyResponseData.Builder.anErrorNotifyResponseData;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
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

  @Inject @Named("verificationDataCollector") protected ExecutorService dataCollectionService;

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

  protected <T> List<T> executeParrallel(List<Callable<T>> callables) throws IOException {
    CompletionService<T> completionService = new ExecutorCompletionService<>(dataCollectionService);
    logger.info("Parallelizing callables {} ", callables.size());
    for (Callable<T> callable : callables) {
      completionService.submit(callable);
    }

    List<T> rv = new ArrayList<>();
    for (int i = 0; i < callables.size(); i++) {
      try {
        rv.add(completionService.take().get());
      } catch (Exception e) {
        throw new IOException(e);
      }
    }
    logger.info("Done parallelizing callables {} ", callables.size());
    return rv;
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
