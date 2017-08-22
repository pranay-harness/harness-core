package software.wings.delegatetasks;

import static software.wings.service.impl.analysis.LogDataCollectionTaskResult.Builder.aLogDataCollectionTaskResult;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.service.impl.analysis.LogDataCollectionTaskResult;
import software.wings.service.impl.analysis.LogDataCollectionTaskResult.LogDataCollectionTaskStatus;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.elk.ElkLogFetchRequest;
import software.wings.service.impl.logz.LogzDataCollectionInfo;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.logz.LogzDelegateService;
import software.wings.sm.StateType;
import software.wings.time.WingsTimeUtils;
import software.wings.utils.JsonUtils;

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
public class ElkLogzDataCollectionTask extends AbstractDelegateRunnableTask<LogDataCollectionTaskResult> {
  private static final SimpleDateFormat ELK_DATE_FORMATER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
  private static final Logger logger = LoggerFactory.getLogger(ElkLogzDataCollectionTask.class);
  private final Object lockObject = new Object();
  private final AtomicBoolean completed = new AtomicBoolean(false);
  private ScheduledExecutorService collectionService;

  @Inject private ElkDelegateService elkDelegateService;
  @Inject private LogzDelegateService logzDelegateService;
  @Inject private LogAnalysisStoreService logAnalysisStoreService;

  public ElkLogzDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<LogDataCollectionTaskResult> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public LogDataCollectionTaskResult run(Object[] parameters) {
    final LogDataCollectionInfo dataCollectionInfo = (LogDataCollectionInfo) parameters[0];
    logger.info("log collection - dataCollectionInfo: {}" + dataCollectionInfo);

    collectionService = scheduleMetricDataCollection(dataCollectionInfo);
    logger.info("going to collect data for " + dataCollectionInfo);

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

  private ScheduledExecutorService scheduleMetricDataCollection(LogDataCollectionInfo dataCollectionInfo) {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(
        new ElkDataCollector(dataCollectionInfo), SplunkDataCollectionTask.DELAY_MINUTES, 1, TimeUnit.MINUTES);
    return scheduledExecutorService;
  }

  private void shutDownCollection() {
    collectionService.shutdown();
    completed.set(true);
    synchronized (lockObject) {
      lockObject.notifyAll();
    }
  }

  private class ElkDataCollector implements Runnable {
    private final LogDataCollectionInfo dataCollectionInfo;
    private long collectionStartTime;
    private int logCollectionMinute = 0;

    private ElkDataCollector(LogDataCollectionInfo dataCollectionInfo) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.collectionStartTime = WingsTimeUtils.getMinuteBoundary(dataCollectionInfo.getStartTime());
    }

    @Override
    public void run() {
      if (dataCollectionInfo.getCollectionTime() <= 0) {
        shutDownCollection();
        return;
      }

      try {
        for (String query : dataCollectionInfo.getQueries()) {
          for (String hostName : dataCollectionInfo.getHosts()) {
            Object searchResponse;
            String hostnameField;
            String messageField;
            switch (dataCollectionInfo.getStateType()) {
              case ELK:
                final ElkLogFetchRequest elkFetchRequest =
                    new ElkLogFetchRequest(query, ((ElkDataCollectionInfo) dataCollectionInfo).getIndices(),
                        ((ElkDataCollectionInfo) dataCollectionInfo).getHostnameField(),
                        ((ElkDataCollectionInfo) dataCollectionInfo).getMessageField(), Collections.singleton(hostName),
                        collectionStartTime, collectionStartTime + TimeUnit.MINUTES.toMillis(1));
                logger.info("running elk query: " + JsonUtils.asJson(elkFetchRequest.toElasticSearchJsonObject()));
                searchResponse = elkDelegateService.search(
                    ((ElkDataCollectionInfo) dataCollectionInfo).getElkConfig(), elkFetchRequest);
                hostnameField = ((ElkDataCollectionInfo) dataCollectionInfo).getHostnameField();
                messageField = ((ElkDataCollectionInfo) dataCollectionInfo).getMessageField();
                break;
              case LOGZ:
                final ElkLogFetchRequest logzFetchRequest = new ElkLogFetchRequest(query,
                    ((LogzDataCollectionInfo) dataCollectionInfo).getIndices(),
                    ((LogzDataCollectionInfo) dataCollectionInfo).getHostnameField(),
                    ((LogzDataCollectionInfo) dataCollectionInfo).getMessageField(), Collections.singleton(hostName),
                    collectionStartTime, collectionStartTime + TimeUnit.MINUTES.toMillis(1));
                logger.info("running logz query: " + JsonUtils.asJson(logzFetchRequest.toElasticSearchJsonObject()));
                searchResponse = logzDelegateService.search(
                    ((LogzDataCollectionInfo) dataCollectionInfo).getLogzConfig(), logzFetchRequest);
                hostnameField = ((LogzDataCollectionInfo) dataCollectionInfo).getHostnameField();
                messageField = ((LogzDataCollectionInfo) dataCollectionInfo).getMessageField();
                break;
              default:
                throw new IllegalStateException("Invalid collection attempt." + dataCollectionInfo);
            }

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

              JSONObject hostObject = null;
              String[] hostPaths = hostnameField.split("\\.");
              for (int j = 0; j < hostPaths.length - 1; ++j) {
                hostObject = source.getJSONObject(hostPaths[j]);
              }
              final String host = hostObject == null ? source.getString(hostnameField)
                                                     : hostObject.getString(hostPaths[hostPaths.length - 1]);

              JSONObject messageObject = null;
              String[] messagePaths = messageField.split("\\.");
              for (int j = 0; j < messagePaths.length - 1; ++j) {
                messageObject = source.getJSONObject(messagePaths[j]);
              }

              final String logMessage = messageObject == null
                  ? source.getString(messageField)
                  : messageObject.getString(messagePaths[messagePaths.length - 1]);
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

            logAnalysisStoreService.save(dataCollectionInfo.getStateType(), dataCollectionInfo.getAccountId(),
                dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(),
                dataCollectionInfo.getWorkflowId(), dataCollectionInfo.getWorkflowExecutionId(),
                dataCollectionInfo.getServiceId(), logElements);
            logger.info("sent " + dataCollectionInfo.getStateType() + "search records to server. Num of events: "
                + logElements.size() + " application: " + dataCollectionInfo.getApplicationId() + " stateExecutionId: "
                + dataCollectionInfo.getStateExecutionId() + " minute: " + logCollectionMinute + " host: " + hostName);
          }
        }
        collectionStartTime += TimeUnit.MINUTES.toMillis(1);
        logCollectionMinute++;
        dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);
      } catch (Exception e) {
        logger.error("error fetching elk logs", e);
      }
    }
  }
}
