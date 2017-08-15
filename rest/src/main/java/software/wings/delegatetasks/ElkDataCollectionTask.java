package software.wings.delegatetasks;

import static software.wings.service.impl.analysis.LogDataCollectionTaskResult.Builder.aLogDataCollectionTaskResult;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.service.impl.analysis.LogDataCollectionTaskResult;
import software.wings.service.impl.analysis.LogDataCollectionTaskResult.LogDataCollectionTaskStatus;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.elk.ElkLogFetchRequest;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.sm.StateType;
import software.wings.time.WingsTimeUtils;
import software.wings.utils.JsonUtils;
import software.wings.utils.Misc;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
  private static final SimpleDateFormat ELK_DATE_FORMATER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
  private static final Logger logger = LoggerFactory.getLogger(ElkDataCollectionTask.class);
  private final Object lockObject = new Object();
  private final AtomicBoolean completed = new AtomicBoolean(false);

  @Inject private ElkDelegateService elkDelegateService;
  @Inject private LogAnalysisStoreService logAnalysisStoreService;

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
    logger.info("going to collect elk data for " + dataCollectionInfo);

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
    scheduledExecutorService.scheduleAtFixedRate(
        new ElkDataCollector(dataCollectionInfo, elkDelegateService, logAnalysisStoreService),
        SplunkDataCollectionTask.DELAY_MINUTES, 1, TimeUnit.MINUTES);
    return scheduledExecutorService;
  }

  private static class ElkDataCollector implements Runnable {
    private final ElkDataCollectionInfo dataCollectionInfo;
    private final ElkDelegateService elkDelegateService;
    private final LogAnalysisStoreService logAnalysisStoreService;
    private long collectionStartTime;
    private int logCollectionMinute = 0;

    private ElkDataCollector(ElkDataCollectionInfo dataCollectionInfo, ElkDelegateService elkDelegateService,
        LogAnalysisStoreService logAnalysisStoreService) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.elkDelegateService = elkDelegateService;
      this.collectionStartTime = WingsTimeUtils.getMinuteBoundary(dataCollectionInfo.getStartTime());
      this.logAnalysisStoreService = logAnalysisStoreService;
    }

    @Override
    public void run() {
      if (dataCollectionInfo.getCollectionTime() <= 0) {
        return;
      }

      try {
        for (String query : dataCollectionInfo.getQueries()) {
          for (String hostName : dataCollectionInfo.getHosts()) {
            final ElkLogFetchRequest logFetchRequest = new ElkLogFetchRequest(query, Collections.singleton(hostName),
                collectionStartTime, collectionStartTime + TimeUnit.MINUTES.toMillis(1));

            logger.info("running elk query: " + JsonUtils.asJson(logFetchRequest.toElasticSearchJsonObject()));

            final Object searchResponse = elkDelegateService.search(dataCollectionInfo.getElkConfig(), logFetchRequest);
            JSONObject responseObject = new JSONObject(JsonUtils.asJson(searchResponse));
            JSONObject hits = responseObject.getJSONObject("hits");
            if (hits == null) {
              continue;
            }

            JSONArray logHits = hits.getJSONArray("hits");
            final List<LogElement> logElements = new ArrayList<>();
            for (int i = 0; i < logHits.length(); i++) {
              JSONObject source = logHits.optJSONObject(i).getJSONObject("_source");
              if (source == null) {
                continue;
              }

              final String host = source.getJSONObject("beat").getString("hostname");
              final String logMessage = source.getString("message");
              final long timeStamp = ELK_DATE_FORMATER.parse(source.getString("@timestamp")).getTime();

              final LogElement elkLogElement = new LogElement();
              elkLogElement.setQuery(query);
              elkLogElement.setClusterLabel(String.valueOf(i));
              elkLogElement.setHost(host);
              elkLogElement.setCount(1);
              elkLogElement.setLogMessage(logMessage);
              elkLogElement.setTimeStamp(timeStamp);
              elkLogElement.setLogCollectionMinute(logCollectionMinute);
              logElements.add(elkLogElement);
            }

            logAnalysisStoreService.save(StateType.ELK, dataCollectionInfo.getAccountId(),
                dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(),
                dataCollectionInfo.getWorkflowId(), dataCollectionInfo.getWorkflowExecutionId(),
                dataCollectionInfo.getServiceId(), logElements);
            logger.info("sent elk search records to server. Num of events: " + logElements.size()
                + " application: " + dataCollectionInfo.getApplicationId() + " stateExecutionId: "
                + dataCollectionInfo.getStateExecutionId() + " minute: " + logCollectionMinute + " host: " + hostName);
          }
        }
        collectionStartTime += TimeUnit.MINUTES.toMillis(1);
        logCollectionMinute++;
        dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);
      } catch (Exception e) {
        logger.error("error fetching splunk logs", e);
      }
    }
  }
}
