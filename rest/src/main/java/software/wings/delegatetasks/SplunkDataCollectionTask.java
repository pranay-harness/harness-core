package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.time.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.splunk.SplunkDataCollectionInfo;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.sm.StateType;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

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
public class SplunkDataCollectionTask extends AbstractDelegateDataCollectionTask {
  public static final int DELAY_MINUTES = 2;
  public static final Duration RETRY_SLEEP = Duration.ofSeconds(30);

  private static final Logger logger = LoggerFactory.getLogger(SplunkDataCollectionTask.class);
  private SplunkDataCollectionInfo dataCollectionInfo;

  @Inject private LogAnalysisStoreService logAnalysisStoreService;
  @Inject private SplunkDelegateService splunkDelegateService;

  public SplunkDataCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  protected DataCollectionTaskResult initDataCollection(Object[] parameters) {
    DataCollectionTaskResult taskResult = DataCollectionTaskResult.builder()
                                              .status(DataCollectionTaskStatus.SUCCESS)
                                              .stateType(StateType.SPLUNKV2)
                                              .build();
    this.dataCollectionInfo = (SplunkDataCollectionInfo) parameters[0];
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
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) {
    return new SplunkDataCollector(getTaskId(), dataCollectionInfo, logAnalysisStoreService, taskResult);
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
    private long startTime;
    private long endTime;
    private int logCollectionMinute;
    private DataCollectionTaskResult taskResult;

    private SplunkDataCollector(String delegateTaskId, SplunkDataCollectionInfo dataCollectionInfo,
        LogAnalysisStoreService logAnalysisStoreService, DataCollectionTaskResult taskResult) {
      this.delegateTaskId = delegateTaskId;
      this.dataCollectionInfo = dataCollectionInfo;
      this.logAnalysisStoreService = logAnalysisStoreService;
      this.logCollectionMinute = dataCollectionInfo.getStartMinute();
      this.taskResult = taskResult;
      this.startTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime())
          + logCollectionMinute * TimeUnit.MINUTES.toMillis(1);
      this.endTime = startTime + TimeUnit.MINUTES.toMillis(1);
    }

    @Override
    public void run() {
      try {
        int retry = 0;
        while (!completed.get() && retry < RETRIES) {
          try {
            final List<LogElement> logElements = new ArrayList<>();
            final List<Callable<List<LogElement>>> callables = new ArrayList<>();
            for (String host : dataCollectionInfo.getHosts()) {
              String query = dataCollectionInfo.getQuery();
              /* Heart beat */
              final LogElement splunkHeartBeatElement = new LogElement();
              splunkHeartBeatElement.setQuery(query);
              splunkHeartBeatElement.setClusterLabel("-3");
              splunkHeartBeatElement.setHost(host);
              splunkHeartBeatElement.setCount(0);
              splunkHeartBeatElement.setLogMessage("");
              splunkHeartBeatElement.setTimeStamp(0);
              splunkHeartBeatElement.setLogCollectionMinute(logCollectionMinute);
              logElements.add(splunkHeartBeatElement);
              callables.add(() -> fetchLogsForHost(host, query));
            }

            List<Optional<List<LogElement>>> results = executeParrallel(callables);
            results.forEach(result -> {
              if (result.isPresent()) {
                logElements.addAll(result.get());
              }
            });

            boolean response = logAnalysisStoreService.save(StateType.SPLUNKV2, dataCollectionInfo.getAccountId(),
                dataCollectionInfo.getApplicationId(), dataCollectionInfo.getStateExecutionId(),
                dataCollectionInfo.getWorkflowId(), dataCollectionInfo.getWorkflowExecutionId(),
                dataCollectionInfo.getServiceId(), delegateTaskId, logElements);
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
          } catch (Exception e) {
            if (++retry == RETRIES) {
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
              logger.warn("error fetching splunk logs. retrying in " + RETRY_SLEEP + "s", e);
              sleep(RETRY_SLEEP);
            }
          }
        }
        startTime += TimeUnit.MINUTES.toMillis(1);
        endTime = startTime + TimeUnit.MINUTES.toMillis(1);
        logCollectionMinute++;
        dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);

      } catch (Exception e) {
        completed.set(true);
        if (taskResult.getStatus() != DataCollectionTaskStatus.FAILURE) {
          taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
          taskResult.setErrorMessage("error fetching splunk logs for minute " + logCollectionMinute);
        }
        logger.error("error fetching splunk logs", e);
      }

      if (completed.get()) {
        logger.info("Shutting down Splunk data collection " + dataCollectionInfo.getStateExecutionId());
        shutDownCollection();
        return;
      }
    }

    private List<LogElement> fetchLogsForHost(String host, String query) {
      ThirdPartyApiCallLog apiCallLog = createApiCallLog(dataCollectionInfo.getStateExecutionId());
      return splunkDelegateService.getLogResults(dataCollectionInfo.getSplunkConfig(),
          dataCollectionInfo.getEncryptedDataDetails(), query, dataCollectionInfo.getHostnameField(), host, startTime,
          endTime, apiCallLog);
    }
  }
}
