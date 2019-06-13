package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.VerificationConstants.STACK_DRIVER_QUERY_SEPARATER;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;

import com.google.api.services.logging.v2.Logging;
import com.google.api.services.logging.v2.model.ListLogEntriesRequest;
import com.google.api.services.logging.v2.model.ListLogEntriesResponse;
import com.google.api.services.logging.v2.model.LogEntry;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.TaskType;
import software.wings.service.impl.GcpHelperService;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.stackdriver.StackDriverLogDataCollectionInfo;
import software.wings.service.intfc.stackdriver.StackDriverDelegateService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class StackDriverLogDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private StackDriverLogDataCollectionInfo dataCollectionInfo;

  @Inject private StackDriverDelegateService stackDriverDelegateService;
  @Inject private DelegateLogService delegateLogService;
  @Inject private GcpHelperService gcpHelperService;
  @Inject private LogAnalysisStoreService logAnalysisStoreService;

  public StackDriverLogDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected StateType getStateType() {
    return StateType.STACK_DRIVER;
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(TaskParameters parameters) {
    dataCollectionInfo = (StackDriverLogDataCollectionInfo) parameters;
    logger.info("metric collection - dataCollectionInfo: {}", dataCollectionInfo);
    return DataCollectionTaskResult.builder()
        .status(DataCollectionTaskResult.DataCollectionTaskStatus.SUCCESS)
        .stateType(StateType.STACK_DRIVER)
        .build();
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected boolean is24X7Task() {
    return getTaskType().equals(TaskType.STACKDRIVER_COLLECT_24_7_LOG_DATA.name());
  }

  @Override
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) throws IOException {
    return new StackDriverMetricCollector(dataCollectionInfo, taskResult, getTaskId());
  }

  private class StackDriverMetricCollector implements Runnable {
    private final StackDriverLogDataCollectionInfo dataCollectionInfo;
    private final DataCollectionTaskResult taskResult;
    private long collectionStartTime;
    private long collectionEndTime;
    private long logCollectionMinute;
    private String delegateTaskId;

    private StackDriverMetricCollector(StackDriverLogDataCollectionInfo dataCollectionInfo,
        DataCollectionTaskResult taskResult, String delegateTaskId) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.taskResult = taskResult;
      this.delegateTaskId = delegateTaskId;

      this.logCollectionMinute = is24X7Task()
          ? (int) TimeUnit.MILLISECONDS.toMinutes(dataCollectionInfo.getEndTime())
          : (int) TimeUnit.MILLISECONDS.toMinutes(dataCollectionInfo.getStartTime());

      this.collectionStartTime = dataCollectionInfo.getStartTime();
      this.collectionEndTime = dataCollectionInfo.getEndTime();
    }

    @Override
    @SuppressWarnings("PMD")
    public void run() {
      encryptionService.decrypt(dataCollectionInfo.getGcpConfig(), dataCollectionInfo.getEncryptedDataDetails());
      int retry = 0;
      logger.info("Initiating Stackdriver log Data collection for startTime : {} duration : {}", collectionStartTime,
          dataCollectionInfo.getCollectionTime());
      while (!completed.get() && retry < RETRIES) {
        try {
          logger.info("starting log data collection for {} for minute {}", dataCollectionInfo, collectionStartTime);

          List<LogElement> logElements = new ArrayList<>();
          ThirdPartyApiCallLog apiCallLog = createApiCallLog(dataCollectionInfo.getStateExecutionId());

          String projectId = stackDriverDelegateService.getProjectId(dataCollectionInfo.getGcpConfig());
          Logging logging = gcpHelperService.getLoggingResource(
              dataCollectionInfo.getGcpConfig(), dataCollectionInfo.getEncryptedDataDetails(), projectId);

          for (String host : dataCollectionInfo.getHosts()) {
            addHeartbeat(host, dataCollectionInfo, logCollectionMinute, logElements);
          }

          try {
            List<LogEntry> entries = fetchLogs(projectId, logging, dataCollectionInfo.getQuery(), collectionStartTime,
                collectionEndTime, dataCollectionInfo, apiCallLog, dataCollectionInfo.getHosts(),
                dataCollectionInfo.getHostnameField());
            int clusterLabel = 0;
            if (isNotEmpty(entries)) {
              logger.info("Total no. of log records found : {}", entries.size());
              for (LogEntry entry : entries) {
                long timeStamp = new DateTime(entry.getTimestamp()).getMillis();
                LogElement logElement =
                    LogElement.builder()
                        .query(dataCollectionInfo.getQuery())
                        .logCollectionMinute(
                            is24X7Task() ? (int) TimeUnit.MILLISECONDS.toMinutes(timeStamp) : logCollectionMinute)
                        .clusterLabel(String.valueOf(clusterLabel++))
                        .count(1)
                        .logMessage(entry.getTextPayload())
                        .timeStamp(timeStamp)
                        .host(entry.getResource().getLabels().get(dataCollectionInfo.getHostnameField()))
                        .build();
                logElements.add(logElement);
              }
            }
          } catch (Exception e) {
            logger.info("Search job was cancelled. Retrying ...", e);
            apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
            apiCallLog.addFieldToResponse(
                HttpStatus.SC_REQUEST_TIMEOUT, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
            delegateLogService.save(getAccountId(), apiCallLog);
            if (++retry == RETRIES) {
              taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
              taskResult.setErrorMessage("Stackdriver cancelled search job " + RETRIES + " times");
              completed.set(true);
              break;
            }
            sleep(RETRY_SLEEP);
            continue;
          }
          logger.info("sent Stackdriver search records to server. Num of events: " + logElements.size()
              + " application: " + dataCollectionInfo.getApplicationId()
              + " stateExecutionId: " + dataCollectionInfo.getStateExecutionId() + " minute: " + logCollectionMinute);

          boolean response = logAnalysisStoreService.save(StateType.STACK_DRIVER, dataCollectionInfo.getAccountId(),
              dataCollectionInfo.getApplicationId(), dataCollectionInfo.getCvConfigId(),
              dataCollectionInfo.getStateExecutionId(), dataCollectionInfo.getWorkflowId(),
              dataCollectionInfo.getWorkflowExecutionId(), dataCollectionInfo.getServiceId(), delegateTaskId,
              logElements);
          if (!response) {
            if (++retry == RETRIES) {
              taskResult.setStatus(DataCollectionTaskResult.DataCollectionTaskStatus.FAILURE);
              taskResult.setErrorMessage("Unable to save Stack driver logs after " + RETRIES + " times");
              completed.set(true);
              break;
            }
            sleep(RETRY_SLEEP);
            continue;
          }

          // We are done with all data collection, so setting task status to success and quitting.
          logger.info(
              "Completed stack driver collection task. So setting task status to success and quitting. StateExecutionId {}",
              dataCollectionInfo.getStateExecutionId());
          completed.set(true);
          taskResult.setStatus(DataCollectionTaskResult.DataCollectionTaskStatus.SUCCESS);
          break;
        } catch (Throwable ex) {
          if (!(ex instanceof Exception) || ++retry >= RETRIES) {
            logger.error("error fetching stack driver logs for {} for minute {}",
                dataCollectionInfo.getStateExecutionId(), logCollectionMinute, ex);
            taskResult.setStatus(DataCollectionTaskResult.DataCollectionTaskStatus.FAILURE);
            completed.set(true);
            break;
          } else {
            if (retry == 1) {
              taskResult.setErrorMessage(ExceptionUtils.getMessage(ex));
            }
            logger.warn("error fetching stack driver logs for minute " + logCollectionMinute + ". retrying in "
                    + RETRY_SLEEP + "s",
                ex);
            sleep(RETRY_SLEEP);
          }
        }
      }

      if (completed.get()) {
        logger.info("Shutting down stack driver data collection");
        shutDownCollection();
        return;
      }
    }

    public List<LogEntry> fetchLogs(String projectId, Logging logging, String query, long startTime, long endTime,
        StackDriverLogDataCollectionInfo dataCollectionInfo, ThirdPartyApiCallLog apiCallLog, Set<String> hosts,
        String hostnameField) {
      apiCallLog.setTitle("Fetching log data from project");
      apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());

      String queryField = getQueryField(hostnameField, new ArrayList<>(hosts), query, startTime, endTime);

      apiCallLog.addFieldToRequest(ThirdPartyApiCallLog.ThirdPartyApiCallField.builder()
                                       .name("query")
                                       .value(queryField)
                                       .type(ThirdPartyApiCallLog.FieldType.JSON)
                                       .build());
      apiCallLog.addFieldToRequest(ThirdPartyApiCallLog.ThirdPartyApiCallField.builder()
                                       .name("Start Time")
                                       .value(stackDriverDelegateService.getDateFormatTime(startTime))
                                       .type(ThirdPartyApiCallLog.FieldType.TIMESTAMP)
                                       .build());
      apiCallLog.addFieldToRequest(ThirdPartyApiCallLog.ThirdPartyApiCallField.builder()
                                       .name("End Time")
                                       .value(stackDriverDelegateService.getDateFormatTime(endTime))
                                       .type(ThirdPartyApiCallLog.FieldType.TIMESTAMP)
                                       .build());

      ListLogEntriesResponse response;
      try {
        ListLogEntriesRequest request = new ListLogEntriesRequest();
        request.setFilter(queryField);
        request.setProjectIds(Collections.singletonList(projectId));
        response = logging.entries().list(request).execute();
      } catch (Exception e) {
        apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
        apiCallLog.addFieldToResponse(
            HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), ThirdPartyApiCallLog.FieldType.TEXT);
        delegateLogService.save(dataCollectionInfo.getGcpConfig().getAccountId(), apiCallLog);
        throw new WingsException(
            "Unsuccessful response while fetching data from StackDriver. Error message: " + e.getMessage());
      }
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_OK, response, ThirdPartyApiCallLog.FieldType.JSON);
      delegateLogService.save(dataCollectionInfo.getGcpConfig().getAccountId(), apiCallLog);

      return response.getEntries();
    }

    private String getQueryField(String hostnameField, List<String> hosts, String query, long startTime, long endTime) {
      String formattedStartTime = stackDriverDelegateService.getDateFormatTime(startTime);
      String formattedEndTime = stackDriverDelegateService.getDateFormatTime(endTime);

      StringBuilder queryBuilder = new StringBuilder(80);

      if (!is24X7Task()) {
        queryBuilder.append("resource.labels." + hostnameField + "=(");
        for (int i = 0; i < hosts.size(); i++) {
          queryBuilder.append(hosts.get(i));
          if (i != hosts.size() - 1) {
            queryBuilder.append(" OR ");
          } else {
            queryBuilder.append(')');
          }
        }
        queryBuilder.append(STACK_DRIVER_QUERY_SEPARATER);
      }
      queryBuilder.append(query)
          .append(STACK_DRIVER_QUERY_SEPARATER)
          .append("timestamp>=\"")
          .append(formattedStartTime)
          .append("\"" + STACK_DRIVER_QUERY_SEPARATER + "timestamp<\"")
          .append(formattedEndTime);

      return queryBuilder.toString() + "\"";
    }
  }
}