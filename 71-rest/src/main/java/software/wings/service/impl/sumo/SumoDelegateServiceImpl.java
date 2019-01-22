package software.wings.service.impl.sumo;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.Constants.URL_STRING;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.inject.Inject;

import com.sumologic.client.Credentials;
import com.sumologic.client.SumoLogicClient;
import com.sumologic.client.SumoServerException;
import com.sumologic.client.model.LogMessage;
import com.sumologic.client.searchjob.model.GetMessagesForSearchJobResponse;
import com.sumologic.client.searchjob.model.GetSearchJobStatusResponse;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import io.harness.time.Timestamp;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.SumoConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.SumoDataCollectionTask;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.sumo.SumoDelegateService;

import java.net.MalformedURLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Delegate Service impl for Sumo Logic.
 *
 * Created by sriram_parthasarathy on 9/11/17.
 */
public class SumoDelegateServiceImpl implements SumoDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(SumoDelegateServiceImpl.class);
  private static final long DEFAULT_SLEEP_TIME_IN_MILLIS = 5000;
  private static final int SUMO_RATE_LIMIT_STATUS = 429;
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService delegateLogService;

  @Override
  public boolean validateConfig(SumoConfig config, List<EncryptedDataDetail> encryptedDataDetails) {
    logger.info("Starting config validation for SumoConfig : " + config);
    String query = "*exception*";
    String startTime = String.valueOf(Timestamp.currentMinuteBoundary() - 1);
    String endTime = String.valueOf(Timestamp.currentMinuteBoundary());
    try {
      getSumoClient(config, encryptedDataDetails, encryptionService)
          .createSearchJob(query, startTime, endTime, TimeZone.getDefault().getID());
      logger.info("Valid config provided");
      return true;
    } catch (Exception exception) {
      throw new WingsException("Error from Sumo xxxxxxxx " + exception.getMessage(), exception);
    }
  }

  @Override
  public List<LogElement> getLogSample(
      SumoConfig config, String index, List<EncryptedDataDetail> encryptedDataDetails, int duration) {
    logger.info("Starting to fetch sample log data for Sumo with config : " + config);
    long startTime = Timestamp.currentMinuteBoundary() - TimeUnit.MINUTES.toMillis(duration);
    long endTime = Timestamp.currentMinuteBoundary();
    String query = "*exception*";
    return getResponse(config, query, "5m", encryptedDataDetails, null, null, startTime, endTime,
        SumoDataCollectionTask.DEFAULT_TIME_ZONE, 5, 0, null);
  }

  @Override
  public VerificationNodeDataSetupResponse getLogDataByHost(String accountId, SumoConfig config, String query,
      String hostNameField, String hostName, List<EncryptedDataDetail> encryptedDataDetails,
      ThirdPartyApiCallLog apiCallLog) {
    logger.info("Starting to fetch test log data by host for sumo logic");

    long startTime = Timestamp.currentMinuteBoundary() - TimeUnit.MINUTES.toMillis(5);
    long endTime = Timestamp.currentMinuteBoundary();

    List<LogElement> responseWithoutHost = getResponse(config, query, "1m", encryptedDataDetails, null, null, startTime,
        endTime, SumoDataCollectionTask.DEFAULT_TIME_ZONE, 5, 0, apiCallLog);
    if (isEmpty(responseWithoutHost)) {
      return VerificationNodeDataSetupResponse.builder()
          .providerReachable(true)
          .loadResponse(VerificationLoadResponse.builder().isLoadPresent(false).build())
          .build();
    } else {
      List<LogElement> responseWithHost = getResponse(config, query, "1m", encryptedDataDetails, hostNameField,
          hostName, startTime, endTime, SumoDataCollectionTask.DEFAULT_TIME_ZONE, 5, 0, apiCallLog);
      return VerificationNodeDataSetupResponse.builder()
          .providerReachable(true)
          .loadResponse(
              VerificationLoadResponse.builder().loadResponse(responseWithoutHost).isLoadPresent(true).build())
          .dataForNode(responseWithHost)
          .build();
    }
  }

  /**
   * Method to fetch response based on query provided.
   * This method do following steps:
   *  - Gets a sumologic client
   *  - Creates a search job based on given query
   *  - Gets Search job Status Response
   *  - Gets message count and create LogElement list
   *  - Save Response in Third Party Call Logs
   * @param config
   * @param query
   * @param timeSlice
   * @param encryptedDataDetails
   * @param hostNameField
   * @param hostName
   * @param startTime
   * @param endTime
   * @param timeZone
   * @param maxMessageCount
   * @param apiCallLog
   * @return
   */
  public List<LogElement> getResponse(SumoConfig config, String query, String timeSlice,
      List<EncryptedDataDetail> encryptedDataDetails, String hostNameField, String hostName, long startTime,
      long endTime, String timeZone, int maxMessageCount, int logCollectionMinute, ThirdPartyApiCallLog apiCallLog) {
    String searchQuery = query;
    if (!isEmpty(hostNameField)) {
      searchQuery = searchQuery + "| where " + hostNameField + " = \"" + hostName + "\" ";
    }
    if (!isEmpty(timeSlice)) {
      searchQuery = searchQuery + " | timeslice " + timeSlice;
    }

    logger.info("Search query to be used in sumoLogic : " + searchQuery);

    Long requestTimeStamp = 0L;
    try {
      // Gets a sumologic client
      SumoLogicClient sumoClient = getSumoClient(config, encryptedDataDetails, encryptionService);

      requestTimeStamp = OffsetDateTime.now().toInstant().toEpochMilli();

      // Creates a search job based on given query
      String searchJobId =
          sumoClient.createSearchJob(searchQuery, String.valueOf(startTime), String.valueOf(endTime), timeZone);
      GetSearchJobStatusResponse searchJobStatusResponse = getSearchJobStatusResponse(sumoClient, searchJobId);

      Long responseTimeStamp = OffsetDateTime.now().toInstant().toEpochMilli();

      // SaveLogs is called for given job response.
      saveThirdPartyCallLogs(apiCallLog, config, searchQuery, String.valueOf(startTime), String.valueOf(endTime),
          searchJobStatusResponse, requestTimeStamp, responseTimeStamp, HttpStatus.SC_OK, FieldType.JSON);

      // SumoLogic may end up resulting lot of records that we may not need, so we have a maxMessageCount limit defined.
      // based on the max count. MessageCount value is set.
      int messageCount = searchJobStatusResponse.getMessageCount() > maxMessageCount
          ? maxMessageCount
          : searchJobStatusResponse.getMessageCount();
      logger.info("Search job Status Response received from SumoLogic with message Count : "
          + searchJobStatusResponse.getMessageCount());
      return getMessagesForSearchJob(
          query, hostName, messageCount, sumoClient, searchJobId, 0, 0, Math.min(messageCount, 5), logCollectionMinute);
    } catch (InterruptedException e) {
      throw new WingsException("Unable to get client for given config");
    } catch (SumoServerException sumoServerException) {
      if (sumoServerException.getHTTPStatus() == SUMO_RATE_LIMIT_STATUS) {
        saveThirdPartyCallLogs(apiCallLog, config, query, String.valueOf(startTime), String.valueOf(endTime),
            sumoServerException.getErrorMessage(), requestTimeStamp, OffsetDateTime.now().toInstant().toEpochMilli(),
            HttpStatus.SC_BAD_REQUEST, FieldType.TEXT);
        int randomNum = ThreadLocalRandom.current().nextInt(1, 11);
        logger.info("Encountered Rate limiting from sumo. Sleeping {}seconds for logCollectionMin {}", 30 + randomNum,
            logCollectionMinute);
        sleep(RETRY_SLEEP.plus(Duration.ofSeconds(randomNum)));
      }
      throw sumoServerException;
    } catch (Exception e) {
      saveThirdPartyCallLogs(apiCallLog, config, query, String.valueOf(startTime), String.valueOf(endTime),
          ExceptionUtils.getStackTrace(e), requestTimeStamp, OffsetDateTime.now().toInstant().toEpochMilli(),
          HttpStatus.SC_BAD_REQUEST, FieldType.TEXT);
      throw e;
    }
  }

  /**
   * Method to get sumologic client.
   * @param sumoConfig
   * @param encryptedDataDetails
   * @param encryptionService
   * @return
   */
  public static SumoLogicClient getSumoClient(
      SumoConfig sumoConfig, List<EncryptedDataDetail> encryptedDataDetails, EncryptionService encryptionService) {
    encryptionService.decrypt(sumoConfig, encryptedDataDetails);
    final Credentials credentials =
        new Credentials(new String(sumoConfig.getAccessId()), new String(sumoConfig.getAccessKey()));
    SumoLogicClient sumoLogicClient = new SumoLogicClient(credentials);
    HttpHost httpProxyHost = Http.getHttpProxyHost(sumoConfig.getSumoUrl());
    if (httpProxyHost != null) {
      sumoLogicClient.setProxyHost(httpProxyHost.getHostName());
      sumoLogicClient.setProxyPort(httpProxyHost.getPort());
      sumoLogicClient.setProxyProtocol(httpProxyHost.getSchemeName());
    }
    try {
      sumoLogicClient.setURL(sumoConfig.getSumoUrl());
    } catch (MalformedURLException e) {
      throw new WingsException("Unable to create SumoLogic Client");
    }
    return sumoLogicClient;
  }

  /**
   * Method gets the actual messages for given query and returns List of LogElements.
   * @param query
   * @param hostName
   *@param messageCount
   * @param sumoClient
   * @param searchJobId
   * @param clusterLabel
   * @param messageOffset
   * @param messageLength       @return
   */
  private List<LogElement> getMessagesForSearchJob(String query, String hostName, int messageCount,
      SumoLogicClient sumoClient, String searchJobId, int clusterLabel, int messageOffset, int messageLength,
      int logCollectionMinute) {
    List<LogElement> logElements = new ArrayList<>();
    if (messageCount > 0) {
      do {
        GetMessagesForSearchJobResponse getMessagesForSearchJobResponse =
            sumoClient.getMessagesForSearchJob(searchJobId, messageOffset, messageLength);
        for (LogMessage logMessage : getMessagesForSearchJobResponse.getMessages()) {
          final LogElement sumoLogElement = new LogElement();
          sumoLogElement.setQuery(query);
          sumoLogElement.setHost(hostName);
          sumoLogElement.setClusterLabel(String.valueOf(clusterLabel++));
          sumoLogElement.setCount(1);
          sumoLogElement.setLogCollectionMinute(logCollectionMinute);
          sumoLogElement.setLogMessage(logMessage.getProperties().get("_raw"));
          sumoLogElement.setTimeStamp(Long.parseLong(logMessage.getProperties().get("_timeslice")));
          logElements.add(sumoLogElement);
        }
        messageCount -= messageLength;
        messageOffset += messageLength;
        messageLength = Math.min(messageCount, 1000);
      } while (messageCount > 0);
    }
    return logElements;
  }

  /**
   * Method gets the Search job status response once the state is "DONE GATHERING RESULTS" | "CANCELLED".
   * @param sumoClient
   * @param searchJobId
   * @return
   */
  private GetSearchJobStatusResponse getSearchJobStatusResponse(SumoLogicClient sumoClient, String searchJobId)
      throws InterruptedException {
    GetSearchJobStatusResponse searchJobStatusResponse = null;
    // We will loop until the search job status
    // is either "DONE GATHERING RESULTS" or "CANCELLED".
    while (searchJobStatusResponse == null
        || (!searchJobStatusResponse.getState().equals("DONE GATHERING RESULTS")
               && !searchJobStatusResponse.getState().equals("CANCELLED"))) {
      Thread.sleep(DEFAULT_SLEEP_TIME_IN_MILLIS);

      // Get the latest search job status.
      searchJobStatusResponse = sumoClient.getSearchJobStatus(searchJobId);
      logger.info("Waiting on search job ID: " + searchJobId + " status: " + searchJobStatusResponse.getState());
    }
    if (searchJobStatusResponse.getState().equals("CANCELLED")) {
      throw new CancellationException("The job was cancelled by sumoLogic");
    }
    return searchJobStatusResponse;
  }

  /**
   * Method to save third party call logs for given searchJobStatusResponse.
   * @param apiCallLog
   * @param config
   * @param query
   * @param collectionStartTime
   * @param collectionEndTime
   * @param searchJobStatusResponse
   */
  private void saveThirdPartyCallLogs(ThirdPartyApiCallLog apiCallLog, SumoConfig config, String query,
      String collectionStartTime, String collectionEndTime, Object searchJobStatusResponse, Long requestTimeStamp,
      Long responseTimeStamp, int httpStatus, FieldType responseFieldType) {
    if (apiCallLog == null) {
      apiCallLog = createApiCallLog(config.getAccountId(), Base.GLOBAL_APP_ID, null);
    }
    apiCallLog.setTitle("Fetch request to " + config.getSumoUrl());
    apiCallLog.addFieldToRequest(
        ThirdPartyApiCallField.builder().name(URL_STRING).value(config.getSumoUrl()).type(FieldType.URL).build());
    apiCallLog.addFieldToRequest(
        ThirdPartyApiCallField.builder().name("Search Query").value(query).type(FieldType.TEXT).build());
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name("Start Time")
                                     .value(collectionStartTime)
                                     .type(FieldType.TIMESTAMP)
                                     .build());
    apiCallLog.addFieldToRequest(
        ThirdPartyApiCallField.builder().name("End Time").value(collectionEndTime).type(FieldType.TIMESTAMP).build());

    apiCallLog.setRequestTimeStamp(requestTimeStamp);

    apiCallLog.setResponseTimeStamp(responseTimeStamp);

    apiCallLog.addFieldToResponse(httpStatus, searchJobStatusResponse, responseFieldType);
    delegateLogService.save(config.getAccountId(), apiCallLog);
  }
}
