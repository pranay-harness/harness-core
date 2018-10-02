package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.Constants.URL_STRING;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.network.Http;
import io.harness.time.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.helpers.ext.apm.APMRestClient;
import software.wings.security.encryption.EncryptedDataDetail;
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
import software.wings.utils.JsonUtils;
import software.wings.utils.Misc;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Praveen
 */
public class LogDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final Logger logger = LoggerFactory.getLogger(LogDataCollectionTask.class);

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
  protected DataCollectionTaskResult initDataCollection(Object[] parameters) {
    dataCollectionInfo = (CustomLogDataCollectionInfo) parameters[0];
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
          throw new WingsException(dataCollectionInfo.getStateType().getName()
                  + ": Log data collection : Unable to decrypt field " + encryptedDataDetail.getFieldName(),
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
    final Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(JacksonConverterFactory.create())
            .client(Http.getOkHttpClientWithNoProxyValueSet(baseUrl).connectTimeout(120, TimeUnit.SECONDS).build())
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
      this.logCollectionMinute = dataCollectionInfo.getStartMinute();
      this.collectionStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime())
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

    private String fetchLogs(final String url, Map<String, String> headers, Map<String, String> options) {
      try {
        BiMap<String, Object> headersBiMap = resolveDollarReferences(headers);
        BiMap<String, Object> optionsBiMap = resolveDollarReferences(options);
        final long startTime = collectionStartTime;
        final long endTime = collectionStartTime + TimeUnit.MINUTES.toMillis(1);

        String resolvedUrl = CustomDataCollectionUtil.resolvedUrl(url, null, startTime, endTime);
        final Call<Object> request =
            getRestClient(dataCollectionInfo.getBaseUrl()).collect(resolvedUrl, headersBiMap, optionsBiMap);
        ThirdPartyApiCallLog apiCallLog = createApiCallLog(dataCollectionInfo.getStateExecutionId());
        apiCallLog.setTitle("Fetch request to " + resolvedUrl);
        apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                         .name(URL_STRING)
                                         .value(request.request().url().toString())
                                         .type(FieldType.URL)
                                         .build());
        apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
        final Response<Object> response = request.execute();
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

    public void run() {
      int retry = 0;
      while (!completed.get() && retry < RETRIES) {
        try {
          for (Map.Entry<String, Map<String, ResponseMapper>> logDataInfo :
              dataCollectionInfo.getLogResponseDefinition().entrySet()) {
            // go fetch the logs first
            String searchResponse =
                fetchLogs(logDataInfo.getKey(), dataCollectionInfo.getHeaders(), dataCollectionInfo.getOptions());
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
              // add heartbeat
              logs.add(LogElement.builder()
                           .query(dataCollectionInfo.getQuery())
                           .clusterLabel("-1")
                           .host(host)
                           .count(0)
                           .logMessage("")
                           .timeStamp(0)
                           .logCollectionMinute(logCollectionMinute)
                           .build());
            }

            boolean response = logAnalysisStoreService.save(dataCollectionInfo.getStateType(),
                dataCollectionInfo.getAccountId(), dataCollectionInfo.getApplicationId(),
                dataCollectionInfo.getStateExecutionId(), dataCollectionInfo.getWorkflowId(),
                dataCollectionInfo.getWorkflowExecutionId(), dataCollectionInfo.getServiceId(), delegateTaskId, logs);
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
        } catch (Exception ex) {
          if (++retry >= RETRIES) {
            taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
            completed.set(true);
            throw new WingsException(
                "Error collecting logs for stateExecutionId: " + dataCollectionInfo.getStateExecutionId());
          } else {
            /*
             * Save the exception from the first attempt. This is usually
             * more meaningful to trouble shoot.
             */
            if (retry == 1) {
              taskResult.setErrorMessage(Misc.getMessage(ex));
            }
            logger.warn("error fetching logs. Retrying in " + RETRY_SLEEP + "s", ex);
            sleep(RETRY_SLEEP);
          }
        }
      }
    }
  }
}
