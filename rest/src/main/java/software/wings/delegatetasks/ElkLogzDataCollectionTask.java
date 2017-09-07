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
import software.wings.time.WingsTimeUtils;
import software.wings.utils.JsonUtils;

import java.text.DateFormat;
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
  private static final Logger logger = LoggerFactory.getLogger(ElkLogzDataCollectionTask.class);
  private static final int RETRIES = 3;
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
    LogDataCollectionTaskResult taskResult =
        aLogDataCollectionTaskResult().withStatus(LogDataCollectionTaskStatus.SUCCESS).build();
    collectionService = scheduleMetricDataCollection(dataCollectionInfo, taskResult);
    logger.info("going to collect data for " + dataCollectionInfo);

    synchronized (lockObject) {
      while (!completed.get()) {
        try {
          lockObject.wait();
        } catch (InterruptedException e) {
          completed.set(true);
          logger.info("ELK/LOGZ data collection interrupted");
        }
      }
    }
    return taskResult;
  }

  private ScheduledExecutorService scheduleMetricDataCollection(
      LogDataCollectionInfo dataCollectionInfo, LogDataCollectionTaskResult taskResult) {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(new ElkDataCollector(getTaskId(), dataCollectionInfo, taskResult),
        SplunkDataCollectionTask.DELAY_MINUTES, 1, TimeUnit.MINUTES);
    return scheduledExecutorService;
  }

  private void shutDownCollection() {
    /* Redundant now, but useful if calling shutDownCollection
     * from the worker threads before the job is aborted
     */
    completed.set(true);
    collectionService.shutdownNow();
    synchronized (lockObject) {
      lockObject.notifyAll();
    }
  }

  private class ElkDataCollector implements Runnable {
    private final LogDataCollectionInfo dataCollectionInfo;
    private long collectionStartTime;
    private int logCollectionMinute = 0;
    private LogDataCollectionTaskResult taskResult;
    private String delegateTaskId;

    private ElkDataCollector(
        String delegateTaskId, LogDataCollectionInfo dataCollectionInfo, LogDataCollectionTaskResult taskResult) {
      this.delegateTaskId = delegateTaskId;
      this.dataCollectionInfo = dataCollectionInfo;
      this.logCollectionMinute = dataCollectionInfo.getStartMinute();
      this.collectionStartTime = WingsTimeUtils.getMinuteBoundary(dataCollectionInfo.getStartTime())
          + logCollectionMinute * TimeUnit.MINUTES.toMillis(1);
      this.taskResult = taskResult;
    }

    @Override
    public void run() {
      try {
        for (String query : dataCollectionInfo.getQueries()) {
          for (String hostName : dataCollectionInfo.getHosts()) {
            int retry = 0;
            while (!completed.get() && retry < RETRIES) {
              try {
                Object searchResponse;
                String hostnameField;
                String messageField;
                String timestampField;
                String timestampFieldFormat;
                switch (dataCollectionInfo.getStateType()) {
                  case ELK:
                    final ElkLogFetchRequest elkFetchRequest =
                        new ElkLogFetchRequest(query, ((ElkDataCollectionInfo) dataCollectionInfo).getIndices(),
                            ((ElkDataCollectionInfo) dataCollectionInfo).getHostnameField(),
                            ((ElkDataCollectionInfo) dataCollectionInfo).getMessageField(),
                            ((ElkDataCollectionInfo) dataCollectionInfo).getTimestampField(),
                            Collections.singleton(hostName), collectionStartTime,
                            collectionStartTime + TimeUnit.MINUTES.toMillis(1));
                    logger.info("running elk query: " + JsonUtils.asJson(elkFetchRequest.toElasticSearchJsonObject()));
                    searchResponse = elkDelegateService.search(
                        ((ElkDataCollectionInfo) dataCollectionInfo).getElkConfig(), elkFetchRequest);
                    hostnameField = ((ElkDataCollectionInfo) dataCollectionInfo).getHostnameField();
                    messageField = ((ElkDataCollectionInfo) dataCollectionInfo).getMessageField();
                    timestampField = ((ElkDataCollectionInfo) dataCollectionInfo).getTimestampField();
                    timestampFieldFormat = ((ElkDataCollectionInfo) dataCollectionInfo).getTimestampFieldFormat();
                    break;
                  case LOGZ:
                    final ElkLogFetchRequest logzFetchRequest = new ElkLogFetchRequest(query, "",
                        ((LogzDataCollectionInfo) dataCollectionInfo).getHostnameField(),
                        ((LogzDataCollectionInfo) dataCollectionInfo).getMessageField(),
                        ((LogzDataCollectionInfo) dataCollectionInfo).getTimestampField(),
                        Collections.singleton(hostName), collectionStartTime,
                        collectionStartTime + TimeUnit.MINUTES.toMillis(1));
                    logger.info(
                        "running logz query: " + JsonUtils.asJson(logzFetchRequest.toElasticSearchJsonObject()));
                    searchResponse = logzDelegateService.search(
                        ((LogzDataCollectionInfo) dataCollectionInfo).getLogzConfig(), logzFetchRequest);
                    hostnameField = ((LogzDataCollectionInfo) dataCollectionInfo).getHostnameField();
                    messageField = ((LogzDataCollectionInfo) dataCollectionInfo).getMessageField();
                    timestampField = ((LogzDataCollectionInfo) dataCollectionInfo).getTimestampField();
                    timestampFieldFormat = ((LogzDataCollectionInfo) dataCollectionInfo).getTimestampFieldFormat();
                    break;
                  default:
                    throw new IllegalStateException("Invalid collection attempt." + dataCollectionInfo);
                }

                JSONObject responseObject = new JSONObject(JsonUtils.asJson(searchResponse));
                JSONObject hits = responseObject.getJSONObject("hits");
                if (hits == null) {
                  continue;
                }

                DateFormat df = new SimpleDateFormat(timestampFieldFormat);
                JSONArray logHits = hits.getJSONArray("hits");
                final List<LogElement> logElements = new ArrayList<>();

                /**
                 * Heart beat.
                 */
                final LogElement elkHeartBeatElement = new LogElement();
                elkHeartBeatElement.setQuery(query);
                elkHeartBeatElement.setClusterLabel("-1");
                elkHeartBeatElement.setHost(hostName);
                elkHeartBeatElement.setCount(0);
                elkHeartBeatElement.setLogMessage("");
                elkHeartBeatElement.setTimeStamp(0);
                elkHeartBeatElement.setLogCollectionMinute(logCollectionMinute);

                logElements.add(elkHeartBeatElement);

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

                  JSONObject timeStampObject = null;
                  String[] timeStampPaths = timestampField.split("\\.");
                  for (int j = 0; j < timeStampPaths.length - 1; ++j) {
                    timeStampObject = source.getJSONObject(timeStampPaths[j]);
                  }
                  final String timeStamp = timeStampObject == null
                      ? source.getString("@timestamp")
                      : timeStampObject.getString(timeStampPaths[timeStampPaths.length - 1]);
                  long timeStampValue = 0;
                  try {
                    timeStampValue = df.parse(timeStamp).getTime();
                  } catch (java.text.ParseException pe) {
                    logger.warn("Failed to parse time stamp : " + timeStamp + ", " + timestampFieldFormat, pe);
                    continue;
                  }

                  final LogElement elkLogElement = new LogElement();
                  elkLogElement.setQuery(query);
                  elkLogElement.setClusterLabel(String.valueOf(i));
                  elkLogElement.setHost(host);
                  elkLogElement.setCount(1);
                  elkLogElement.setLogMessage(logMessage);
                  elkLogElement.setTimeStamp(timeStampValue);
                  elkLogElement.setLogCollectionMinute(logCollectionMinute);
                  logElements.add(elkLogElement);
                }

                boolean response =
                    logAnalysisStoreService.save(dataCollectionInfo.getStateType(), dataCollectionInfo.getAccountId(),
                        dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(),
                        dataCollectionInfo.getWorkflowId(), dataCollectionInfo.getWorkflowExecutionId(),
                        dataCollectionInfo.getServiceId(), delegateTaskId, logElements);
                if (!response) {
                  if (++retry == RETRIES) {
                    taskResult.setStatus(LogDataCollectionTaskStatus.FAILURE);
                    taskResult.setErrorMessage("Cannot save log records. Server returned error");
                    completed.set(true);
                    break;
                  }
                  continue;
                }
                logger.info("sent " + dataCollectionInfo.getStateType() + "search records to server. Num of events: "
                    + logElements.size() + " application: " + dataCollectionInfo.getApplicationId()
                    + " stateExecutionId: " + dataCollectionInfo.getStateExecutionId()
                    + " minute: " + logCollectionMinute + " host: " + hostName);
                break;
              } catch (Exception e) {
                if (++retry == RETRIES) {
                  taskResult.setStatus(LogDataCollectionTaskStatus.FAILURE);
                  taskResult.setErrorMessage(e.getMessage());
                  completed.set(true);
                  throw(e);
                } else {
                  logger.warn("error fetching elk logs. retrying...", e);
                }
              }
            }
          }
        }
        collectionStartTime += TimeUnit.MINUTES.toMillis(1);
        logCollectionMinute++;
        // dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);

      } catch (Exception e) {
        logger.error("error fetching elk logs", e);
      }

      if (completed.get()) {
        logger.info("Shutting down ELK/LOGZ collection");
        shutDownCollection();
      }
    }
  }
}
