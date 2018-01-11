package software.wings.delegatetasks;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.slf4j.Logger;
import software.wings.beans.DelegateTask;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.StateType;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by rsingh on 9/11/17.
 */
public abstract class AbstractDelegateDataCollectionTask extends AbstractDelegateRunnableTask {
  public static final String HARNESS_HEARTBEAT_METRIC_NAME = "Harness heartbeat metric";
  protected static final int COLLECTION_PERIOD_MINS = 1;
  protected static final int RETRIES = 3;
  protected final AtomicBoolean completed = new AtomicBoolean(false);
  protected final Object lockObject = new Object();
  @Inject protected EncryptionService encryptionService;
  @Inject private ExecutorService executorService;
  @Inject @Named("verificationExecutor") private ScheduledExecutorService verificationExecutor;

  private ScheduledFuture future;
  private volatile Future taskFuture;
  private volatile boolean pendingTask;

  public AbstractDelegateDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<NotifyResponseData> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  protected void waitForCompletion() {
    synchronized (lockObject) {
      try {
        lockObject.wait();
      } catch (InterruptedException e) {
        completed.set(true);
        getLogger().info("{} data collection interrupted", getStateType());
        Thread.currentThread().interrupt();
      }
    }
  }

  protected void shutDownCollection() {
    /* Redundant now, but useful if calling shutDownCollection
     * from the worker threads before the job is aborted
     */
    completed.set(true);
    future.cancel(true);
    synchronized (lockObject) {
      lockObject.notifyAll();
    }
  }

  public DataCollectionTaskResult run(Object[] parameters) {
    try {
      DataCollectionTaskResult taskResult = initDataCollection(parameters);
      if (taskResult.getStatus() == DataCollectionTaskStatus.FAILURE) {
        getLogger().error("Data collection task Init failed");
        return taskResult;
      }

      final Runnable runnable = getDataCollector(taskResult);
      future = verificationExecutor.scheduleAtFixedRate(() -> {
        if (taskFuture == null || taskFuture.isCancelled() || taskFuture.isDone()) {
          taskFuture = executorService.submit(runnable);
        } else {
          if (!pendingTask) {
            executorService.submit(() -> {
              try {
                getLogger().info("queuing pending task");
                taskFuture.get();
                taskFuture = executorService.submit(runnable);
              } catch (Exception e) {
                getLogger().error("Unable to queue collection task ", e);
                shutDownCollection();
              }
              pendingTask = false;
            });
          } else {
            getLogger().info("task already in queue");
          }
          pendingTask = true;
        }
      }, getInitialDelayMinutes(), getPeriodMinutes(), TimeUnit.MINUTES);
      getLogger().info("going to collect data for " + parameters[0]);
      waitForCompletion();
      getLogger().info(" finish data collection for " + parameters[0] + ". result is " + taskResult);
      return taskResult;
    } catch (Exception e) {
      getLogger().error("Data collection task   failed : ", e);
      return DataCollectionTaskResult.builder()
          .status(DataCollectionTaskStatus.FAILURE)
          .stateType(getStateType())
          .errorMessage("Data collection task failed : " + e.getMessage())
          .build();
    }
  }

  protected abstract StateType getStateType();

  protected abstract DataCollectionTaskResult initDataCollection(Object[] parameters);

  protected abstract Logger getLogger();

  protected abstract Runnable getDataCollector(DataCollectionTaskResult taskResult) throws IOException;

  protected int getInitialDelayMinutes() {
    return SplunkDataCollectionTask.DELAY_MINUTES;
  }

  protected int getPeriodMinutes() {
    return COLLECTION_PERIOD_MINS;
  }
}
