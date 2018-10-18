package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;

import com.google.inject.Inject;

import com.sumologic.client.SumoLogicClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.time.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.sumo.SumoDataCollectionInfo;
import software.wings.service.impl.sumo.SumoDelegateServiceImpl;
import software.wings.sm.StateType;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by sriram_parthasarathy on 9/12/17.
 */
public class SumoDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final Logger logger = LoggerFactory.getLogger(SumoDataCollectionTask.class);
  private SumoDataCollectionInfo dataCollectionInfo;
  private SumoLogicClient sumoClient;
  public static final String DEFAULT_TIME_ZONE = "UTC";

  @Inject private LogAnalysisStoreService logAnalysisStoreService;
  @Inject private DelegateLogService delegateLogService;
  @Inject private SumoDelegateServiceImpl sumoDelegateService;

  public SumoDataCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected StateType getStateType() {
    return StateType.SUMO;
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(Object[] parameters) {
    DataCollectionTaskResult taskResult =
        DataCollectionTaskResult.builder().status(DataCollectionTaskStatus.SUCCESS).stateType(StateType.SUMO).build();
    this.dataCollectionInfo = (SumoDataCollectionInfo) parameters[0];
    logger.info("log collection - dataCollectionInfo: {}", dataCollectionInfo);
    sumoClient = sumoDelegateService.getSumoClient(
        dataCollectionInfo.getSumoConfig(), dataCollectionInfo.getEncryptedDataDetails(), encryptionService);
    return taskResult;
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) throws IOException {
    return new SumoDataCollector(getTaskId(), dataCollectionInfo, logAnalysisStoreService, taskResult);
  }

  protected int getInitialDelayMinutes() {
    return SplunkDataCollectionTask.DELAY_MINUTES + 1;
  }

  private class SumoDataCollector implements Runnable {
    private String delegateTaskId;
    private final SumoDataCollectionInfo dataCollectionInfo;
    private final LogAnalysisStoreService logAnalysisStoreService;
    private long collectionStartTime;
    private int logCollectionMinute;
    private DataCollectionTaskResult taskResult;

    private SumoDataCollector(String delegateTaskId, SumoDataCollectionInfo dataCollectionInfo,
        LogAnalysisStoreService logAnalysisStoreService, DataCollectionTaskResult taskResult) {
      this.delegateTaskId = delegateTaskId;
      this.dataCollectionInfo = dataCollectionInfo;
      this.logAnalysisStoreService = logAnalysisStoreService;
      this.logCollectionMinute = dataCollectionInfo.getStartMinute();
      this.taskResult = taskResult;
      this.collectionStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime())
          + logCollectionMinute * TimeUnit.MINUTES.toMillis(1);
    }

    @SuppressFBWarnings({"RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"})
    @Override
    public void run() {
      try {
        int retry = 0;
        while (!completed.get() && retry < RETRIES) {
          try {
            final List<LogElement> logElements = new ArrayList<>();
            for (String host : dataCollectionInfo.getHosts()) {
              String hostStr = dataCollectionInfo.getHostnameField() + " = \"" + host + "\" ";
              String query = dataCollectionInfo.getQuery();

              /* Heart beat */
              final LogElement sumoHeartBeatElement = new LogElement();
              sumoHeartBeatElement.setQuery(query);
              sumoHeartBeatElement.setClusterLabel("-3");
              sumoHeartBeatElement.setHost(host);
              sumoHeartBeatElement.setCount(0);
              sumoHeartBeatElement.setLogMessage("");
              sumoHeartBeatElement.setTimeStamp(0);
              sumoHeartBeatElement.setLogCollectionMinute(logCollectionMinute);
              logElements.add(sumoHeartBeatElement);

              if (hostStr == null) {
                throw new IllegalArgumentException("No hosts found for Sumo task " + dataCollectionInfo.toString());
              }

              ThirdPartyApiCallLog apiCallLog = createApiCallLog(dataCollectionInfo.getStateExecutionId());
              final long collectionEndTime = collectionStartTime + TimeUnit.MINUTES.toMillis(1) - 1;

              try {
                List<LogElement> logElementsResponse = sumoDelegateService.getResponse(
                    dataCollectionInfo.getSumoConfig(), query, "1m", dataCollectionInfo.getEncryptedDataDetails(),
                    dataCollectionInfo.getHostnameField(), host, collectionStartTime, collectionEndTime,
                    DEFAULT_TIME_ZONE, 1000, logCollectionMinute, apiCallLog);
                logElements.addAll(logElementsResponse);
              } catch (CancellationException e) {
                logger.info("Ugh. Search job was cancelled. Retrying ...");
                if (++retry == RETRIES) {
                  taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
                  taskResult.setErrorMessage("Sumo Logic cancelled search job " + RETRIES + " times");
                  completed.set(true);
                  break;
                }
                continue;
              }
            }

            boolean response = logAnalysisStoreService.save(StateType.SUMO, dataCollectionInfo.getAccountId(),
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
            logger.info("sent sumo search records to server. Num of events: " + logElements.size()
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
              logger.warn("error fetching sumo logs. retrying in " + RETRY_SLEEP + "s", e);
              sleep(RETRY_SLEEP);
            }
          }
        }
        collectionStartTime += TimeUnit.MINUTES.toMillis(1);
        logCollectionMinute++;
        dataCollectionInfo.setCollectionTime(dataCollectionInfo.getCollectionTime() - 1);
        if (dataCollectionInfo.getCollectionTime() <= 0) {
          // We are done with all data collection, so setting task status to success and quitting.
          logger.info(
              "Completed SumoLogic collection task. So setting task status to success and quitting. StateExecutionId {}",
              dataCollectionInfo.getStateExecutionId());
          completed.set(true);
          taskResult.setStatus(DataCollectionTaskStatus.SUCCESS);
        }

      } catch (Exception e) {
        completed.set(true);
        if (taskResult.getStatus() != DataCollectionTaskStatus.FAILURE) {
          taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
          taskResult.setErrorMessage("error fetching sumo logs for minute " + logCollectionMinute);
        }
        logger.error("error fetching sumo logs", e);
      }

      if (completed.get()) {
        logger.info("Shutting down sumo data collection " + dataCollectionInfo.getStateExecutionId());
        shutDownCollection();
        return;
      }
    }
  }
}
