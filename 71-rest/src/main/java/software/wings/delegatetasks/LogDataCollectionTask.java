package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.VerificationConstants.BODY_STRING;
import static software.wings.common.VerificationConstants.URL_STRING;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
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
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.slf4j.Logger;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.TaskType;
import software.wings.helpers.ext.apm.APMRestClient;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.impl.analysis.CustomLogDataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.log.LogResponseParser;
import software.wings.sm.StateType;
import software.wings.sm.states.CustomLogVerificationState.ResponseMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Praveen
 */
@Slf4j
public class LogDataCollectionTask extends AbstractDelegateDataCollectionTask {
  @Inject private LogAnalysisStoreService logAnalysisStoreService;

  @Inject private DelegateLogService delegateLogService;
  private CustomLogDataCollectionInfo dataCollectionInfo;
  private Map<String, String> decryptedFields = new HashMap<>();

  public LogDataCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected StateType getStateType() {
    return dataCollectionInfo.getStateType();
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

    private String fetchLogs(final String url, Map<String, String> headers, Map<String, String> options,
        Map<String, Object> body, String query, Set<String> hosts, String hostNameSeparator) {
      try {
        BiMap<String, Object> headersBiMap = resolveDollarReferences(headers);
        BiMap<String, Object> optionsBiMap = resolveDollarReferences(options);
        final long startTime = collectionStartTime;
        final long endTime =
            is24X7Task() ? dataCollectionInfo.getEndTime() : collectionStartTime + TimeUnit.MINUTES.toMillis(1);

        String concatenatedHostQuery = CustomDataCollectionUtils.getConcatenatedQuery(hosts, hostNameSeparator);
        String resolvedUrl = CustomDataCollectionUtils.resolvedUrl(url, null, startTime, endTime, null);
        resolvedUrl = resolveDollarReferencesOfSecrets(resolvedUrl);

        Map<String, Object> resolvedBody =
            CustomDataCollectionUtils.resolveMap(body, concatenatedHostQuery, startTime, endTime, query);
        String bodyToLog = JsonUtils.asJson(resolvedBody);
        String resolvedBodyStr = resolveDollarReferencesOfSecrets(JsonUtils.asJson(resolvedBody));
        resolvedBody = new JSONObject(resolvedBodyStr).toMap();

        Call<Object> request;
        if (isNotEmpty(body)) {
          request = getRestClient(dataCollectionInfo.getBaseUrl())
                        .postCollect(resolvedUrl, headersBiMap, optionsBiMap, resolvedBody);
        } else {
          request = getRestClient(dataCollectionInfo.getBaseUrl()).collect(resolvedUrl, headersBiMap, optionsBiMap);
        }
        ThirdPartyApiCallLog apiCallLog = createApiCallLog(dataCollectionInfo.getStateExecutionId());
        apiCallLog.setTitle("Fetch request to " + resolvedUrl);
        apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                         .name(URL_STRING)
                                         .value(request.request().url().toString())
                                         .type(FieldType.URL)
                                         .build());
        apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());

        if (isNotEmpty(bodyToLog)) {
          apiCallLog.addFieldToRequest(
              ThirdPartyApiCallField.builder().name(BODY_STRING).value(bodyToLog).type(FieldType.JSON).build());
        }
        Response<Object> response;
        try {
          response = request.execute();
        } catch (Exception e) {
          apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
          apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
          delegateLogService.save(getAccountId(), apiCallLog);
          throw new WingsException(
              "Unsuccessful response while fetching data from provider. Error message: " + e.getMessage());
        }
        apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
        if (response.isSuccessful()) {
          apiCallLog.addFieldToResponse(response.code(), response.body(), FieldType.JSON);
          delegateLogService.save(getAccountId(), apiCallLog);
          return JsonUtils.asJson(response.body());
        } else {
          apiCallLog.addFieldToResponse(response.code(), response.errorBody(), FieldType.TEXT);
          delegateLogService.save(getAccountId(), apiCallLog);
          throw new WingsException("Exception while fetching logs from provider. Error code " + response.code());
        }
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
            // go fetch the logs first
            String searchResponse = fetchLogs(logDataInfo.getKey(), dataCollectionInfo.getHeaders(),
                dataCollectionInfo.getOptions(), dataCollectionInfo.getBody(), dataCollectionInfo.getQuery(),
                dataCollectionInfo.getHosts(), dataCollectionInfo.getHostnameSeparator());

            LogResponseParser.LogResponseData data = new LogResponseParser.LogResponseData(searchResponse,
                dataCollectionInfo.getHosts(), dataCollectionInfo.isShouldInspectHosts(), logDataInfo.getValue());
            // parse the results that were fetched.
            List<LogElement> logs = new LogResponseParser().extractLogs(data);
            int i = 0;
            String tempHost = dataCollectionInfo.getHosts().iterator().next();
            for (LogElement log : logs) {
              log.setLogCollectionMinute(logCollectionMinute);
              log.setClusterLabel(String.valueOf(i++));
              log.setQuery(dataCollectionInfo.getQuery());
              if (!dataCollectionInfo.isShouldInspectHosts()) {
                log.setHost(tempHost);
              }
            }

            for (String host : dataCollectionInfo.getHosts()) {
              addHeartbeat(host, dataCollectionInfo, logCollectionMinute, logs);
            }

            boolean response = logAnalysisStoreService.save(dataCollectionInfo.getStateType(),
                dataCollectionInfo.getAccountId(), dataCollectionInfo.getApplicationId(),
                dataCollectionInfo.getCvConfigId(), dataCollectionInfo.getStateExecutionId(),
                dataCollectionInfo.getWorkflowId(), dataCollectionInfo.getWorkflowExecutionId(),
                dataCollectionInfo.getServiceId(), delegateTaskId, logs);
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
            logger.warn("error fetching logs. Retrying in " + RETRY_SLEEP + "s", ex);
            sleep(RETRY_SLEEP);
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
