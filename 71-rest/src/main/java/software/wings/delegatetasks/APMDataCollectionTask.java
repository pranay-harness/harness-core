package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.VerificationConstants.DATA_COLLECTION_RETRY_SLEEP;
import static software.wings.common.VerificationConstants.VERIFICATION_HOST_PLACEHOLDER;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.sm.states.DynatraceState.CONTROL_HOST_NAME;
import static software.wings.sm.states.DynatraceState.TEST_HOST_NAME;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
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
import software.wings.beans.TaskType;
import software.wings.delegatetasks.cv.RequestExecutor;
import software.wings.helpers.ext.apm.APMRestClient;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.impl.apm.APMDataCollectionInfo;
import software.wings.service.impl.apm.APMMetricInfo;
import software.wings.service.impl.apm.APMResponseParser;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.net.URLEncoder;
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

@Slf4j
public class APMDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final int CANARY_DAYS_TO_COLLECT = 7;

  private static final String BATCH_REGEX = "\\$harness_batch\\{([^,]*),([^}]*)\\}";
  private static final String DATADOG_API_MASK = "api_key=([^&]*)&application_key=([^&]*)&";
  private static final String BATCH_TEXT = "$harness_batch";
  private static final int MAX_HOSTS_PER_BATCH = 15;
  private static Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");
  private static final String URL_BODY_APPENDER = "__harness-body__";
  private static final int FIVE_MINS_IN_SECONDS = 5 * 60;
  private static final int TWO_MINS_IN_SECONDS = 2 * 60;

  @Inject private NewRelicDelegateService newRelicDelegateService;
  @Inject private DelegateLogService delegateLogService;
  @Inject private RequestExecutor requestExecutor;

  private int collectionWindow = 1;

  private APMDataCollectionInfo dataCollectionInfo;
  private Map<String, String> decryptedFields = new HashMap<>();

  public APMDataCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected boolean is24X7Task() {
    return getTaskType().equalsIgnoreCase(TaskType.APM_24_7_METRIC_DATA_COLLECTION_TASK.name());
  }

  @Override
  protected int getInitialDelayMinutes() {
    if (is24X7Task()) {
      return 0;
    }
    int delayMinsFromDataCollectionTask = (int) TimeUnit.SECONDS.toMinutes(dataCollectionInfo.getInitialDelaySeconds());
    if (delayMinsFromDataCollectionTask > 5 || delayMinsFromDataCollectionTask < 0) {
      return 2;
    }
    return delayMinsFromDataCollectionTask;
  }

  @Override
  protected int getInitialDelaySeconds() {
    if (is24X7Task()) {
      return 0;
    }
    if (dataCollectionInfo.getInitialDelaySeconds() > FIVE_MINS_IN_SECONDS
        || dataCollectionInfo.getInitialDelaySeconds() < 0) {
      return TWO_MINS_IN_SECONDS;
    }
    return dataCollectionInfo.getInitialDelaySeconds();
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(TaskParameters parameters) {
    dataCollectionInfo = (APMDataCollectionInfo) parameters;
    collectionWindow =
        dataCollectionInfo.getDataCollectionFrequency() != 0 ? dataCollectionInfo.getDataCollectionFrequency() : 1;
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
    return new APMMetricCollector(dataCollectionInfo, taskResult,
        this.getTaskType().equalsIgnoreCase(TaskType.APM_24_7_METRIC_DATA_COLLECTION_TASK.name()));
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
    boolean is24x7Task;

    private APMMetricCollector(
        APMDataCollectionInfo dataCollectionInfo, DataCollectionTaskResult taskResult, boolean is24x7Task) {
      this.dataCollectionInfo = dataCollectionInfo;
      this.taskResult = taskResult;
      this.collectionStartMinute = Timestamp.currentMinuteBoundary();
      this.collectionStartTime = Timestamp.minuteBoundary(dataCollectionInfo.getStartTime());
      this.dataCollectionMinute = dataCollectionInfo.getDataCollectionMinute();
      this.lastEndTime = dataCollectionInfo.getStartTime();
      this.currentElapsedTime = 0;
      this.is24x7Task = is24x7Task;
      hostStartMinuteMap = new HashMap<>();
    }

    private APMRestClient getAPMRestClient(final String baseUrl) {
      final Retrofit retrofit = new Retrofit.Builder()
                                    .baseUrl(baseUrl)
                                    .addConverterFactory(JacksonConverterFactory.create())
                                    .client(getUnsafeHttpClient(baseUrl))
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

      if (result.contains(VERIFICATION_HOST_PLACEHOLDER)) {
        result = result.replace(VERIFICATION_HOST_PLACEHOLDER, host);
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

      if (!dataCollectionInfo.isCanaryUrlPresent() && TEST_HOST_NAME.equals(host)
          && strategy == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
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
        if (isPredictiveAnalysis() && dataCollectionMinute == 0 && !is24x7Task) {
          startTime = startTime - TimeUnit.MINUTES.toMillis(PREDECTIVE_HISTORY_MINUTES);
        } else if (is24x7Task) {
          endTime = startTime + TimeUnit.MINUTES.toMillis(dataCollectionInfo.getDataCollectionTotalTime());
        }
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

    private Map<String, String> getStringsToMask() {
      Map<String, String> maskFields = new HashMap<>();
      if (isNotEmpty(decryptedFields)) {
        decryptedFields.forEach((k, v) -> { maskFields.put(v, "<" + k + ">"); });
      }
      return maskFields;
    }

    private String collect(Call<Object> request) {
      try {
        ThirdPartyApiCallLog apiCallLog = createApiCallLog(dataCollectionInfo.getStateExecutionId());
        apiCallLog.setTitle("Fetch request to: " + dataCollectionInfo.getBaseUrl());
        Object response = requestExecutor.executeRequest(apiCallLog, request, getStringsToMask());
        return JsonUtils.asJson(response);
      } catch (Exception e) {
        throw new WingsException("Error while fetching data. " + ExceptionUtils.getMessage(e), e);
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
        Map<String, String> options, String initialUrl, List<APMMetricInfo> metricInfos,
        AnalysisComparisonStrategy strategy) throws IOException {
      // OkHttp seems to have issues encoding backtick, so explictly encoding it.
      String[] urlAndBody = initialUrl.split(URL_BODY_APPENDER);
      initialUrl = urlAndBody[0];
      final String body = urlAndBody.length > 1 ? urlAndBody[1] : "";
      if (initialUrl.contains("`")) {
        try {
          initialUrl = initialUrl.replaceAll("`", URLEncoder.encode("`", "UTF-8"));
        } catch (Exception e) {
          logger.warn("Unsupported exception caught when encoding a back-tick", e);
        }
      }
      List<APMResponseParser.APMResponseData> responses = new ArrayList<>();

      BiMap<String, Object> headersBiMap = resolveDollarReferences(headers);
      BiMap<String, Object> optionsBiMap = resolveDollarReferences(options);

      String hostKey = fetchHostKey(optionsBiMap);
      List<Callable<APMResponseParser.APMResponseData>> callabels = new ArrayList<>();
      if (dataCollectionInfo.isCanaryUrlPresent()) {
        dataCollectionInfo.getCanaryMetricInfos().forEach(canaryMetricInfo -> {
          String url =
              resolveDollarReferences(canaryMetricInfo.getUrl(), canaryMetricInfo.getHostName(), strategy).get(0);

          List<String> resolvedBodies = resolveDollarReferences(body, TEST_HOST_NAME, strategy);
          if (isEmpty(body)) {
            callabels.add(
                ()
                    -> new APMResponseParser.APMResponseData(canaryMetricInfo.getHostName(), DEFAULT_GROUP_NAME,
                        collect(getAPMRestClient(baseUrl).collect(url, headersBiMap, optionsBiMap)), metricInfos));

          } else {
            resolvedBodies.forEach(resolvedBody -> {
              callabels.add(
                  ()
                      -> new APMResponseParser.APMResponseData(canaryMetricInfo.getHostName(), DEFAULT_GROUP_NAME,
                          collect(getAPMRestClient(baseUrl).postCollect(
                              url, headersBiMap, optionsBiMap, new JSONObject(resolvedBody).toMap())),
                          metricInfos));
            });
          }
        });
      } else if (!isEmpty(hostKey) || initialUrl.contains(VERIFICATION_HOST_PLACEHOLDER)
          || (isNotEmpty(body) && body.contains(VERIFICATION_HOST_PLACEHOLDER))) {
        if (initialUrl.contains(BATCH_TEXT)) {
          List<String> urlList = resolveBatchHosts(initialUrl);
          for (String url : urlList) {
            // host has already been resolved. So it's ok to pass null here.
            List<String> curUrls = resolveDollarReferences(url, null, strategy);
            curUrls.forEach(curUrl
                -> callabels.add(()
                                     -> new APMResponseParser.APMResponseData(null, DEFAULT_GROUP_NAME,
                                         collect(getAPMRestClient(baseUrl).collect(curUrl, headersBiMap, optionsBiMap)),
                                         metricInfos)));
          }
        } else {
          // This is not a batch query
          for (String host : dataCollectionInfo.getHosts().keySet()) {
            List<String> curUrls = resolveDollarReferences(initialUrl, host, strategy);
            if (!isEmpty(body)) {
              String resolvedBody = resolvedUrl(body, host, lastEndTime, System.currentTimeMillis());
              Map<String, Object> bodyMap =
                  isEmpty(resolvedBody) ? new HashMap<>() : new JSONObject(resolvedBody).toMap();

              curUrls.forEach(curUrl
                  -> callabels.add(
                      ()
                          -> new APMResponseParser.APMResponseData(host, dataCollectionInfo.getHosts().get(host),
                              collect(
                                  getAPMRestClient(baseUrl).postCollect(curUrl, headersBiMap, optionsBiMap, bodyMap)),
                              metricInfos)));
            } else {
              curUrls.forEach(curUrl
                  -> callabels.add(
                      ()
                          -> new APMResponseParser.APMResponseData(host, dataCollectionInfo.getHosts().get(host),
                              collect(getAPMRestClient(baseUrl).collect(curUrl, headersBiMap, optionsBiMap)),
                              metricInfos)));
            }
          }
        }

      } else {
        List<String> curUrls = resolveDollarReferences(initialUrl, TEST_HOST_NAME, strategy);
        List<String> resolvedBodies = resolveDollarReferences(body, TEST_HOST_NAME, strategy);
        if (isEmpty(body)) {
          IntStream.range(0, curUrls.size())
              .forEach(index
                  -> callabels.add(
                      ()
                          -> new APMResponseParser.APMResponseData(getHostNameForTestControl(index), DEFAULT_GROUP_NAME,
                              collect(
                                  getAPMRestClient(baseUrl).collect(curUrls.get(index), headersBiMap, optionsBiMap)),
                              metricInfos)));
        } else {
          IntStream.range(0, curUrls.size()).forEach(index -> {
            resolvedBodies.forEach(resolvedBody
                -> callabels.add(
                    ()
                        -> new APMResponseParser.APMResponseData(getHostNameForTestControl(index), DEFAULT_GROUP_NAME,
                            collect(getAPMRestClient(baseUrl).postCollect(
                                curUrls.get(index), headersBiMap, optionsBiMap, new JSONObject(resolvedBody).toMap())),
                            metricInfos)));
          });
        }
      }

      executeParallel(callabels)
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
      List<String> hostList = new ArrayList<>(dataCollectionInfo.getHosts().keySet());
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

    private boolean isPredictiveAnalysis() {
      return dataCollectionInfo.getStrategy() == AnalysisComparisonStrategy.PREDICTIVE;
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

    @Override
    @SuppressWarnings("PMD")
    public void run() {
      int retry = 0;
      boolean shouldRunDataCollection = shouldRunCollection();
      while (shouldRunDataCollection && !completed.get() && retry < RETRIES) {
        try {
          TreeBasedTable<String, Long, NewRelicMetricDataRecord> records = TreeBasedTable.create();

          List<APMResponseParser.APMResponseData> apmResponseDataList = new ArrayList<>();
          if (isNotEmpty(dataCollectionInfo.getCanaryMetricInfos())) {
            apmResponseDataList.addAll(collect(dataCollectionInfo.getBaseUrl(), dataCollectionInfo.getHeaders(),
                dataCollectionInfo.getOptions(), dataCollectionInfo.getBaseUrl(),
                dataCollectionInfo.getCanaryMetricInfos(), dataCollectionInfo.getStrategy()));
          }

          for (Map.Entry<String, List<APMMetricInfo>> metricInfoEntry :
              dataCollectionInfo.getMetricEndpoints().entrySet()) {
            apmResponseDataList.addAll(collect(dataCollectionInfo.getBaseUrl(), dataCollectionInfo.getHeaders(),
                dataCollectionInfo.getOptions(), metricInfoEntry.getKey(), metricInfoEntry.getValue(),
                dataCollectionInfo.getStrategy()));
          }
          Set<String> groupNameSet = dataCollectionInfo.getHosts() != null
              ? new HashSet<>(dataCollectionInfo.getHosts().values())
              : new HashSet<>();
          Collection<NewRelicMetricDataRecord> newRelicMetricDataRecords =
              APMResponseParser.extract(apmResponseDataList);

          newRelicMetricDataRecords.forEach(newRelicMetricDataRecord -> {
            if (newRelicMetricDataRecord.getTimeStamp() == 0) {
              newRelicMetricDataRecord.setTimeStamp(currentEndTime);
            }
            newRelicMetricDataRecord.setServiceId(dataCollectionInfo.getServiceId());
            newRelicMetricDataRecord.setStateExecutionId(dataCollectionInfo.getStateExecutionId());
            newRelicMetricDataRecord.setWorkflowExecutionId(dataCollectionInfo.getWorkflowExecutionId());
            newRelicMetricDataRecord.setWorkflowId(dataCollectionInfo.getWorkflowId());
            newRelicMetricDataRecord.setCvConfigId(dataCollectionInfo.getCvConfigId());
            long startTimeMinForHost = collectionStartMinute;
            if (hostStartMinuteMap.containsKey(newRelicMetricDataRecord.getHost())) {
              startTimeMinForHost = hostStartMinuteMap.get(newRelicMetricDataRecord.getHost());
            }

            int collectionMin = resolveDataCollectionMinute(
                newRelicMetricDataRecord.getTimeStamp(), newRelicMetricDataRecord.getHost(), false);
            newRelicMetricDataRecord.setDataCollectionMinute(collectionMin);

            if (isPredictiveAnalysis()) {
              newRelicMetricDataRecord.setHost(newRelicMetricDataRecord.getGroupName());
            }

            newRelicMetricDataRecord.setStateType(dataCollectionInfo.getStateType());
            groupNameSet.add(newRelicMetricDataRecord.getGroupName());

            newRelicMetricDataRecord.setAppId(dataCollectionInfo.getApplicationId());
            if (newRelicMetricDataRecord.getTimeStamp() >= startTimeMinForHost || is24x7Task) {
              records.put(newRelicMetricDataRecord.getName() + newRelicMetricDataRecord.getHost(),
                  newRelicMetricDataRecord.getTimeStamp(), newRelicMetricDataRecord);
            } else {
              logger.info("The data record {} is older than startTime. Ignoring", newRelicMetricDataRecord);
            }
          });

          dataCollectionMinute = currentElapsedTime - 1;
          addHeartbeatRecords(groupNameSet, records);
          List<NewRelicMetricDataRecord> allMetricRecords = getAllMetricRecords(records);

          if (!saveMetrics(dataCollectionInfo.getAccountId(), dataCollectionInfo.getApplicationId(),
                  dataCollectionInfo.getStateExecutionId(), allMetricRecords)) {
            logger.error("Error saving metrics to the database. DatacollectionMin: {} StateexecutionId: {}",
                dataCollectionMinute, dataCollectionInfo.getStateExecutionId());
          } else {
            logger.info(dataCollectionInfo.getStateType() + ": Sent {} metric records to the server for minute {}",
                allMetricRecords.size(), dataCollectionMinute);
          }
          lastEndTime = currentEndTime;
          collectionStartTime += TimeUnit.MINUTES.toMillis(collectionWindow);
          if (dataCollectionMinute >= dataCollectionInfo.getDataCollectionTotalTime() || is24x7Task) {
            // We are done with all data collection, so setting task status to success and quitting.
            logger.info(
                "Completed APM collection task. So setting task status to success and quitting. StateExecutionId {}",
                dataCollectionInfo.getStateExecutionId());
            completed.set(true);
            taskResult.setStatus(DataCollectionTaskStatus.SUCCESS);
          }
          break;

        } catch (Throwable ex) {
          if (!(ex instanceof Exception) || ++retry >= RETRIES) {
            logger.error("error fetching metrics for {} for minute {}", dataCollectionInfo.getStateExecutionId(),
                dataCollectionMinute, ex);
            taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
            completed.set(true);
            break;
          } else {
            if (retry == 1) {
              taskResult.setErrorMessage(ExceptionUtils.getMessage(ex));
            }
            logger.warn("error fetching apm metrics for minute " + dataCollectionMinute + ". retrying in "
                    + DATA_COLLECTION_RETRY_SLEEP + "s",
                ex);
            sleep(DATA_COLLECTION_RETRY_SLEEP);
          }
        }
      }

      if (completed.get()) {
        logger.info(dataCollectionInfo.getStateType() + ": Shutting down apm data collection");
        shutDownCollection();
        return;
      }
    }

    private int resolveDataCollectionMinute(long timestamp, String host, boolean isHeartbeat) {
      int collectionMinute = -1;
      if (isHeartbeat) {
        collectionMinute = dataCollectionMinute;
        if (is24x7Task) {
          collectionMinute = (int) TimeUnit.MILLISECONDS.toMinutes(dataCollectionInfo.getStartTime())
              + dataCollectionInfo.getDataCollectionTotalTime();
        }
      } else if (is24x7Task) {
        collectionMinute = (int) TimeUnit.MILLISECONDS.toMinutes(timestamp);
      } else {
        long startTimeMinForHost = isPredictiveAnalysis()
            ? collectionStartMinute - TimeUnit.MINUTES.toMillis(PREDECTIVE_HISTORY_MINUTES)
            : collectionStartMinute;
        if (hostStartMinuteMap.containsKey(host)) {
          startTimeMinForHost = hostStartMinuteMap.get(host);
        }
        collectionMinute =
            (int) ((Timestamp.minuteBoundary(timestamp) - startTimeMinForHost) / TimeUnit.MINUTES.toMillis(1));
      }

      return collectionMinute;
    }

    private void addHeartbeatRecords(
        Set<String> groupNameSet, TreeBasedTable<String, Long, NewRelicMetricDataRecord> records) {
      if (isEmpty(groupNameSet)) {
        groupNameSet = new HashSet<>(Arrays.asList(DEFAULT_GROUP_NAME));
      }
      for (String group : groupNameSet) {
        if (group == null) {
          final String errorMsg =
              "Unexpected null groupName received while sending APM Heartbeat. Please contact Harness Support.";
          logger.error(errorMsg);
          throw new WingsException(errorMsg);
        }
        // Heartbeat
        int heartbeatCounter = 0;
        records.put(HARNESS_HEARTBEAT_METRIC_NAME + group, (long) heartbeatCounter++,
            NewRelicMetricDataRecord.builder()
                .stateType(getStateType())
                .name(HARNESS_HEARTBEAT_METRIC_NAME)
                .workflowId(dataCollectionInfo.getWorkflowId())
                .workflowExecutionId(dataCollectionInfo.getWorkflowExecutionId())
                .serviceId(dataCollectionInfo.getServiceId())
                .cvConfigId(dataCollectionInfo.getCvConfigId())
                .stateExecutionId(dataCollectionInfo.getStateExecutionId())
                .appId(dataCollectionInfo.getApplicationId())
                .dataCollectionMinute(resolveDataCollectionMinute(Timestamp.currentMinuteBoundary(), null, true))
                .timeStamp(collectionStartTime)
                .level(ClusterLevel.H0)
                .groupName(group)
                .build());
      }
    }
  }
}
