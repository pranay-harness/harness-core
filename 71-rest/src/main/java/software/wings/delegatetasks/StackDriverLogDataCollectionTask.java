package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.VerificationConstants.DATA_COLLECTION_RETRY_SLEEP;
import static software.wings.common.VerificationConstants.STACKDRIVER_DEFAULT_HOST_NAME_FIELD;
import static software.wings.common.VerificationConstants.STACKDRIVER_DEFAULT_LOG_MESSAGE_FIELD;

import com.google.api.services.logging.v2.model.LogEntry;
import com.google.inject.Inject;

import com.jayway.jsonpath.JsonPath;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import software.wings.beans.TaskType;
import software.wings.service.impl.GcpHelperService;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.stackdriver.StackDriverLogDataCollectionInfo;
import software.wings.service.intfc.stackdriver.StackDriverDelegateService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    return StateType.STACK_DRIVER_LOG;
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(TaskParameters parameters) {
    dataCollectionInfo = (StackDriverLogDataCollectionInfo) parameters;
    logger.info("metric collection - dataCollectionInfo: {}", dataCollectionInfo);
    return DataCollectionTaskResult.builder()
        .status(DataCollectionTaskResult.DataCollectionTaskStatus.SUCCESS)
        .stateType(StateType.STACK_DRIVER_LOG)
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
          for (String host : dataCollectionInfo.getHosts()) {
            addHeartbeat(host, dataCollectionInfo, logCollectionMinute, logElements);
          }

          try {
            List<LogEntry> entries = stackDriverDelegateService.fetchLogs(dataCollectionInfo.getStateExecutionId(),
                dataCollectionInfo.getQuery(), collectionStartTime, collectionEndTime, dataCollectionInfo.getHosts(),
                dataCollectionInfo.getHostnameField(), dataCollectionInfo.getGcpConfig(),
                dataCollectionInfo.getEncryptedDataDetails(), is24X7Task(), true);
            int clusterLabel = 0;

            logger.info("Total no. of log records found : {}", entries.size());
            for (LogEntry entry : entries) {
              long timeStamp = new DateTime(entry.getTimestamp()).getMillis();
              String logMessageField = isNotEmpty(dataCollectionInfo.getLogMessageField())
                  ? dataCollectionInfo.getLogMessageField()
                  : STACKDRIVER_DEFAULT_LOG_MESSAGE_FIELD;
              String logMessage = JsonPath.read(entry.toString(), logMessageField);
              String host = JsonPath.read(entry.toString(),
                  isNotEmpty(dataCollectionInfo.getHostnameField()) ? dataCollectionInfo.getHostnameField()
                                                                    : STACKDRIVER_DEFAULT_HOST_NAME_FIELD);
              if (isEmpty(logMessage) || isEmpty(host)) {
                logger.error(
                    "either log message or host is empty for stateExId {} cvConfigId {}. Log message field: {} host field: {} entry: {} ",
                    dataCollectionInfo.getStateExecutionId(), dataCollectionInfo.getCvConfigId(), logMessageField,
                    dataCollectionInfo.getHostnameField(), entry);
              } else {
                LogElement logElement =
                    LogElement.builder()
                        .query(dataCollectionInfo.getQuery())
                        .logCollectionMinute(
                            is24X7Task() ? (int) TimeUnit.MILLISECONDS.toMinutes(timeStamp) : logCollectionMinute)
                        .clusterLabel(String.valueOf(clusterLabel++))
                        .count(1)
                        .logMessage(logMessage)
                        .timeStamp(timeStamp)
                        .host(host)
                        .build();
                logElements.add(logElement);
              }
            }

          } catch (Exception e) {
            logger.info("Search job was cancelled. Retrying ...", e);
            if (++retry == RETRIES) {
              taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
              taskResult.setErrorMessage("Stackdriver cancelled search job " + RETRIES + " times");
              completed.set(true);
              break;
            }
            sleep(DATA_COLLECTION_RETRY_SLEEP);
            continue;
          }
          logger.info("sent Stackdriver search records to server. Num of events: " + logElements.size()
              + " application: " + dataCollectionInfo.getApplicationId()
              + " stateExecutionId: " + dataCollectionInfo.getStateExecutionId() + " minute: " + logCollectionMinute);

          boolean response = logAnalysisStoreService.save(StateType.STACK_DRIVER_LOG, dataCollectionInfo.getAccountId(),
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
            sleep(DATA_COLLECTION_RETRY_SLEEP);
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
                    + DATA_COLLECTION_RETRY_SLEEP + "s",
                ex);
            sleep(DATA_COLLECTION_RETRY_SLEEP);
          }
        }
      }

      if (completed.get()) {
        logger.info("Shutting down stack driver data collection");
        shutDownCollection();
        return;
      }
    }
  }

  @Override
  protected int getInitialDelayMinutes() {
    // this state is implemented with per min task
    return 0;
  }
}