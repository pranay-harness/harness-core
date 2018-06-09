package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import io.harness.network.Http;
import io.harness.time.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.DelegateTask;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.apm.APMRestClient;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.apm.APMDataCollectionInfo;
import software.wings.service.impl.apm.APMMetricInfo;
import software.wings.service.impl.apm.APMResponseParser;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class APMDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final Logger logger = LoggerFactory.getLogger(APMDataCollectionTask.class);
  private static final int INITIAL_DELYA_MINS = 2;
  private static final int COLLECT_WINDOW = 1;
  private static Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");

  @Inject private NewRelicDelegateService newRelicDelegateService;
  @Inject private MetricDataStoreService metricStoreService;
  @Inject private EncryptionService encryptionService;

  private APMDataCollectionInfo dataCollectionInfo;
  private Map<String, String> decryptedFields = new HashMap<>();

  public APMDataCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected int getInitialDelayMinutes() {
    return INITIAL_DELYA_MINS;
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(Object[] parameters) {
    dataCollectionInfo = (APMDataCollectionInfo) parameters[0];
    logger.info("apm collection - dataCollectionInfo: {}", dataCollectionInfo);
    char[] decryptedValue;
    for (EncryptedDataDetail encryptedDataDetail : dataCollectionInfo.getEncryptedDataDetails()) {
      try {
        decryptedValue = encryptionService.getDecryptedValue(encryptedDataDetail);
        if (decryptedValue != null) {
          decryptedFields.put(encryptedDataDetail.getFieldName(), new String(decryptedValue));
        }
      } catch (IOException e) {
        throw new WingsException(dataCollectionInfo.getStateType().getName()
                + ": APM data collection : Unable to decrypt field " + encryptedDataDetail.getFieldName(),
            e);
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
  protected StateType getStateType() {
    return dataCollectionInfo.getStateType();
  }

  @Override
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) throws IOException {
    return new APMMetricCollector(dataCollectionInfo, taskResult);
  }

  private class APMMetricCollector implements Runnable {
    private final APMDataCollectionInfo dataCollectionInfo;
    private final DataCollectionTaskResult taskResult;
    private long collectionStartTime;
    private int dataCollectionMinute;

    private APMMetricCollector(APMDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.taskResult = taskResult;
      this.collectionStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime());
      this.dataCollectionMinute = dataCollectionInfo.getDataCollectionMinute();
    }

    private APMRestClient getAPMRestClient(final String baseUrl) {
      final Retrofit retrofit =
          new Retrofit.Builder()
              .baseUrl(baseUrl)
              .addConverterFactory(JacksonConverterFactory.create())
              .client(Http.getOkHttpClientWithNoProxyValueSet(baseUrl).connectTimeout(30, TimeUnit.SECONDS).build())
              .build();
      return retrofit.create(APMRestClient.class);
    }

    private String resolveDollarReferences(String url, String host) {
      if (isEmpty(url)) {
        return url;
      }
      String result = url;
      if (result.contains("${start_time}")) {
        result = result.replace("${start_time}", String.valueOf(collectionStartTime));
      }
      if (result.contains("${end_time}")) {
        result = result.replace(
            "${end_time}", String.valueOf(collectionStartTime + TimeUnit.MINUTES.toMillis(COLLECT_WINDOW)));
      }
      if (result.contains("${start_time_seconds}")) {
        result = result.replace("${start_time_seconds}", String.valueOf(collectionStartTime / 1000L));
      }
      if (result.contains("${end_time_seconds}")) {
        result = result.replace("${end_time_seconds}",
            String.valueOf((collectionStartTime + TimeUnit.MINUTES.toMillis(COLLECT_WINDOW)) / 1000L));
      }

      if (result.contains("${host}")) {
        result = result.replace("${host}", host);
      }

      Matcher matcher = pattern.matcher(result);
      while (matcher.find()) {
        result = result.replace(
            matcher.group(), decryptedFields.get(matcher.group().substring(2, matcher.group().length() - 1)));
      }

      return result;
    }

    private BiMap<String, Object> resolveDollarReferences(Map<String, String> input) {
      BiMap<String, Object> output = HashBiMap.create();
      if (input == null) {
        return output;
      }
      for (Map.Entry<String, String> entry : input.entrySet()) {
        if (entry.getValue().equals("${start_time}")) {
          output.put(entry.getKey(), collectionStartTime);
        } else if (entry.getValue().equals("${end_time}")) {
          output.put(entry.getKey(), collectionStartTime + TimeUnit.MINUTES.toMillis(COLLECT_WINDOW));
        }
        if (entry.getValue().equals("${start_time_seconds}")) {
          output.put(entry.getKey(), collectionStartTime / 1000L);
        } else if (entry.getValue().equals("${end_time_seconds}")) {
          output.put(entry.getKey(), (collectionStartTime + TimeUnit.MINUTES.toMillis(COLLECT_WINDOW)) / 1000L);
        } else if (entry.getValue().equals("${host}")) {
          output.put(entry.getKey(), entry.getValue());
        } else if (entry.getValue().startsWith("${")) {
          output.put(entry.getKey(), decryptedFields.get(entry.getValue().substring(2, entry.getValue().length() - 1)));
        } else {
          output.put(entry.getKey(), entry.getValue());
        }
      }

      return output;
    }

    private String collect(Call<Object> request) {
      final Response<Object> response;
      try {
        response = request.execute();
        if (response.isSuccessful()) {
          return JsonUtils.asJson(response.body());
        } else {
          logger.error(dataCollectionInfo.getStateType() + ": Request not successful. Reason: {}, {}", response.code(),
              response.message());
          throw new WingsException(response.errorBody().string());
        }
      } catch (Exception e) {
        throw new WingsException("Unable to collect data " + e.getMessage(), e);
      }
    }

    private String fetchHostKey(BiMap<String, Object> optionsBiMap) {
      for (Map.Entry<String, Object> entry : optionsBiMap.entrySet()) {
        if (entry.getValue() instanceof String) {
          if (((String) entry.getValue()).contains("${host}")) {
            return entry.getKey();
          }
        }
      }
      return "";
    }

    private List<APMResponseParser.APMResponseData> collect(String baseUrl, Map<String, String> headers,
        Map<String, String> options, final String url, List<APMMetricInfo> metricInfos) {
      List<APMResponseParser.APMResponseData> responses = new ArrayList<>();

      //      /**
      //       * Headers and options are special fields.
      //       * See {@APMVerificationConfig.inputMap} for format details.
      //       */
      //      if (decryptedFields.containsKey("headers")) {
      //        config.getHeaders().putAll(APMVerificationConfig.inputMap(decryptedFields.get("headers")));
      //      }
      //      if (decryptedFields.containsKey("options")) {
      //        config.getOptions().putAll(APMVerificationConfig.inputMap(decryptedFields.get("options")));
      //      }

      BiMap<String, Object> headersBiMap = resolveDollarReferences(headers);
      BiMap<String, Object> optionsBiMap = resolveDollarReferences(options);
      // TODO Resolve dollar references for url
      String hostKey = fetchHostKey(optionsBiMap);
      if (!isEmpty(hostKey) || url.contains("${host}")) {
        for (String host : dataCollectionInfo.getHosts()) {
          BiMap<String, Object> curOptionsBiMap = resolveDollarReferences(options);
          if (!isEmpty(hostKey)) {
            curOptionsBiMap.put(hostKey, ((String) curOptionsBiMap.get(hostKey)).replace("${host}", host));
          }
          // This needs to be a new variable else it will overwrite url for next iteration
          String curUrl = resolveDollarReferences(url, host);
          responses.add(new APMResponseParser.APMResponseData(
              host, collect(getAPMRestClient(baseUrl).collect(curUrl, headersBiMap, curOptionsBiMap)), metricInfos));
        }
      } else {
        String curUrl = resolveDollarReferences(url, "");
        responses.add(new APMResponseParser.APMResponseData(
            "", collect(getAPMRestClient(baseUrl).collect(curUrl, headersBiMap, optionsBiMap)), metricInfos));
      }

      return responses;
    }

    @Override
    public void run() {
      try {
        int retry = 0;
        while (!completed.get() && retry < RETRIES) {
          try {
            TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = TreeBasedTable.create();

            List<APMResponseParser.APMResponseData> apmResponseDataList = new ArrayList<>();
            for (Map.Entry<String, List<APMMetricInfo>> metricInfoEntry :
                dataCollectionInfo.getMetricEndpoints().entrySet()) {
              apmResponseDataList.addAll(collect(dataCollectionInfo.getBaseUrl(), dataCollectionInfo.getHeaders(),
                  dataCollectionInfo.getOptions(), metricInfoEntry.getKey(), metricInfoEntry.getValue()));
            }
            Collection<NewRelicMetricDataRecord> newRelicMetricDataRecords =
                new APMResponseParser().extract(apmResponseDataList);

            newRelicMetricDataRecords.stream().forEach(newRelicMetricDataRecord -> {
              newRelicMetricDataRecord.setServiceId(dataCollectionInfo.getServiceId());
              newRelicMetricDataRecord.setStateExecutionId(dataCollectionInfo.getStateExecutionId());
              newRelicMetricDataRecord.setWorkflowExecutionId(dataCollectionInfo.getWorkflowExecutionId());
              newRelicMetricDataRecord.setWorkflowId(dataCollectionInfo.getWorkflowId());
              newRelicMetricDataRecord.setStateType(dataCollectionInfo.getStateType());
              newRelicMetricDataRecord.setDataCollectionMinute(dataCollectionMinute);
              newRelicMetricDataRecord.setAppId(dataCollectionInfo.getApplicationId());
              records.put(newRelicMetricDataRecord.getName() + newRelicMetricDataRecord.getHost(),
                  newRelicMetricDataRecord.getTimeStamp(), newRelicMetricDataRecord);
            });

            // HeartBeat
            records.put(HARNESS_HEARTBEAT_METRIC_NAME, 0L,
                NewRelicMetricDataRecord.builder()
                    .stateType(getStateType())
                    .name(HARNESS_HEARTBEAT_METRIC_NAME)
                    .workflowId(dataCollectionInfo.getWorkflowId())
                    .workflowExecutionId(dataCollectionInfo.getWorkflowExecutionId())
                    .serviceId(dataCollectionInfo.getServiceId())
                    .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                    .appId(dataCollectionInfo.getApplicationId())
                    .dataCollectionMinute(dataCollectionMinute)
                    .timeStamp(collectionStartTime)
                    .level(ClusterLevel.H0)
                    .groupName(DEFAULT_GROUP_NAME)
                    .build());

            List<NewRelicMetricDataRecord> allMetricRecords = getAllMetricRecords(records);

            if (!saveMetrics(dataCollectionInfo.getAccountId(), dataCollectionInfo.getApplicationId(),
                    dataCollectionInfo.getStateExecutionId(), allMetricRecords)) {
              retry = RETRIES;
              taskResult.setErrorMessage("Cannot save new apm metric records to Harness. Server returned error");
              throw new RuntimeException(dataCollectionInfo.getStateType()
                  + ": Cannot save new apm metric records to Harness. Server returned error");
            }
            logger.info(dataCollectionInfo.getStateType() + ": Sent {} metric records to the server for minute {}",
                allMetricRecords.size(), dataCollectionMinute);

            dataCollectionMinute++;
            collectionStartTime += TimeUnit.MINUTES.toMillis(1);
            break;

          } catch (Exception ex) {
            if (++retry >= RETRIES) {
              taskResult.setStatus(DataCollectionTaskResult.DataCollectionTaskStatus.FAILURE);
              completed.set(true);
              break;
            } else {
              if (retry == 1) {
                if (ex instanceof WingsException) {
                  if (((WingsException) ex).getParams().containsKey("reason")) {
                    taskResult.setErrorMessage((String) ((WingsException) ex).getParams().get("reason"));
                  } else {
                    taskResult.setErrorMessage(ex.getMessage());
                  }
                } else {
                  taskResult.setErrorMessage(ex.getMessage());
                }
              }
              logger.warn("error fetching apm metrics for minute " + dataCollectionMinute + ". retrying in "
                      + RETRY_SLEEP + "s",
                  ex);
              sleep(RETRY_SLEEP);
            }
          }
        }
      } catch (Exception e) {
        completed.set(true);
        taskResult.setStatus(DataCollectionTaskResult.DataCollectionTaskStatus.FAILURE);
        taskResult.setErrorMessage("error fetching apm metrics for minute " + dataCollectionMinute);
        logger.error(
            dataCollectionInfo.getStateType() + ": error fetching apm metrics for minute " + dataCollectionMinute, e);
      }

      if (completed.get()) {
        logger.info(dataCollectionInfo.getStateType() + ": Shutting down apm data collection");
        shutDownCollection();
        return;
      }
    }
  }

  private List<NewRelicMetricDataRecord> getAllMetricRecords(
      TreeBasedTable<String, Long, NewRelicMetricDataRecord> records) {
    List<NewRelicMetricDataRecord> rv = new ArrayList<>();
    records.cellSet().forEach(cell -> rv.add(cell.getValue()));
    return rv;
  }
}
