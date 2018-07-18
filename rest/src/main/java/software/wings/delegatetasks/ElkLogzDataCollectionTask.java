package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;
import static software.wings.sm.states.ElkAnalysisState.DEFAULT_TIME_FIELD;

import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.time.Timestamp;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.elk.ElkLogFetchRequest;
import software.wings.service.impl.logz.LogzDataCollectionInfo;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.logz.LogzDelegateService;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by rsingh on 5/18/17.
 */

public class ElkLogzDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final Logger logger = LoggerFactory.getLogger(ElkLogzDataCollectionTask.class);

  @Inject private ElkDelegateService elkDelegateService;
  @Inject private LogzDelegateService logzDelegateService;
  @Inject private LogAnalysisStoreService logAnalysisStoreService;
  private LogDataCollectionInfo dataCollectionInfo;

  public ElkLogzDataCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(Object[] parameters) {
    this.dataCollectionInfo = (LogDataCollectionInfo) parameters[0];
    logger.info("log collection - dataCollectionInfo: {}", dataCollectionInfo);
    return DataCollectionTaskResult.builder()
        .status(DataCollectionTaskStatus.SUCCESS)
        .stateType(dataCollectionInfo.getStateType())
        .build();
  }

  @Override
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) {
    return new ElkLogzDataCollector(getTaskId(), dataCollectionInfo, taskResult);
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected StateType getStateType() {
    return dataCollectionInfo.getStateType();
  }

  private class ElkLogzDataCollector implements Runnable {
    private final LogDataCollectionInfo dataCollectionInfo;
    private long collectionStartTime;
    private int logCollectionMinute;
    private DataCollectionTaskResult taskResult;
    private String delegateTaskId;

    private ElkLogzDataCollector(
        String delegateTaskId, LogDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult) {
      this.delegateTaskId = delegateTaskId;
      this.dataCollectionInfo = dataCollectionInfo;
      this.logCollectionMinute = dataCollectionInfo.getStartMinute();
      this.collectionStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime())
          + logCollectionMinute * TimeUnit.MINUTES.toMillis(1);
      this.taskResult = taskResult;
    }

    @SuppressFBWarnings({"DLS_DEAD_LOCAL_STORE", "REC_CATCH_EXCEPTION"})
    @Override
    public void run() {
      try {
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
                  final ElkDataCollectionInfo elkDataCollectionInfo = (ElkDataCollectionInfo) dataCollectionInfo;
                  final ElkLogFetchRequest elkFetchRequest =
                      ElkLogFetchRequest.builder()
                          .query(dataCollectionInfo.getQuery())
                          .indices(elkDataCollectionInfo.getIndices())
                          .hostnameField(elkDataCollectionInfo.getHostnameField())
                          .messageField(elkDataCollectionInfo.getMessageField())
                          .timestampField(elkDataCollectionInfo.getTimestampField())
                          .hosts(Collections.singleton(hostName))
                          .startTime(collectionStartTime)
                          .endTime(collectionStartTime + TimeUnit.MINUTES.toMillis(1))
                          .queryType(elkDataCollectionInfo.getQueryType())
                          .build();
                  logger.info("running elk query: " + JsonUtils.asJson(elkFetchRequest.toElasticSearchJsonObject()));
                  searchResponse = elkDelegateService.search(elkDataCollectionInfo.getElkConfig(),
                      elkDataCollectionInfo.getEncryptedDataDetails(), elkFetchRequest,
                      createApiCallLog(dataCollectionInfo.getStateExecutionId()));
                  hostnameField = elkDataCollectionInfo.getHostnameField();
                  messageField = elkDataCollectionInfo.getMessageField();
                  timestampField = elkDataCollectionInfo.getTimestampField();
                  timestampFieldFormat = elkDataCollectionInfo.getTimestampFieldFormat();
                  break;
                case LOGZ:
                  final LogzDataCollectionInfo logzDataCollectionInfo = (LogzDataCollectionInfo) dataCollectionInfo;
                  final ElkLogFetchRequest logzFetchRequest =
                      ElkLogFetchRequest.builder()
                          .query(dataCollectionInfo.getQuery())
                          .indices("")
                          .hostnameField(logzDataCollectionInfo.getHostnameField())
                          .messageField(logzDataCollectionInfo.getMessageField())
                          .timestampField(logzDataCollectionInfo.getTimestampField())
                          .hosts(Collections.singleton(hostName))
                          .startTime(collectionStartTime)
                          .endTime(collectionStartTime + TimeUnit.MINUTES.toMillis(1))
                          .queryType(logzDataCollectionInfo.getQueryType())
                          .build();

                  logger.info("running logz query: " + JsonUtils.asJson(logzFetchRequest.toElasticSearchJsonObject()));
                  searchResponse = logzDelegateService.search(logzDataCollectionInfo.getLogzConfig(),
                      logzDataCollectionInfo.getEncryptedDataDetails(), logzFetchRequest,
                      createApiCallLog(dataCollectionInfo.getStateExecutionId()));
                  hostnameField = logzDataCollectionInfo.getHostnameField();
                  messageField = logzDataCollectionInfo.getMessageField();
                  timestampField = logzDataCollectionInfo.getTimestampField();
                  timestampFieldFormat = logzDataCollectionInfo.getTimestampFieldFormat();
                  break;
                default:
                  throw new IllegalStateException("Invalid collection attempt." + dataCollectionInfo);
              }

              JSONObject responseObject = new JSONObject(JsonUtils.asJson(searchResponse));
              JSONObject hits = responseObject.getJSONObject("hits");
              if (hits == null) {
                continue;
              }
              List<LogElement> logElements;
              try {
                logElements = parseElkResponse(searchResponse, dataCollectionInfo.getQuery(), timestampField,
                    timestampFieldFormat, hostnameField, hostName, messageField, logCollectionMinute, true);
              } catch (Exception pe) {
                retry = RETRIES;
                throw pe;
              }
              /**
               * Heart beat.
               */
              logElements.add(LogElement.builder()
                                  .query(dataCollectionInfo.getQuery())
                                  .clusterLabel("-1")
                                  .host(hostName)
                                  .count(0)
                                  .logMessage("")
                                  .timeStamp(0)
                                  .logCollectionMinute(logCollectionMinute)
                                  .build());
              boolean response =
                  logAnalysisStoreService.save(dataCollectionInfo.getStateType(), dataCollectionInfo.getAccountId(),
                      dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(),
                      dataCollectionInfo.getWorkflowId(), dataCollectionInfo.getWorkflowExecutionId(),
                      dataCollectionInfo.getServiceId(), delegateTaskId, logElements);
              if (!response) {
                if (++retry == RETRIES) {
                  taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
                  taskResult.setErrorMessage("Cannot save log records. Server returned error");
                  completed.set(true);
                  break;
                }
                continue;
              }
              logger.info("sent " + dataCollectionInfo.getStateType() + "search records to server. Num of events: "
                  + logElements.size() + " application: " + dataCollectionInfo.getApplicationId()
                  + " stateExecutionId: " + dataCollectionInfo.getStateExecutionId() + " minute: " + logCollectionMinute
                  + " host: " + hostName);
              break;
            } catch (Exception e) {
              if (++retry >= RETRIES) {
                taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
                completed.set(true);
                throw e;
              } else {
                /*
                 * Save the exception from the first attempt. This is usually
                 * more meaningful to trouble shoot.
                 */
                if (retry == 1) {
                  taskResult.setErrorMessage(Misc.getMessage(e));
                }
                logger.warn("error fetching elk/logz logs. retrying in " + RETRY_SLEEP + "s", e);
                sleep(RETRY_SLEEP);
              }
            }
          }
        }
        collectionStartTime += TimeUnit.MINUTES.toMillis(1);
        logCollectionMinute++;
        // dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);

      } catch (Exception e) {
        completed.set(true);
        if (taskResult.getStatus() != DataCollectionTaskStatus.FAILURE) {
          taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
          taskResult.setErrorMessage("error fetching elk/logz logs for minute " + logCollectionMinute);
        }
        logger.error("error fetching elk/logz logs", e);
      }

      if (completed.get()) {
        logger.info("Shutting down ELK/LOGZ collection " + dataCollectionInfo.getStateExecutionId());
        shutDownCollection();
      }
    }
  }

  public static List<LogElement> parseElkResponse(Object searchResponse, String query, String timestampField,
      String timestampFieldFormat, String hostnameField, String hostName, String messageField, int logCollectionMinute,
      boolean validateHost) {
    List<LogElement> logElements = new ArrayList<>();
    JSONObject responseObject = new JSONObject(JsonUtils.asJson(searchResponse));
    JSONObject hits = responseObject.getJSONObject("hits");
    if (hits == null) {
      return logElements;
    }

    DateTimeFormatter df = DateTimeFormatter.ofPattern(timestampFieldFormat);
    JSONArray logHits = hits.getJSONArray("hits");

    for (int i = 0; i < logHits.length(); i++) {
      JSONObject source = logHits.optJSONObject(i).getJSONObject("_source");
      if (source == null) {
        continue;
      }

      JSONObject hostObject = source;
      String[] hostPaths = hostnameField.split("\\.");
      for (int j = 0; j < hostPaths.length - 1; ++j) {
        hostObject = hostObject.getJSONObject(hostPaths[j]);
      }
      final String host =
          hostObject == null ? source.getString(hostnameField) : hostObject.getString(hostPaths[hostPaths.length - 1]);

      // if this elkResponse doesn't belong to this host, ignore it.
      // We ignore case because we don't know if elasticsearch might just lowercase everything in the index.
      if (validateHost && !hostName.trim().equalsIgnoreCase(host.trim())) {
        continue;
      }

      JSONObject messageObject = null;
      String[] messagePaths = messageField.split("\\.");
      for (int j = 0; j < messagePaths.length - 1; ++j) {
        messageObject = source.getJSONObject(messagePaths[j]);
      }
      final String logMessage = messageObject == null ? source.getString(messageField)
                                                      : messageObject.getString(messagePaths[messagePaths.length - 1]);

      JSONObject timeStampObject = null;
      String[] timeStampPaths = timestampField.split("\\.");
      for (int j = 0; j < timeStampPaths.length - 1; ++j) {
        timeStampObject = source.getJSONObject(timeStampPaths[j]);
      }
      final String timeStamp = timeStampObject == null
          ? source.getString(DEFAULT_TIME_FIELD)
          : timeStampObject.getString(timeStampPaths[timeStampPaths.length - 1]);
      long timeStampValue;
      try {
        timeStampValue = Instant.from(df.parse(timeStamp)).toEpochMilli();
      } catch (Exception pe) {
        throw new WingsException(
            "Failed to parse time stamp : " + timeStamp + ", with format: " + timestampFieldFormat, pe);
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

    return logElements;
  }
}
