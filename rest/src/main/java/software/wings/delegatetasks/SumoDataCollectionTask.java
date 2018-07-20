package software.wings.delegatetasks;

import static io.harness.threading.Morpheus.sleep;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;

import com.google.inject.Inject;

import com.sumologic.client.Credentials;
import com.sumologic.client.SumoLogicClient;
import com.sumologic.client.model.LogMessage;
import com.sumologic.client.searchjob.model.GetMessagesForSearchJobResponse;
import com.sumologic.client.searchjob.model.GetSearchJobStatusResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.time.Timestamp;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.sumo.SumoDataCollectionInfo;
import software.wings.sm.StateType;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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

  @Inject private LogAnalysisStoreService logAnalysisStoreService;
  @Inject private DelegateLogService delegateLogService;

  public SumoDataCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
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
    encryptionService.decrypt(dataCollectionInfo.getSumoConfig(), dataCollectionInfo.getEncryptedDataDetails());
    Credentials credential = new Credentials(new String(dataCollectionInfo.getSumoConfig().getAccessId()),
        new String(dataCollectionInfo.getSumoConfig().getAccessKey()));
    sumoClient = new SumoLogicClient(credential);
    try {
      sumoClient.setURL(dataCollectionInfo.getSumoConfig().getSumoUrl());
    } catch (MalformedURLException e) {
      taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
      taskResult.setErrorMessage("Invalid server URL " + dataCollectionInfo.getSumoConfig().getSumoUrl());
    }
    return taskResult;
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) throws IOException {
    return new SumoDataCollector(getTaskId(), sumoClient, dataCollectionInfo, logAnalysisStoreService, taskResult);
  }

  protected int getInitialDelayMinutes() {
    return SplunkDataCollectionTask.DELAY_MINUTES + 1;
  }

  private class SumoDataCollector implements Runnable {
    private String delegateTaskId;
    private SumoLogicClient sumoClient;
    private final SumoDataCollectionInfo dataCollectionInfo;
    private final LogAnalysisStoreService logAnalysisStoreService;
    private long collectionStartTime;
    private int logCollectionMinute;
    private DataCollectionTaskResult taskResult;

    private SumoDataCollector(String delegateTaskId, SumoLogicClient sumoClient,
        SumoDataCollectionInfo dataCollectionInfo, LogAnalysisStoreService logAnalysisStoreService,
        DataCollectionTaskResult taskResult) {
      this.delegateTaskId = delegateTaskId;
      this.sumoClient = sumoClient;
      this.dataCollectionInfo = dataCollectionInfo;
      this.logAnalysisStoreService = logAnalysisStoreService;
      this.logCollectionMinute = dataCollectionInfo.getStartMinute();
      this.taskResult = taskResult;
      this.collectionStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime())
          + logCollectionMinute * TimeUnit.MINUTES.toMillis(1);
    }

    @SuppressFBWarnings({"RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "REC_CATCH_EXCEPTION"})
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

              hostStr = " | where " + hostStr + " ";

              String searchQuery = query + hostStr + " | timeslice 1m";

              final long endTime = collectionStartTime + TimeUnit.MINUTES.toMillis(1) - 1;
              ThirdPartyApiCallLog apiCallLog = createApiCallLog(dataCollectionInfo.getStateExecutionId());
              apiCallLog.setTitle("Fetch request to " + dataCollectionInfo.getSumoConfig().getSumoUrl());
              apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                               .name("url")
                                               .value(dataCollectionInfo.getSumoConfig().getSumoUrl())
                                               .type(FieldType.URL)
                                               .build());
              apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                               .name("url")
                                               .value(dataCollectionInfo.getSumoConfig().getSumoUrl())
                                               .type(FieldType.URL)
                                               .build());
              apiCallLog.addFieldToRequest(
                  ThirdPartyApiCallField.builder().name("searchQuery").value(searchQuery).type(FieldType.TEXT).build());
              apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                               .name("startTime")
                                               .value(Long.toString(collectionStartTime))
                                               .type(FieldType.TIMESTAMP)
                                               .build());
              apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                               .name("endTime")
                                               .value(Long.toString(endTime))
                                               .type(FieldType.TIMESTAMP)
                                               .build());
              apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toEpochSecond());
              logger.info("triggering sumo query startTime: " + collectionStartTime + " endTime: " + endTime
                  + " query: " + searchQuery + " url: " + dataCollectionInfo.getSumoConfig().getSumoUrl());
              String searchJobId = sumoClient.createSearchJob(
                  searchQuery, Long.toString(collectionStartTime), Long.toString(endTime), "UTC");

              int messageCount;
              GetSearchJobStatusResponse getSearchJobStatusResponse = null;
              // We will loop until the search job status
              // is either "DONE GATHERING RESULTS" or
              // "CANCELLED".
              while (getSearchJobStatusResponse == null
                  || (!getSearchJobStatusResponse.getState().equals("DONE GATHERING RESULTS")
                         && !getSearchJobStatusResponse.getState().equals("CANCELLED"))) {
                Thread.sleep(5000);

                // Get the latest search job status.
                getSearchJobStatusResponse = sumoClient.getSearchJobStatus(searchJobId);
                logger.info(
                    "Waiting on search job ID: " + searchJobId + " status: " + getSearchJobStatusResponse.getState());
              }

              apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toEpochSecond());
              apiCallLog.addFieldToResponse(HttpStatus.SC_OK, getSearchJobStatusResponse, FieldType.JSON);
              // If the last search job status indicated
              // that the search job was "CANCELLED", we
              // can't get messages or records.
              if (getSearchJobStatusResponse.getState().equals("CANCELLED")) {
                logger.info("Ugh. Search job was cancelled. Retrying ...");
                delegateLogService.save(getAccountId(), apiCallLog);
                if (++retry == RETRIES) {
                  taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
                  taskResult.setErrorMessage("Sumo Logic cancelled search job " + RETRIES + " times");
                  completed.set(true);
                  break;
                }
                continue;
              }

              delegateLogService.save(getAccountId(), apiCallLog);
              messageCount = getSearchJobStatusResponse.getMessageCount();

              int clusterLabel = 0;
              int messageOffset = 0;
              int messageLength = Math.min(messageCount, 1000);
              if (messageCount > 0) {
                do {
                  GetMessagesForSearchJobResponse getMessagesForSearchJobResponse =
                      sumoClient.getMessagesForSearchJob(searchJobId, messageOffset, messageLength);
                  for (LogMessage logMessage : getMessagesForSearchJobResponse.getMessages()) {
                    final LogElement sumoLogElement = new LogElement();
                    sumoLogElement.setQuery(query);
                    sumoLogElement.setClusterLabel(String.valueOf(clusterLabel++));
                    sumoLogElement.setCount(1);
                    sumoLogElement.setLogMessage(logMessage.getProperties().get("_raw"));
                    sumoLogElement.setTimeStamp(Long.parseLong(logMessage.getProperties().get("_timeslice")));
                    sumoLogElement.setLogCollectionMinute(logCollectionMinute);
                    logElements.add(sumoLogElement);
                    sumoLogElement.setHost(host);
                  }
                  messageCount -= messageLength;
                  messageOffset += messageLength;
                  messageLength = Math.min(messageCount, 1000);
                } while (messageCount > 0);
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
