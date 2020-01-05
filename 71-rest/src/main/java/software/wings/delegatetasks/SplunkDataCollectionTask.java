package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import software.wings.beans.TaskType;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.splunk.SplunkDataCollectionInfo;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.sm.StateType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by rsingh on 5/18/17.
 */
@Slf4j
public class SplunkDataCollectionTask extends AbstractDelegateDataCollectionTask {
  public static final Duration RETRY_SLEEP = Duration.ofSeconds(30);

  private SplunkDataCollectionInfo dataCollectionInfo;

  @Inject private LogAnalysisStoreService logAnalysisStoreService;
  @Inject private SplunkDelegateService splunkDelegateService;

  public SplunkDataCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(TaskParameters parameters) {
    DataCollectionTaskResult taskResult = DataCollectionTaskResult.builder()
                                              .status(DataCollectionTaskStatus.SUCCESS)
                                              .stateType(StateType.SPLUNKV2)
                                              .build();
    this.dataCollectionInfo = (SplunkDataCollectionInfo) parameters;
    logger.info("log collection - dataCollectionInfo: {}", dataCollectionInfo);

    // Check whether for given splunk config, splunk Service is possible or not.
    try {
      splunkDelegateService.initSplunkService(
          dataCollectionInfo.getSplunkConfig(), dataCollectionInfo.getEncryptedDataDetails());
    } catch (WingsException ex) {
      taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
      taskResult.setErrorMessage(ex.getMessage());
      logger.error("Error initializing splunkService", ex);
    }
    return taskResult;
  }

  @Override
  protected boolean is24X7Task() {
    return getTaskType().equals(TaskType.SPLUNK_COLLECT_24_7_LOG_DATA.name());
  }

  @Override
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) {
    return new SplunkDataCollector(getTaskId(), dataCollectionInfo, logAnalysisStoreService, is24X7Task(), taskResult);
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected StateType getStateType() {
    return StateType.SPLUNKV2;
  }

  private class SplunkDataCollector implements Runnable {
    private String delegateTaskId;
    private final SplunkDataCollectionInfo dataCollectionInfo;
    private final LogAnalysisStoreService logAnalysisStoreService;
    private long startTimeMilliSec;
    private long endTimeMilliSec;
    private int logCollectionMinute;
    private DataCollectionTaskResult taskResult;

    private SplunkDataCollector(String delegateTaskId, SplunkDataCollectionInfo dataCollectionInfo,
        LogAnalysisStoreService logAnalysisStoreService, boolean is247Task, DataCollectionTaskResult taskResult) {
      this.delegateTaskId = delegateTaskId;
      this.dataCollectionInfo = dataCollectionInfo;
      this.logAnalysisStoreService = logAnalysisStoreService;
      this.logCollectionMinute = is247Task ? (int) TimeUnit.MILLISECONDS.toMinutes(dataCollectionInfo.getEndTime())
                                           : dataCollectionInfo.getStartMinute();
      this.taskResult = taskResult;
      this.startTimeMilliSec = is247Task ? dataCollectionInfo.getStartTime()
                                         : Timestamp.minuteBoundary(dataCollectionInfo.getStartTime())
              + logCollectionMinute * TimeUnit.MINUTES.toMillis(1);
      this.endTimeMilliSec =
          is247Task ? dataCollectionInfo.getEndTime() : startTimeMilliSec + TimeUnit.MINUTES.toMillis(1);
    }

    @Override
    @SuppressWarnings("PMD")
    public void run() {
      int retry = 0;
      while (!completed.get() && retry < RETRIES) {
        try {
          final List<LogElement> logElements = new ArrayList<>();
          final List<Callable<List<LogElement>>> callables = new ArrayList<>();
          for (String host : dataCollectionInfo.getHosts()) {
            String query = dataCollectionInfo.getQuery();
            addHeartbeat(host, dataCollectionInfo, logCollectionMinute, logElements);
            callables.add(() -> fetchLogs(host, query, logCollectionMinute));
          }
          List<Optional<List<LogElement>>> results = executeParallel(callables);
          results.forEach(result -> {
            if (result.isPresent()) {
              logElements.addAll(result.get());
            }
          });

          boolean response = logAnalysisStoreService.save(StateType.SPLUNKV2, dataCollectionInfo.getAccountId(),
              dataCollectionInfo.getApplicationId(), dataCollectionInfo.getCvConfigId(),
              dataCollectionInfo.getStateExecutionId(), dataCollectionInfo.getWorkflowId(),
              dataCollectionInfo.getWorkflowExecutionId(), dataCollectionInfo.getServiceId(), delegateTaskId,
              logElements);
          if (!response) {
            if (++retry == RETRIES) {
              taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
              // TODO capture error code and send back for all collectors
              taskResult.setErrorMessage("Cannot save log records. Server returned error ");
              completed.set(true);
              break;
            }
            continue;
          }
          logger.info("sent splunk search records to server. Num of events: " + logElements.size()
              + " application: " + dataCollectionInfo.getApplicationId()
              + " stateExecutionId: " + dataCollectionInfo.getStateExecutionId() + " minute: " + logCollectionMinute);
          break;
        } catch (Throwable ex) {
          if (!(ex instanceof Exception) || ++retry >= RETRIES) {
            logger.error("error fetching logs for {} for minute {}", dataCollectionInfo.getStateExecutionId(),
                logCollectionMinute, ex);
            taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
            completed.set(true);
            break;
          } else {
            /*
             * Save the exception from the first attempt. This is usually
             * more meaningful to trouble shoot.
             */
            if (retry == 1) {
              taskResult.setErrorMessage(ExceptionUtils.getMessage(ex));
            }
            logger.warn("error fetching splunk logs. retrying in " + RETRY_SLEEP + "s", ex);
            sleep(RETRY_SLEEP);
          }
        }
      }
      startTimeMilliSec += TimeUnit.MINUTES.toMillis(1);
      endTimeMilliSec = startTimeMilliSec + TimeUnit.MINUTES.toMillis(1);
      logCollectionMinute++;
      dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);
      if (dataCollectionInfo.getCollectionTime() <= 0) {
        // We are done with all data collection, so setting task status to success and quitting.
        logger.info(
            "Completed Splunk collection task. So setting task status to success and quitting. StateExecutionId {}",
            dataCollectionInfo.getStateExecutionId());
        completed.set(true);
        taskResult.setStatus(DataCollectionTaskStatus.SUCCESS);
      }

      if (completed.get()) {
        logger.info("Shutting down Splunk data collection " + dataCollectionInfo.getStateExecutionId());
        shutDownCollection();
        return;
      }
    }

    private List<LogElement> fetchLogs(String host, String query, int logCollectionMinute) {
      ThirdPartyApiCallLog apiCallLog = createApiCallLog(dataCollectionInfo.getStateExecutionId());
      return splunkDelegateService.getLogResults(dataCollectionInfo.getSplunkConfig(),
          dataCollectionInfo.getEncryptedDataDetails(), query, dataCollectionInfo.getHostnameField(), host,
          startTimeMilliSec, endTimeMilliSec, apiCallLog, logCollectionMinute, dataCollectionInfo.isAdvancedQuery());
    }
  }
}
