package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;

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

public class APMDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final Logger logger = LoggerFactory.getLogger(APMDataCollectionTask.class);
  private static final int INITIAL_DELYA_MINS = 2;
  private static final int COLLECT_WINDOW = 1;

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
    for (EncryptedDataDetail encryptedDataDetail : dataCollectionInfo.getEncryptedDataDetails()) {
      try {
        decryptedFields.put(
            encryptedDataDetail.getFieldName(), new String(encryptionService.getDecryptedValue(encryptedDataDetail)));
      } catch (IOException e) {
        throw new WingsException(dataCollectionInfo.getStateType().getName()
            + ": APM data collection : Unable to decrypt field " + encryptedDataDetail.getFieldName());
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

    private APMRestClient getAPMRestClient(final String baseUrl, final APMMetricInfo config) {
      final Retrofit retrofit =
          new Retrofit.Builder()
              .baseUrl(baseUrl)
              .addConverterFactory(JacksonConverterFactory.create())
              .client(Http.getOkHttpClientWithNoProxyValueSet(baseUrl).connectTimeout(30, TimeUnit.SECONDS).build())
              .build();
      return retrofit.create(APMRestClient.class);
    }

    private BiMap<String, Object> resolveDollarReferences(Map<String, String> input) {
      BiMap<String, Object> output = HashBiMap.create();
      if (input == null) {
        return output;
      }
      for (Map.Entry<String, String> entry : input.entrySet()) {
        if (entry.getValue().equals("${start_time}")) {
          // TODO how to know minutes or seconds
          output.put(entry.getKey(), collectionStartTime / 1000L);
        } else if (entry.getValue().equals("${end_time}")) {
          // TODO how to know minutes or seconds
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
          logger.error(dataCollectionInfo.getStateType() + ": Request not successful. Reason: {}", response);
          throw new WingsException(response.errorBody().string());
        }
      } catch (Exception e) {
        throw new WingsException(e);
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

    private List<APMResponseParser.APMResponseData> collect(String baseUrl, APMMetricInfo config) {
      List<APMResponseParser.APMResponseData> responses = new ArrayList<>();
      BiMap<String, Object> headersBiMap = resolveDollarReferences(config.getHeaders());
      BiMap<String, Object> optionsBiMap = resolveDollarReferences(config.getOptions());
      String hostKey = fetchHostKey(optionsBiMap);
      if (!isEmpty(hostKey)) {
        for (String host : dataCollectionInfo.getHosts()) {
          optionsBiMap.put(hostKey, ((String) optionsBiMap.get(hostKey)).replace("${host}", host));
          responses.add(new APMResponseParser.APMResponseData(config.getTransactionName(), config.getMetricName(), host,
              collect(getAPMRestClient(baseUrl, config).collect(config.getUrl(), headersBiMap, optionsBiMap)),
              config.getTag(), config.getResponseMappers()));
        }
      } else {
        responses.add(new APMResponseParser.APMResponseData(config.getTransactionName(), config.getMetricName(),
            config.getHostName(),
            collect(getAPMRestClient(baseUrl, config).collect(config.getUrl(), headersBiMap, optionsBiMap)),
            config.getTag(), config.getResponseMappers()));
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

            for (List<APMMetricInfo> metricInfos : dataCollectionInfo.getMetricEndpoints()) {
              List<APMResponseParser.APMResponseData> apmResponseDataList = new ArrayList<>();
              for (APMMetricInfo metricInfo : metricInfos) {
                apmResponseDataList.addAll(collect(dataCollectionInfo.getBaseUrl(), metricInfo));
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
                newRelicMetricDataRecord.setApplicationId(dataCollectionInfo.getApplicationId());
                records.put(newRelicMetricDataRecord.getName(), newRelicMetricDataRecord.getTimeStamp(),
                    newRelicMetricDataRecord);
              });
            }
            // HeartBeat
            records.put(HARNESS_HEARTBEAT_METRIC_NAME, 0L,
                NewRelicMetricDataRecord.builder()
                    .stateType(getStateType())
                    .name(HARNESS_HEARTBEAT_METRIC_NAME)
                    .applicationId(dataCollectionInfo.getApplicationId())
                    .workflowId(dataCollectionInfo.getWorkflowId())
                    .workflowExecutionId(dataCollectionInfo.getWorkflowExecutionId())
                    .serviceId(dataCollectionInfo.getServiceId())
                    .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                    .dataCollectionMinute(dataCollectionMinute)
                    .timeStamp(collectionStartTime)
                    .level(ClusterLevel.H0)
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
