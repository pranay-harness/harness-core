package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.VerificationConstants.DUMMY_HOST_NAME;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.TaskType;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.analysis.LogDataCollectionInfo;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.elk.ElkDelegateServiceImpl;
import software.wings.service.impl.elk.ElkLogFetchRequest;
import software.wings.service.impl.logz.LogzDataCollectionInfo;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.logz.LogzDelegateService;
import software.wings.sm.StateType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by rsingh on 5/18/17.
 */

@Slf4j
public class ElkLogzDataCollectionTask extends AbstractDelegateDataCollectionTask {
  @Inject private ElkDelegateService elkDelegateService;
  @Inject private LogzDelegateService logzDelegateService;
  @Inject private LogAnalysisStoreService logAnalysisStoreService;
  private LogDataCollectionInfo dataCollectionInfo;

  public ElkLogzDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(TaskParameters parameters) {
    this.dataCollectionInfo = (LogDataCollectionInfo) parameters;
    logger.info("log collection - dataCollectionInfo: {}", dataCollectionInfo);
    return DataCollectionTaskResult.builder()
        .status(DataCollectionTaskStatus.SUCCESS)
        .stateType(dataCollectionInfo.getStateType())
        .build();
  }

  @Override
  protected boolean is24X7Task() {
    return getTaskType().equals(TaskType.ELK_COLLECT_24_7_LOG_DATA.name());
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
      this.logCollectionMinute = is24X7Task() ? (int) TimeUnit.MILLISECONDS.toMinutes(dataCollectionInfo.getEndTime())
                                              : dataCollectionInfo.getStartMinute();
      this.collectionStartTime = is24X7Task() ? dataCollectionInfo.getStartTime()
                                              : Timestamp.minuteBoundary(dataCollectionInfo.getStartTime())
              + logCollectionMinute * TimeUnit.MINUTES.toMillis(1);
      this.taskResult = taskResult;
    }

    @Override
    @SuppressWarnings("PMD")
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
                          .hosts(hostName.equals(DUMMY_HOST_NAME) ? Collections.emptySet()
                                                                  : Collections.singleton(hostName))
                          .startTime(collectionStartTime)
                          .endTime(is24X7Task() ? dataCollectionInfo.getEndTime()
                                                : collectionStartTime + TimeUnit.MINUTES.toMillis(1))
                          .queryType(elkDataCollectionInfo.getQueryType())
                          .build();
                  logger.info("running elk query: " + JsonUtils.asJson(elkFetchRequest.toElasticSearchJsonObject()));
                  searchResponse = elkDelegateService.search(elkDataCollectionInfo.getElkConfig(),
                      elkDataCollectionInfo.getEncryptedDataDetails(), elkFetchRequest,
                      createApiCallLog(dataCollectionInfo.getStateExecutionId()), ElkDelegateServiceImpl.MAX_RECORDS);
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
                          .hosts(hostName.equals(DUMMY_HOST_NAME) ? Collections.emptySet()
                                                                  : Collections.singleton(hostName))
                          .startTime(collectionStartTime)
                          .endTime(is24X7Task() ? dataCollectionInfo.getEndTime()
                                                : collectionStartTime + TimeUnit.MINUTES.toMillis(1))
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
                    timestampFieldFormat, hostnameField, hostName, messageField, logCollectionMinute, is24X7Task(),
                    dataCollectionInfo.getStartTime(), dataCollectionInfo.getEndTime());
              } catch (Exception pe) {
                retry = RETRIES;
                taskResult.setErrorMessage(ExceptionUtils.getMessage(pe));
                throw pe;
              }
              /**
               * Heart beat.
               */
              addHeartbeat(hostName, dataCollectionInfo, logCollectionMinute, logElements);
              boolean response = logAnalysisStoreService.save(dataCollectionInfo.getStateType(),
                  dataCollectionInfo.getAccountId(), dataCollectionInfo.getApplicationId(),
                  dataCollectionInfo.getCvConfigId(), dataCollectionInfo.getStateExecutionId(),
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
            } catch (Throwable ex) {
              if (!(ex instanceof Exception) || ++retry >= RETRIES) {
                logger.error("error fetching logs for {} for minute {}", dataCollectionInfo.getStateExecutionId(),
                    logCollectionMinute, ex);
                taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
                completed.set(true);
                throw ex;
              } else {
                /*
                 * Save the exception from the first attempt. This is usually
                 * more meaningful to trouble shoot.
                 */
                if (retry == 1) {
                  taskResult.setErrorMessage(ExceptionUtils.getMessage(ex));
                }
                logger.warn("error fetching elk/logz logs. retrying in " + RETRY_SLEEP + "s", ex);
                sleep(RETRY_SLEEP);
              }
            }
          }
        }
        collectionStartTime += TimeUnit.MINUTES.toMillis(1);
        logCollectionMinute++;
        if (logCollectionMinute >= dataCollectionInfo.getCollectionTime()) {
          // We are done with all data collection, so setting task status to success and quitting.
          logger.info(
              "Completed ELK collection task. So setting task status to success and quitting. StateExecutionId {}",
              dataCollectionInfo.getStateExecutionId());
          completed.set(true);
          taskResult.setStatus(DataCollectionTaskStatus.SUCCESS);
        }
        // dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);

      } catch (Throwable e) {
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
      boolean is24x7Task, long collectionStartTime, long collectionEndTime) {
    List<LogElement> logElements = new ArrayList<>();
    JSONObject responseObject = new JSONObject(JsonUtils.asJson(searchResponse));
    JSONObject hits = responseObject.getJSONObject("hits");
    if (hits == null) {
      return logElements;
    }

    SimpleDateFormat timeFormatter = new SimpleDateFormat(timestampFieldFormat);
    JSONArray logHits = hits.getJSONArray("hits");

    for (int i = 0; i < logHits.length(); i++) {
      JSONObject source = logHits.optJSONObject(i).getJSONObject("_source");
      if (source == null) {
        continue;
      }

      final String host = parseAndGetValue(source, hostnameField);

      // if this elkResponse doesn't belong to this host, ignore it.
      // We ignore case because we don't know if elasticsearch might just lowercase everything in the index.
      if (!is24x7Task && !hostName.trim().equalsIgnoreCase(host.trim())) {
        continue;
      }

      final String logMessage = parseAndGetValue(source, messageField);

      final String timeStamp = parseAndGetValue(source, timestampField);
      long timeStampValue;
      try {
        timeStampValue = timeFormatter.parse(timeStamp).getTime();
      } catch (ParseException pe) {
        throw new WingsException(ErrorCode.ELK_CONFIGURATION_ERROR,
            "Failed to parse time stamp : " + timeStamp + ", with format: " + timestampFieldFormat, WingsException.USER,
            pe)
            .addParam("reason", "Failed to parse time stamp : " + timeStamp + ", with format: " + timestampFieldFormat);
      }

      if (is24x7Task && (timeStampValue < collectionStartTime || timeStampValue > collectionEndTime)) {
        logger.info("received response outside the time range");
        continue;
      }

      final LogElement elkLogElement = new LogElement();
      elkLogElement.setQuery(query);
      elkLogElement.setClusterLabel(String.valueOf(i));
      elkLogElement.setHost(host);
      elkLogElement.setCount(1);
      elkLogElement.setLogMessage(logMessage);
      elkLogElement.setTimeStamp(timeStampValue);
      elkLogElement.setLogCollectionMinute(
          is24x7Task ? TimeUnit.MILLISECONDS.toMinutes(timeStampValue) : logCollectionMinute);
      logElements.add(elkLogElement);
    }

    return logElements;
  }

  public static String parseAndGetValue(JSONObject source, String field) {
    Object messageObject = source;
    String[] messagePaths = field.split("\\.");
    for (int j = 0; j < messagePaths.length; ++j) {
      if (messageObject instanceof JSONObject) {
        messageObject = ((JSONObject) messageObject).get(messagePaths[j]);
      } else if (messageObject instanceof JSONArray) {
        messageObject = ((JSONArray) messageObject).get(Integer.parseInt(messagePaths[j]));
      }
    }
    if (messageObject instanceof String) {
      return (String) messageObject;
    }
    throw new WingsException("Unable to parse JSON response " + source.toString() + " and field " + field);
  }
}
