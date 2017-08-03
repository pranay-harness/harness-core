package software.wings.delegatetasks;

import static software.wings.service.impl.analysis.LogDataCollectionTaskResult.Builder.aLogDataCollectionTaskResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.service.impl.analysis.LogDataCollectionTaskResult;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.elk.ElkLogFetchRequest;
import software.wings.service.impl.analysis.LogDataCollectionTaskResult.LogDataCollectionTaskStatus;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.time.WingsTimeUtils;
import software.wings.utils.Misc;

import java.text.SimpleDateFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;

/**
 * Created by rsingh on 5/18/17.
 */
public class ElkDataCollectionTask extends AbstractDelegateRunnableTask<LogDataCollectionTaskResult> {
  private static final SimpleDateFormat SPLUNK_DATE_FORMATER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
  private static final Logger logger = LoggerFactory.getLogger(ElkDataCollectionTask.class);
  private final Object lockObject = new Object();
  private final AtomicBoolean completed = new AtomicBoolean(false);

  @Inject private ElkDelegateService elkDelegateService;

  public ElkDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<LogDataCollectionTaskResult> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public LogDataCollectionTaskResult run(Object[] parameters) {
    final ElkDataCollectionInfo dataCollectionInfo = (ElkDataCollectionInfo) parameters[0];
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
    }, dataCollectionInfo.getCollectionTime() + SplunkDataCollectionTask.DELAY_MINUTES + 1, TimeUnit.MINUTES);
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
    return aLogDataCollectionTaskResult().withStatus(LogDataCollectionTaskStatus.SUCCESS).build();
  }

  private ScheduledExecutorService scheduleMetricDataCollection(ElkDataCollectionInfo dataCollectionInfo) {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(new ElkDataCollector(dataCollectionInfo, elkDelegateService),
        SplunkDataCollectionTask.DELAY_MINUTES, 1, TimeUnit.MINUTES);
    return scheduledExecutorService;
  }

  private static class ElkDataCollector implements Runnable {
    private final ElkDataCollectionInfo dataCollectionInfo;
    private final ElkDelegateService elkDelegateService;
    private long collectionStartTime;
    private int logCollectionMinute = 0;

    private ElkDataCollector(ElkDataCollectionInfo dataCollectionInfo, ElkDelegateService elkDelegateService) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.elkDelegateService = elkDelegateService;
      this.collectionStartTime = WingsTimeUtils.getMinuteBoundary(dataCollectionInfo.getStartTime());
    }

    @Override
    public void run() {
      if (dataCollectionInfo.getCollectionTime() <= 0) {
        return;
      }

      try {
        for (String query : dataCollectionInfo.getQueries()) {
          final ElkLogFetchRequest logFetchRequest = new ElkLogFetchRequest(query, dataCollectionInfo.getHosts(),
              collectionStartTime, collectionStartTime + TimeUnit.MINUTES.toMillis(1));

          logger.info(elkDelegateService.search(dataCollectionInfo.getElkConfig(), logFetchRequest).toString());
        }
        collectionStartTime += TimeUnit.MINUTES.toMillis(1);
        logCollectionMinute++;
        dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);
      } catch (Exception e) {
        Misc.error(logger, "error fetching splunk logs", e);
      }
    }
  }
}
