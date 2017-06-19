package software.wings.delegatetasks;

import static software.wings.service.impl.splunk.SplunkDataCollectionTaskResult.Builder.aSplunkDataCollectionTaskResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.service.impl.splunk.SplunkDataCollectionInfo;
import software.wings.service.impl.splunk.SplunkDataCollectionTaskResult;
import software.wings.service.impl.splunk.SplunkDataCollectionTaskResult.SplunkDataCollectionTaskStatus;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by rsingh on 5/18/17.
 */
public class SplunkDataCollectionTask extends AbstractDelegateRunnableTask<SplunkDataCollectionTaskResult> {
  private static final Logger logger = LoggerFactory.getLogger(SplunkDataCollectionTask.class);
  private final Object lockObject = new Object();
  private final AtomicBoolean completed = new AtomicBoolean(false);

  public SplunkDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<SplunkDataCollectionTaskResult> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public SplunkDataCollectionTaskResult run(Object[] parameters) {
    final SplunkDataCollectionInfo dataCollectionInfo = (SplunkDataCollectionInfo) parameters[0];
    logger.info("log collection - dataCollectionInfo: {}" + dataCollectionInfo);
    final ScheduledExecutorService collectionService = scheduleMetricDataCollection(dataCollectionInfo);
    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    scheduledExecutorService.schedule(() -> {
      try {
        logger.info("log collection finished for " + dataCollectionInfo);
        collectionService.shutdown();
        collectionService.awaitTermination(1, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        collectionService.shutdown();
      }

      completed.set(true);
      synchronized (lockObject) {
        lockObject.notifyAll();
      }
    }, dataCollectionInfo.getCollectionTime() + 1, TimeUnit.MINUTES);
    logger.info("going to collect splunk data for " + dataCollectionInfo);

    synchronized (lockObject) {
      while (!completed.get()) {
        try {
          lockObject.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    return aSplunkDataCollectionTaskResult().withStatus(SplunkDataCollectionTaskStatus.SUCCESS).build();
  }

  private ScheduledExecutorService scheduleMetricDataCollection(SplunkDataCollectionInfo dataCollectionInfo) {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(new SplunkDataCollector(dataCollectionInfo), 0, 1, TimeUnit.MINUTES);
    return scheduledExecutorService;
  }

  private static class SplunkDataCollector implements Runnable {
    private final SplunkDataCollectionInfo dataCollectionInfo;

    private SplunkDataCollector(SplunkDataCollectionInfo dataCollectionInfo) {
      this.dataCollectionInfo = dataCollectionInfo;
    }

    @Override
    public void run() {
      if (dataCollectionInfo.getCollectionTime() <= 0) {
        return;
      }

      return;
    }
  }
}
