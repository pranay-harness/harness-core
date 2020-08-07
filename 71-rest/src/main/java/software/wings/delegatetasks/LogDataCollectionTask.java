package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.VerificationConstants.AZURE_BASE_URL;
import static software.wings.common.VerificationConstants.AZURE_TOKEN_URL;
import static software.wings.common.VerificationConstants.DATA_COLLECTION_RETRY_SLEEP;
import static software.wings.common.VerificationConstants.NON_HOST_PREVIOUS_ANALYSIS;
import static software.wings.common.VerificationConstants.URL_BODY_APPENDER;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.eraro.ErrorCode;
import io.harness.exception.VerificationOperationException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.JsonUtils;
import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.cv.RequestExecutor;
import software.wings.helpers.ext.apm.APMRestClient;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.AzureLogAnalyticsConnectionDetails;
import software.wings.service.impl.analysis.CustomLogDataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.log.LogResponseParser;
import software.wings.sm.StateType;
import software.wings.sm.states.CustomLogVerificationState.ResponseMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Praveen
 */
@Slf4j
public class LogDataCollectionTask extends AbstractDelegateDataCollectionTask {
  @Inject private LogAnalysisStoreService logAnalysisStoreService;
  @Inject private RequestExecutor requestExecutor;
  @Inject private DelegateLogService delegateLogService;
  private CustomLogDataCollectionInfo dataCollectionInfo;
  private Map<String, String> decryptedFields = new HashMap<>();
  private static final String DATADOG_API_MASK = "api_key=([^&]*)&application_key=([^&]*)";

  // special case for azure. This is unfortunately a hack
  private AzureLogAnalyticsConnectionDetails azureLogAnalyticsConnectionDetails;

  public LogDataCollectionTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  protected StateType getStateType() {
    return dataCollectionInfo.getStateType();
  }

  @Override
  protected int getInitialDelayMinutes() {
    return dataCollectionInfo.getDelayMinutes();
  }

  @Override
  protected boolean is24X7Task() {
    return getTaskType().equals(TaskType.CUSTOM_COLLECT_24_7_LOG_DATA.name());
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(TaskParameters parameters) {
    dataCollectionInfo = (CustomLogDataCollectionInfo) parameters;
    logger.info("Log collection - dataCollectionInfo: {}", dataCollectionInfo);
    if (!isEmpty(dataCollectionInfo.getEncryptedDataDetails())) {
      char[] decryptedValue;
      for (EncryptedDataDetail encryptedDataDetail : dataCollectionInfo.getEncryptedDataDetails()) {
        try {
          decryptedValue = encryptionService.getDecryptedValue(encryptedDataDetail);
          if (decryptedValue != null) {
            decryptedFields.put(encryptedDataDetail.getFieldName(), new String(decryptedValue));
          }

        } catch (IOException e) {
          throw new VerificationOperationException(ErrorCode.DEFAULT_ERROR_CODE,
              dataCollectionInfo.getStateType().getName() + ": Log data collection : Unable to decrypt field "
                  + encryptedDataDetail.getFieldName(),
              e);
        }
      }
    }
    return DataCollectionTaskResult.builder()
        .status(DataCollectionTaskResult.DataCollectionTaskStatus.SUCCESS)
        .stateType(dataCollectionInfo.getStateType())
        .build();
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) {
    return new LogDataCollector(getTaskId(), dataCollectionInfo, taskResult);
  }

  private APMRestClient getRestClient(final String baseUrl) {
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(baseUrl)
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .client(getUnsafeHttpClient(baseUrl))
                                  .build();
    return retrofit.create(APMRestClient.class);
  }

  private class LogDataCollector implements Runnable {
    private final CustomLogDataCollectionInfo dataCollectionInfo;
    private long collectionStartTime;
    private int logCollectionMinute;
    private DataCollectionTaskResult taskResult;
    private String delegateTaskId;

    private LogDataCollector(
        String delegateTaskId, CustomLogDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult) {
      this.delegateTaskId = delegateTaskId;
      this.dataCollectionInfo = dataCollectionInfo;
      this.logCollectionMinute = is24X7Task() ? (int) TimeUnit.MILLISECONDS.toMinutes(dataCollectionInfo.getEndTime())
                                              : dataCollectionInfo.getStartMinute();
      this.collectionStartTime = is24X7Task() ? dataCollectionInfo.getStartTime()
                                              : Timestamp.minuteBoundary(dataCollectionInfo.getStartTime())
              + logCollectionMinute * TimeUnit.MINUTES.toMillis(1);
      this.taskResult = taskResult;
    }

    private Map<String, String> fetchAdditionalHeaders(CustomLogDataCollectionInfo dataCollectionInfo) {
      // Special case for getting the bearer token for azure log analytics
      if (!dataCollectionInfo.getBaseUrl().contains(AZURE_BASE_URL)) {
        return null;
      }
      Map<String, Object> resolvedOptions = resolveDollarReferences(dataCollectionInfo.getOptions());
      String clientId = azureLogAnalyticsConnectionDetails == null ? (String) resolvedOptions.get("client_id")
                                                                   : azureLogAnalyticsConnectionDetails.getClientId();
      String clientSecret = azureLogAnalyticsConnectionDetails == null
          ? (String) resolvedOptions.get("client_secret")
          : azureLogAnalyticsConnectionDetails.getClientSecret();
      String tenantId = azureLogAnalyticsConnectionDetails == null ? (String) resolvedOptions.get("tenant_id")
                                                                   : azureLogAnalyticsConnectionDetails.getTenantId();
      if (azureLogAnalyticsConnectionDetails == null) {
        // saving the details in this object so we can remove the details from the request parameters
        azureLogAnalyticsConnectionDetails = AzureLogAnalyticsConnectionDetails.builder()
                                                 .clientId(clientId)
                                                 .clientSecret(clientSecret)
                                                 .tenantId(tenantId)
                                                 .build();

        dataCollectionInfo.getOptions().remove("client_id");
        dataCollectionInfo.getOptions().remove("tenant_id");
        dataCollectionInfo.getOptions().remove("client_secret");
      }

      Preconditions.checkNotNull(
          clientId, "client_id parameter cannot be null when collecting data from azure log analytics");
      Preconditions.checkNotNull(
          tenantId, "tenant_id parameter cannot be null when collecting data from azure log analytics");
      Preconditions.checkNotNull(
          clientSecret, "client_secret parameter cannot be null when collecting data from azure log analytics");
      String urlForToken = tenantId + "/oauth2/token";

      Map<String, String> bearerTokenHeader = new HashMap<>();
      bearerTokenHeader.put("Content-Type", "application/x-www-form-urlencoded");
      Call<Object> bearerTokenCall = getRestClient(AZURE_TOKEN_URL)
                                         .getAzureBearerToken(urlForToken, bearerTokenHeader, "client_credentials",
                                             clientId, AZURE_BASE_URL, clientSecret);

      Object response = requestExecutor.executeRequest(bearerTokenCall);
      Map<String, Object> responseMap = new JSONObject(JsonUtils.asJson(response)).toMap();
      String bearerToken = (String) responseMap.get("access_token");

      String headerVal = "Bearer " + bearerToken;
      Map<String, String> header = new HashMap<>();
      header.put("Authorization", headerVal);
      return header;
    }

    private BiMap<String, Object> resolveDollarReferences(Map<String, String> input) {
      BiMap<String, Object> output = HashBiMap.create();
      if (input == null) {
        return output;
      }
      for (Map.Entry<String, String> entry : input.entrySet()) {
        String headerVal = entry.getValue();
        if (!headerVal.contains("${")) {
          output.put(entry.getKey(), entry.getValue());
          continue;
        }
        while (headerVal.contains("${")) {
          int startIndex = headerVal.indexOf("${");
          int endIndex = headerVal.indexOf('}', startIndex);
          String fieldName = headerVal.substring(startIndex + 2, endIndex);
          String headerBeforeIndex = headerVal.substring(0, startIndex);

          headerVal = headerBeforeIndex + decryptedFields.get(fieldName) + headerVal.substring(endIndex + 1);
          output.put(entry.getKey(), headerVal);
        }
      }

      return output;
    }

    private String resolveDollarReferencesOfSecrets(String input) {
      while (input.contains("${")) {
        int startIndex = input.indexOf("${");
        int endIndex = input.indexOf('}', startIndex);
        String fieldName = input.substring(startIndex + 2, endIndex);
        String headerBeforeIndex = input.substring(0, startIndex);
        if (!decryptedFields.containsKey(fieldName)) {
          // this could be a ${startTime}, so we're ignoring and moving on
          continue;
        }
        input = headerBeforeIndex + decryptedFields.get(fieldName) + input.substring(endIndex + 1);
      }
      return input;
    }
    private Map<String, String> getStringsToMask() {
      Map<String, String> maskFields = new HashMap<>();
      if (isNotEmpty(decryptedFields)) {
        decryptedFields.forEach((k, v) -> { maskFields.put(v, "<" + k + ">"); });
      }
      return maskFields;
    }

    private String fetchLogs(String url, Map<String, String> headers, Map<String, String> options,
        Map<String, Object> body, String query, String host, String hostNameSeparator) {
      try {
        BiMap<String, Object> headersBiMap = resolveDollarReferences(headers);
        BiMap<String, Object> optionsBiMap = resolveDollarReferences(options);
        final long startTime = collectionStartTime;
        final long endTime =
            is24X7Task() ? dataCollectionInfo.getEndTime() : collectionStartTime + TimeUnit.MINUTES.toMillis(1);
        String bodyStr = null;
        // We're doing this check because in SG24/7 we allow only one logCollection. So the body is present in the
        // dataCollectionInfo. In workflow there can be one body per logCollection in setup.
        // So we're taking care of both.
        String[] urlAndBody = url.split(URL_BODY_APPENDER);
        url = urlAndBody[0];
        if (isEmpty(body)) {
          bodyStr = urlAndBody.length > 1 ? urlAndBody[1] : "";
        } else {
          bodyStr = JsonUtils.asJson(body);
        }
        String resolvedUrl =
            CustomDataCollectionUtils.resolvedUrl(url, host, startTime, endTime, dataCollectionInfo.getQuery());
        resolvedUrl = resolveDollarReferencesOfSecrets(resolvedUrl);

        String resolvedBodyStr =
            CustomDataCollectionUtils.resolvedUrl(bodyStr, host, startTime, endTime, dataCollectionInfo.getQuery());
        String bodyToLog = resolvedBodyStr;
        resolvedBodyStr = resolveDollarReferencesOfSecrets(resolvedBodyStr);
        Map<String, Object> resolvedBody = isNotEmpty(resolvedBodyStr) ? new JSONObject(resolvedBodyStr).toMap() : null;

        Call<Object> request;
        if (isNotEmpty(resolvedBody)) {
          request = getRestClient(dataCollectionInfo.getBaseUrl())
                        .postCollect(resolvedUrl, headersBiMap, optionsBiMap, resolvedBody);
        } else {
          request = getRestClient(dataCollectionInfo.getBaseUrl()).collect(resolvedUrl, headersBiMap, optionsBiMap);
        }
        ThirdPartyApiCallLog apiCallLog =
            ThirdPartyApiCallLog.fromDetails(createApiCallLog(dataCollectionInfo.getStateExecutionId()));
        apiCallLog.setTitle("Fetch request to: " + dataCollectionInfo.getBaseUrl());
        Object response = requestExecutor.executeRequest(apiCallLog, request, getStringsToMask());
        return JsonUtils.asJson(response);

      } catch (Exception ex) {
        String err = ex.getMessage()
            + "Exception occurred while fetching logs. StateExecutionId: " + dataCollectionInfo.getStateExecutionId();
        logger.error(err);
        throw new WingsException(err);
      }
    }

    @Override
    @SuppressWarnings("PMD")
    public void run() {
      int retry = 0;
      while (!completed.get() && retry < RETRIES) {
        try {
          for (Map.Entry<String, Map<String, ResponseMapper>> logDataInfo :
              dataCollectionInfo.getLogResponseDefinition().entrySet()) {
            List<LogElement> logs = new ArrayList<>();
            String tempHost = dataCollectionInfo.getHosts().iterator().next();
            Map<String, String> additionalHeaders = fetchAdditionalHeaders(dataCollectionInfo);
            if (isNotEmpty(additionalHeaders)) {
              if (dataCollectionInfo.getHeaders() == null) {
                dataCollectionInfo.setHeaders(new HashMap<>());
              }
              additionalHeaders.forEach((key, val) -> dataCollectionInfo.getHeaders().put(key, val));
            }
            // go fetch the logs first
            if (!dataCollectionInfo.isShouldDoHostBasedFiltering()) {
              // this query is not host based. So we should not make one call per host
              String searchResponse = fetchLogs(logDataInfo.getKey(), dataCollectionInfo.getHeaders(),
                  dataCollectionInfo.getOptions(), dataCollectionInfo.getBody(), dataCollectionInfo.getQuery(),
                  tempHost, dataCollectionInfo.getHostnameSeparator());

              LogResponseParser.LogResponseData data = new LogResponseParser.LogResponseData(searchResponse,
                  dataCollectionInfo.getHosts(), dataCollectionInfo.isShouldDoHostBasedFiltering(),
                  dataCollectionInfo.isFixedHostName(), logDataInfo.getValue());
              // parse the results that were fetched.
              List<LogElement> curLogs = new LogResponseParser().extractLogs(data);
              logs.addAll(curLogs);
            } else {
              dataCollectionInfo.getHosts().forEach(host -> {
                String searchResponse = fetchLogs(logDataInfo.getKey(), dataCollectionInfo.getHeaders(),
                    dataCollectionInfo.getOptions(), dataCollectionInfo.getBody(), dataCollectionInfo.getQuery(), host,
                    dataCollectionInfo.getHostnameSeparator());

                LogResponseParser.LogResponseData data = new LogResponseParser.LogResponseData(searchResponse,
                    dataCollectionInfo.getHosts(), dataCollectionInfo.isShouldDoHostBasedFiltering(),
                    dataCollectionInfo.isFixedHostName(), logDataInfo.getValue());
                // parse the results that were fetched.
                List<LogElement> curLogs = new LogResponseParser().extractLogs(data);
                logs.addAll(curLogs);
              });
            }

            int i = 0;

            for (LogElement log : logs) {
              log.setLogCollectionMinute(logCollectionMinute);
              log.setClusterLabel(String.valueOf(i++));
              log.setQuery(dataCollectionInfo.getQuery());
              if (log.getHost() == null) {
                log.setHost(tempHost);
              }
            }

            List<LogElement> filteredLogs = new ArrayList<>(logs);
            Set<String> allHosts = new HashSet<>(dataCollectionInfo.getHosts());
            if (dataCollectionInfo.isShouldDoHostBasedFiltering()) {
              filteredLogs = logs.stream()
                                 .filter(log -> dataCollectionInfo.getHosts().contains(log.getHost()))
                                 .collect(Collectors.toList());
            }
            filteredLogs.forEach(log -> allHosts.add(log.getHost()));
            for (String host : allHosts) {
              addHeartbeat(host, dataCollectionInfo, logCollectionMinute, filteredLogs);
            }

            if (!dataCollectionInfo.isShouldDoHostBasedFiltering()) {
              addHeartbeat(NON_HOST_PREVIOUS_ANALYSIS, dataCollectionInfo, logCollectionMinute, filteredLogs);
            }

            boolean response = logAnalysisStoreService.save(dataCollectionInfo.getStateType(),
                dataCollectionInfo.getAccountId(), dataCollectionInfo.getApplicationId(),
                dataCollectionInfo.getCvConfigId(), dataCollectionInfo.getStateExecutionId(),
                dataCollectionInfo.getWorkflowId(), dataCollectionInfo.getWorkflowExecutionId(),
                dataCollectionInfo.getServiceId(), delegateTaskId, filteredLogs);
            if (!response) {
              logger.error(
                  "Error while saving logs for stateExecutionId: {}", dataCollectionInfo.getStateExecutionId());
            }
          }
          logCollectionMinute++;
          collectionStartTime += TimeUnit.MINUTES.toMillis(1);
          if (logCollectionMinute >= dataCollectionInfo.getCollectionTime()) {
            // We are done with all data collection, so setting task status to success and quitting.
            logger.info(
                "Completed Log collection task. So setting task status to success and quitting. StateExecutionId {}",
                dataCollectionInfo.getStateExecutionId());
            completed.set(true);
            taskResult.setStatus(DataCollectionTaskStatus.SUCCESS);
          }
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
            logger.warn("error fetching logs. Retrying in " + DATA_COLLECTION_RETRY_SLEEP + "s", ex);
            sleep(DATA_COLLECTION_RETRY_SLEEP);
          }
        }
      }

      if (completed.get()) {
        logger.info("Shutting down log collection {}", dataCollectionInfo.getStateExecutionId());
        shutDownCollection();
      }
    }
  }
}
