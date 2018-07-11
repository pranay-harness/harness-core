package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.delegatetasks.SplunkDataCollectionTask.RETRY_SLEEP;
import static software.wings.sm.states.DynatraceState.CONTROL_HOST_NAME;
import static software.wings.sm.states.DynatraceState.TEST_HOST_NAME;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.data.structure.EmptyPredicate;
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
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
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
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class APMDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final Logger logger = LoggerFactory.getLogger(APMDataCollectionTask.class);
  private static final int CANARY_DAYS_TO_COLLECT = 7;

  private static final String BATCH_REGEX = "\\$harness_batch\\{([^,]*),([^}]*)\\}";
  private static final String BATCH_TEXT = "$harness_batch";
  private static final int MAX_HOSTS_PER_BATCH = 15;
  private static Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");

  @Inject private NewRelicDelegateService newRelicDelegateService;
  @Inject private MetricDataStoreService metricStoreService;
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService delegateLogService;
  private int initialDelayMins = 2;
  private int collectionWindow = 1;

  private APMDataCollectionInfo dataCollectionInfo;
  private Map<String, String> decryptedFields = new HashMap<>();

  public APMDataCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected int getInitialDelayMinutes() {
    return initialDelayMins;
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(Object[] parameters) {
    dataCollectionInfo = (APMDataCollectionInfo) parameters[0];
    collectionWindow =
        dataCollectionInfo.getDataCollectionFrequency() != 0 ? dataCollectionInfo.getDataCollectionFrequency() : 1;
    initialDelayMins = 2;
    logger.info("apm collection - dataCollectionInfo: {}", dataCollectionInfo);

    if (!EmptyPredicate.isEmpty(dataCollectionInfo.getEncryptedDataDetails())) {
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
    private final long collectionStartMinute;
    private long lastEndTime;
    private long currentEndTime;
    private int currentElapsedTime;
    Map<String, Long> hostStartMinuteMap;

    private APMMetricCollector(APMDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.taskResult = taskResult;
      this.collectionStartMinute = Timestamp.currentMinuteBoundary();
      this.collectionStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime());
      this.dataCollectionMinute = dataCollectionInfo.getDataCollectionMinute();
      this.lastEndTime = dataCollectionInfo.getStartTime();
      this.currentElapsedTime = 0;
      hostStartMinuteMap = new HashMap<>();
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

    private String resolvedUrl(String url, String host, long startTime, long endTime) {
      String result = url;
      if (result.contains("${start_time}")) {
        result = result.replace("${start_time}", String.valueOf(startTime));
      }
      if (result.contains("${end_time}")) {
        result = result.replace("${end_time}", String.valueOf(endTime));
      }
      if (result.contains("${start_time_seconds}")) {
        result = result.replace("${start_time_seconds}", String.valueOf(startTime / 1000L));
      }
      if (result.contains("${end_time_seconds}")) {
        result = result.replace("${end_time_seconds}", String.valueOf(endTime / 1000L));
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

    private List<String> resolveDollarReferences(String url, String host, AnalysisComparisonStrategy strategy) {
      if (isEmpty(url)) {
        return Collections.EMPTY_LIST;
      }
      // TODO: Come back and clean up the time variables.
      List<String> result = new ArrayList<>();
      long startTime = lastEndTime;
      long endTime = Timestamp.currentMinuteBoundary();
      currentEndTime = endTime;
      if (TEST_HOST_NAME.equals(host) && strategy == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
        for (int i = 0; i <= CANARY_DAYS_TO_COLLECT; i++) {
          String hostName = getHostNameForTestControl(i);
          long thisEndTime = endTime - TimeUnit.DAYS.toMillis(i);
          long thisStartTime = startTime - TimeUnit.DAYS.toMillis(i);
          if (!hostStartMinuteMap.containsKey(hostName)) {
            hostStartMinuteMap.put(hostName, thisStartTime);
          }
          result.add(resolvedUrl(url, hostName, thisStartTime, thisEndTime));
        }
      } else {
        result.add(resolvedUrl(url, host, startTime, endTime));
      }
      logger.info("Start and end times for minute {} were {} and {}", dataCollectionMinute, startTime, endTime);
      return result;
    }

    private String getHostNameForTestControl(int i) {
      return i == 0 ? TEST_HOST_NAME : CONTROL_HOST_NAME + "-" + i;
    }

    private BiMap<String, Object> resolveDollarReferences(Map<String, String> input) {
      BiMap<String, Object> output = HashBiMap.create();
      if (input == null) {
        return output;
      }
      for (Map.Entry<String, String> entry : input.entrySet()) {
        if (entry.getValue().startsWith("${")) {
          output.put(entry.getKey(), decryptedFields.get(entry.getValue().substring(2, entry.getValue().length() - 1)));
        } else {
          output.put(entry.getKey(), entry.getValue());
        }
      }

      return output;
    }

    private String collect(Call<Object> request, final String urlToLog) {
      final Response<Object> response;
      try {
        ThirdPartyApiCallLog apiCallLog = createApiCallLog(dataCollectionInfo.getStateExecutionId());
        apiCallLog.setRequest(urlToLog);
        apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toEpochSecond());
        response = request.execute();
        apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toEpochSecond());
        apiCallLog.setStatusCode(response.code());
        apiCallLog.setJsonResponse(response.body());
        delegateLogService.save(getAccountId(), apiCallLog);
        if (response.isSuccessful()) {
          return JsonUtils.asJson(response.body());
        } else {
          logger.error(dataCollectionInfo.getStateType() + ": Request not successful. Reason: {}, {}", response.code(),
              response.message());
          throw new WingsException(response.errorBody().string());
        }
      } catch (Exception e) {
        throw new WingsException("Unable to collect data " + Misc.getMessage(e), e);
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
        Map<String, String> options, final String initialUrl, List<APMMetricInfo> metricInfos,
        AnalysisComparisonStrategy strategy) throws IOException {
      List<APMResponseParser.APMResponseData> responses = new ArrayList<>();

      BiMap<String, Object> headersBiMap = resolveDollarReferences(headers);
      BiMap<String, Object> optionsBiMap = resolveDollarReferences(options);

      String hostKey = fetchHostKey(optionsBiMap);
      List<Callable<APMResponseParser.APMResponseData>> callabels = new ArrayList<>();
      if (!isEmpty(hostKey) || initialUrl.contains("${host}")) {
        if (initialUrl.contains(BATCH_TEXT)) {
          List<String> urlList = resolveBatchHosts(initialUrl);
          for (String url : urlList) {
            // host has already been resolved. So it's ok to pass null here.
            List<String> curUrls = resolveDollarReferences(url, null, strategy);
            curUrls.forEach(curUrl
                -> callabels.add(()
                                     -> new APMResponseParser.APMResponseData(null,
                                         collect(getAPMRestClient(baseUrl).collect(curUrl, headersBiMap, optionsBiMap),
                                             baseUrl + curUrl),
                                         metricInfos)));
          }
        } else {
          // This is not a batch query
          for (String host : dataCollectionInfo.getHosts()) {
            List<String> curUrls = resolveDollarReferences(initialUrl, host, strategy);
            curUrls.forEach(curUrl
                -> callabels.add(()
                                     -> new APMResponseParser.APMResponseData(host,
                                         collect(getAPMRestClient(baseUrl).collect(curUrl, headersBiMap, optionsBiMap),
                                             baseUrl + curUrl),
                                         metricInfos)));
          }
        }

      } else {
        List<String> curUrls = resolveDollarReferences(initialUrl, TEST_HOST_NAME, strategy);
        int i = 0;
        IntStream.range(0, curUrls.size())
            .forEach(index
                -> callabels.add(
                    ()
                        -> new APMResponseParser.APMResponseData(getHostNameForTestControl(index),
                            collect(getAPMRestClient(baseUrl).collect(curUrls.get(index), headersBiMap, optionsBiMap),
                                baseUrl + curUrls.get(index)),
                            metricInfos)));
      }

      executeParrallel(callabels)
          .stream()
          .filter(Optional::isPresent)
          .forEach(response -> responses.add(response.get()));
      return responses;
    }

    /**
     *
     * @param batchUrl urlData{$harness_batch{pod_name:${host},'|'}}
     * @return urlData{pod_name:host1.pod1.com | pod_name:host2.pod3.com }
     */
    protected List<String> resolveBatchHosts(final String batchUrl) {
      List<String> hostList = new ArrayList<>(dataCollectionInfo.getHosts());
      List<String> batchResolvedUrls = new ArrayList<>();
      Pattern batchPattern = Pattern.compile(BATCH_REGEX);
      Matcher matcher = batchPattern.matcher(batchUrl);
      while (matcher.find()) {
        final String fullBatchToken = matcher.group();
        final String hostString = matcher.group(1);
        final String separator = matcher.group(2).replaceAll("'", "");
        String batchedHosts = "";
        for (int i = 0; i < hostList.size(); i++) {
          batchedHosts += hostString.replace("${host}", hostList.get(i));
          if (((i + 1) % MAX_HOSTS_PER_BATCH) == 0 || i == hostList.size() - 1) {
            String curUrl = batchUrl.replace(fullBatchToken, batchedHosts);
            batchResolvedUrls.add(curUrl);
            batchedHosts = "";
            continue;
          }
          batchedHosts += separator;
        }
      }
      return batchResolvedUrls;
    }

    /**
     * if it is time to collect data, return true. Else false.
     * @return
     */
    private boolean shouldRunCollection() {
      currentElapsedTime =
          (int) ((Timestamp.currentMinuteBoundary() - collectionStartMinute) / TimeUnit.MINUTES.toMillis(1));
      boolean shouldCollectData = false;
      if (dataCollectionMinute == 0 || currentElapsedTime % collectionWindow == 0
          || currentElapsedTime >= dataCollectionInfo.getDataCollectionTotalTime() - 1) {
        shouldCollectData = true;
      }
      logger.info("ShouldCollectDataCollection is {} for minute {}", shouldCollectData, currentElapsedTime);
      return shouldCollectData;
    }
    @SuppressFBWarnings
    @Override
    public void run() {
      try {
        int retry = 0;
        boolean shouldRunDataCollection = shouldRunCollection();
        while (shouldRunDataCollection && !completed.get() && retry < RETRIES) {
          try {
            TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = TreeBasedTable.create();

            List<APMResponseParser.APMResponseData> apmResponseDataList = new ArrayList<>();
            for (Map.Entry<String, List<APMMetricInfo>> metricInfoEntry :
                dataCollectionInfo.getMetricEndpoints().entrySet()) {
              apmResponseDataList.addAll(collect(dataCollectionInfo.getBaseUrl(), dataCollectionInfo.getHeaders(),
                  dataCollectionInfo.getOptions(), metricInfoEntry.getKey(), metricInfoEntry.getValue(),
                  dataCollectionInfo.getStrategy()));
            }
            Set<String> groupNameSet = new HashSet<>();
            Collection<NewRelicMetricDataRecord> newRelicMetricDataRecords =
                new APMResponseParser().extract(apmResponseDataList);

            newRelicMetricDataRecords.forEach(newRelicMetricDataRecord -> {
              newRelicMetricDataRecord.setServiceId(dataCollectionInfo.getServiceId());
              newRelicMetricDataRecord.setStateExecutionId(dataCollectionInfo.getStateExecutionId());
              newRelicMetricDataRecord.setWorkflowExecutionId(dataCollectionInfo.getWorkflowExecutionId());
              newRelicMetricDataRecord.setWorkflowId(dataCollectionInfo.getWorkflowId());
              long startTimeMinForHost = collectionStartMinute;
              if (hostStartMinuteMap.containsKey(newRelicMetricDataRecord.getHost())) {
                startTimeMinForHost = hostStartMinuteMap.get(newRelicMetricDataRecord.getHost());
              }
              newRelicMetricDataRecord.setDataCollectionMinute(
                  (int) ((Timestamp.minuteBoundary(newRelicMetricDataRecord.getTimeStamp()) - startTimeMinForHost)
                      / TimeUnit.MINUTES.toMillis(1)));

              newRelicMetricDataRecord.setStateType(dataCollectionInfo.getStateType());
              groupNameSet.add(newRelicMetricDataRecord.getGroupName());

              newRelicMetricDataRecord.setAppId(dataCollectionInfo.getApplicationId());
              records.put(newRelicMetricDataRecord.getName() + newRelicMetricDataRecord.getHost(),
                  newRelicMetricDataRecord.getTimeStamp(), newRelicMetricDataRecord);
            });

            dataCollectionMinute = currentElapsedTime - 1;
            addHeartbeatRecords(groupNameSet, records);
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

            lastEndTime = currentEndTime;
            collectionStartTime += TimeUnit.MINUTES.toMillis(collectionWindow);
            break;

          } catch (Exception ex) {
            if (++retry >= RETRIES) {
              taskResult.setStatus(DataCollectionTaskResult.DataCollectionTaskStatus.FAILURE);
              completed.set(true);
              break;
            } else {
              if (retry == 1) {
                taskResult.setErrorMessage(Misc.getMessage(ex));
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

    private void addHeartbeatRecords(
        Set<String> groupNameSet, TreeBasedTable<String, Long, NewRelicMetricDataRecord> records) {
      if (isEmpty(groupNameSet)) {
        groupNameSet = new HashSet<>(Arrays.asList(NewRelicMetricDataRecord.DEFAULT_GROUP_NAME));
      }
      for (String group : groupNameSet) {
        // HeartBeat
        int heartbeatCounter = 0;
        records.put(HARNESS_HEARTBEAT_METRIC_NAME + group, (long) heartbeatCounter++,
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
                .groupName(group)
                .build());
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
