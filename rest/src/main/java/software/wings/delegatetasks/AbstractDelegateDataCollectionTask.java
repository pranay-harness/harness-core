package software.wings.delegatetasks;

import org.slf4j.Logger;
import software.wings.beans.DelegateTask;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by rsingh on 9/11/17.
 */
public abstract class AbstractDelegateDataCollectionTask
    extends AbstractDelegateRunnableTask<DataCollectionTaskResult> {
  protected static final int RETRIES = 3;
  protected final AtomicBoolean completed = new AtomicBoolean(false);
  protected ScheduledExecutorService collectionService;
  protected final Object lockObject = new Object();

  public AbstractDelegateDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<DataCollectionTaskResult> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  protected void waitForCompletion() {
    synchronized (lockObject) {
      try {
        lockObject.wait();
      } catch (InterruptedException e) {
        completed.set(true);
        getLogger().info("Splunk data collection interrupted");
      }
    }
  }

  protected void shutDownCollection() {
    /* Redundant now, but useful if calling shutDownCollection
     * from the worker threads before the job is aborted
     */
    completed.set(true);
    collectionService.shutdownNow();
    synchronized (lockObject) {
      lockObject.notifyAll();
    }
  }

  protected ScheduledExecutorService scheduleDataCollection(DataCollectionTaskResult taskResult) throws IOException {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(getDataCollector(taskResult), 0, 1, TimeUnit.MINUTES);
    return scheduledExecutorService;
  }

  public DataCollectionTaskResult run(Object[] parameters) {
    try {
      DataCollectionTaskResult taskResult = initDataCollection(parameters);
      if (taskResult.getStatus() == DataCollectionTaskStatus.FAILURE) {
        return taskResult;
      }

      scheduleDataCollection(taskResult);
      getLogger().info("going to collect data for " + parameters[0]);
      waitForCompletion();
      return taskResult;
    } catch (Exception e) {
      return DataCollectionTaskResult.builder()
          .status(DataCollectionTaskStatus.FAILURE)
          .stateType(getStateType())
          .errorMessage("Unable to connect to server : " + e.getMessage())
          .build();
    }
  }

  protected abstract StateType getStateType();

  protected abstract DataCollectionTaskResult initDataCollection(Object[] parameters);

  protected abstract Logger getLogger();

  protected abstract Runnable getDataCollector(DataCollectionTaskResult taskResult) throws IOException;
}
