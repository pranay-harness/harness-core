package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.VerificationConstants.DUMMY_HOST_NAME;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
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
import software.wings.beans.TaskType;
import software.wings.common.VerificationConstants;
import software.wings.service.impl.ThirdPartyApiCallLog;
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
  protected int getInitialDelayMinutes() {
    return dataCollectionInfo.getInitialDelayMinutes();
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

      // Condition needed as ELK follows absolute minute model and LogZ follows relative minute
      switch (getStateType()) {
        case ELK:
          this.collectionStartTime =
              is24X7Task() ? dataCollectionInfo.getStartTime() : logCollectionMinute * TimeUnit.MINUTES.toMillis(1);
          break;
        case LOGZ:
          this.collectionStartTime = is24X7Task() ? dataCollectionInfo.getStartTime()
                                                  : Timestamp.minuteBoundary(dataCollectionInfo.getStartTime())
                  + logCollectionMinute * TimeUnit.MINUTES.toMillis(1);
          break;

        default:
          throw new WingsException("Invalid StateType : " + getStateType());
      }
      this.taskResult = taskResult;
    }

    @Override
    @SuppressWarnings("PMD")
    public void run() {
      int retry = 0;
      while (!completed.get() && retry < RETRIES) {
        try {
          final List<LogElement> logElements = new ArrayList<>();
          for (String hostName : dataCollectionInfo.getHosts()) {
            addHeartbeat(hostName, dataCollectionInfo, logCollectionMinute, logElements);
            ThirdPartyApiCallLog apiCallLog = createApiCallLog(dataCollectionInfo.getStateExecutionId());

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
                        .hosts(
                            hostName.equals(DUMMY_HOST_NAME) ? Collections.emptySet() : Collections.singleton(hostName))
                        .startTime(collectionStartTime)
                        .endTime(is24X7Task() ? dataCollectionInfo.getEndTime()
                                              : collectionStartTime + TimeUnit.MINUTES.toMillis(1))
                        .queryType(elkDataCollectionInfo.getQueryType())
                        .build();
                logger.info("running elk query: " + JsonUtils.asJson(elkFetchRequest.toElasticSearchJsonObject()));
                searchResponse = elkDelegateService.search(elkDataCollectionInfo.getElkConfig(),
                    elkDataCollectionInfo.getEncryptedDataDetails(), elkFetchRequest, apiCallLog,
                    ElkDelegateServiceImpl.MAX_RECORDS);
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
                        .hosts(
                            hostName.equals(DUMMY_HOST_NAME) ? Collections.emptySet() : Collections.singleton(hostName))
                        .startTime(collectionStartTime)
                        .endTime(is24X7Task() ? dataCollectionInfo.getEndTime()
                                              : collectionStartTime + TimeUnit.MINUTES.toMillis(1))
                        .queryType(logzDataCollectionInfo.getQueryType())
                        .build();

                logger.info("running logz query: " + JsonUtils.asJson(logzFetchRequest.toElasticSearchJsonObject()));
                searchResponse = logzDelegateService.search(logzDataCollectionInfo.getLogzConfig(),
                    logzDataCollectionInfo.getEncryptedDataDetails(), logzFetchRequest, apiCallLog);
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
            if (!is24X7Task()) {
              long totalHitsPerMinute = getTotalHitsPerMinute(hits);
              if (totalHitsPerMinute > getDelegateTotalHitsVerificationThreshold()) {
                String reason = "Number of logs returned per minute are above the threshold. Please refine your query.";
                throw new WingsException(ErrorCode.ELK_CONFIGURATION_ERROR, reason).addParam("reason", reason);
              }
            }
            try {
              List<LogElement> logRecords = parseElkResponse(searchResponse, dataCollectionInfo.getQuery(),
                  timestampField, timestampFieldFormat, hostnameField, hostName, messageField, logCollectionMinute,
                  is24X7Task(), dataCollectionInfo.getStartTime(), dataCollectionInfo.getEndTime());
              logElements.addAll(logRecords);
              logger.info("Added {} records to logElements", logRecords.size());
            } catch (Exception pe) {
              logger.info("Exception occured while parsing elk response");
              if (++retry == RETRIES) {
                taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
                taskResult.setErrorMessage("ELK failed search job " + RETRIES + " times");
                completed.set(true);
                break;
              }
              sleep(RETRY_SLEEP);
              continue;
            }
          }
          logAnalysisStoreService.save(dataCollectionInfo.getStateType(), dataCollectionInfo.getAccountId(),
              dataCollectionInfo.getApplicationId(), dataCollectionInfo.getCvConfigId(),
              dataCollectionInfo.getStateExecutionId(), dataCollectionInfo.getWorkflowId(),
              dataCollectionInfo.getWorkflowExecutionId(), dataCollectionInfo.getServiceId(), delegateTaskId,
              logElements);
          logger.info("sent " + dataCollectionInfo.getStateType() + "search records to server. Num of events: "
              + logElements.size() + " application: " + dataCollectionInfo.getApplicationId()
              + " stateExecutionId: " + dataCollectionInfo.getStateExecutionId() + " minute: " + logCollectionMinute);
          break;
        } catch (Throwable ex) {
          /*
           * Save the exception from the first attempt. This is usually
           * more meaningful to trouble shoot.
           */
          if (retry == 0) {
            taskResult.setErrorMessage(ExceptionUtils.getMessage(ex));
          }
          if (ex instanceof WingsException || !(ex instanceof Exception) || ++retry >= RETRIES) {
            logger.error("error fetching logs for {} for minute {}", dataCollectionInfo.getStateExecutionId(),
                logCollectionMinute, ex);
            taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
            completed.set(true);
          } else {
            logger.warn("error fetching elk/logz logs. retrying in " + RETRY_SLEEP + "s", ex);
            sleep(RETRY_SLEEP);
          }
        }
      }
      if (taskResult.getStatus().equals(DataCollectionTaskStatus.FAILURE)) {
        logger.info("Failed Data collection for ELK collection task so quitting the task with StateExecutionId {}",
            dataCollectionInfo.getStateExecutionId());
        completed.set(true);
      } else {
        collectionStartTime += TimeUnit.MINUTES.toMillis(1);
        logCollectionMinute++;
        dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);
        if (dataCollectionInfo.getCollectionTime() <= 0) {
          // We are done with all data collection, so setting task status to success and quitting.
          logger.info(
              "Completed ELK collection task. So setting task status to success and quitting. StateExecutionId {}",
              dataCollectionInfo.getStateExecutionId());
          completed.set(true);
          taskResult.setStatus(DataCollectionTaskStatus.SUCCESS);
        }
      }

      if (completed.get()) {
        logger.info("Shutting down ELK/LOGZ collection " + dataCollectionInfo.getStateExecutionId());
        shutDownCollection();
      }
    }

    private long getTotalHitsPerMinute(JSONObject hits) {
      long totalHits = 0;
      if (hits.has("total")) {
        totalHits = hits.getLong("total");
      }
      long collectionEndTime =
          is24X7Task() ? dataCollectionInfo.getEndTime() : collectionStartTime + TimeUnit.MINUTES.toMillis(1);
      double intervalInMinutes = (collectionEndTime - collectionStartTime) / (1000 * 60.0);
      if (intervalInMinutes != 0) {
        return (long) (totalHits / intervalInMinutes);
      } else {
        return totalHits;
      }
    }
  }

  private long getDelegateTotalHitsVerificationThreshold() {
    // The multiplier is added to reduce number of runtime exception when running workflow.
    // Varying little bit from configure time threshold is acceptable.
    return 2 * VerificationConstants.TOTAL_HITS_PER_MIN_THRESHOLD;
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
