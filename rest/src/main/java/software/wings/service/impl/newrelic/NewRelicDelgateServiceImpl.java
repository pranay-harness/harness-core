package software.wings.service.impl.newrelic;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.common.Constants.PAYLOAD;
import static software.wings.common.Constants.URL_STRING;
import static software.wings.service.impl.ThirdPartyApiCallLog.apiCallLogWithDummyStateExecution;
import static software.wings.service.impl.security.SecretManagementDelegateServiceImpl.NUM_OF_RETRIES;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.network.Http;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.NewRelicDeploymentMarkerPayload;
import software.wings.delegatetasks.DataCollectionExecutorService;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.newrelic.NewRelicRestClient;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by rsingh on 8/28/17.
 */
@SuppressFBWarnings("STCAL_STATIC_SIMPLE_DATE_FORMAT_INSTANCE")
public class NewRelicDelgateServiceImpl implements NewRelicDelegateService {
  private static final int METRIC_DATA_QUERY_BATCH_SIZE = 50;
  private static final int MIN_RPM = 1;
  private static final String NEW_RELIC_DATE_FORMAT = "YYYY-MM-dd'T'HH:mm:ssZ";
  public static final SimpleDateFormat dateFormatter = new SimpleDateFormat(NEW_RELIC_DATE_FORMAT);

  private static final Logger logger = LoggerFactory.getLogger(NewRelicDelgateServiceImpl.class);
  @Inject private EncryptionService encryptionService;
  @Inject private DataCollectionExecutorService dataCollectionService;
  @Inject private DelegateLogService delegateLogService;

  @Override
  public boolean validateConfig(NewRelicConfig newRelicConfig) throws IOException, CloneNotSupportedException {
    getAllApplications(newRelicConfig, Collections.emptyList(), null);
    return true;
  }

  @Override
  public List<NewRelicApplication> getAllApplications(
      NewRelicConfig newRelicConfig, List<EncryptedDataDetail> encryptedDataDetails, ThirdPartyApiCallLog apiCallLog)
      throws IOException, CloneNotSupportedException {
    List<NewRelicApplication> rv = new ArrayList<>();
    if (apiCallLog == null) {
      apiCallLog = apiCallLogWithDummyStateExecution(newRelicConfig.getAccountId());
    }
    int pageCount = 1;
    while (true) {
      apiCallLog = apiCallLog.copy();
      apiCallLog.setTitle("Fetching applications from " + newRelicConfig.getNewRelicUrl());
      apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toEpochSecond());
      apiCallLog.addFieldToRequest(
          ThirdPartyApiCallField.builder()
              .name(URL_STRING)
              .value(newRelicConfig.getNewRelicUrl() + "/v2/applications.json?page=" + pageCount)
              .type(FieldType.URL)
              .build());
      final Call<NewRelicApplicationsResponse> request =
          getNewRelicRestClient(newRelicConfig, encryptedDataDetails).listAllApplications(pageCount);
      final Response<NewRelicApplicationsResponse> response = request.execute();
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toEpochSecond());
      if (response.isSuccessful()) {
        apiCallLog.addFieldToResponse(response.code(), response.body(), FieldType.JSON);
        List<NewRelicApplication> applications = response.body().getApplications();
        delegateLogService.save(newRelicConfig.getAccountId(), apiCallLog);
        if (isEmpty(applications)) {
          break;
        } else {
          rv.addAll(applications);
        }
      } else {
        apiCallLog.addFieldToResponse(response.code(), response.errorBody().string(), FieldType.TEXT);
        delegateLogService.save(newRelicConfig.getAccountId(), apiCallLog);
        throw new WingsException(response.errorBody().string());
      }

      pageCount++;
    }

    return rv;
  }

  @Override
  public List<NewRelicApplicationInstance> getApplicationInstances(NewRelicConfig newRelicConfig,
      List<EncryptedDataDetail> encryptedDataDetails, long newRelicApplicationId, ThirdPartyApiCallLog apiCallLog)
      throws IOException, CloneNotSupportedException {
    List<NewRelicApplicationInstance> rv = new ArrayList<>();
    if (apiCallLog == null) {
      apiCallLog = apiCallLogWithDummyStateExecution(newRelicConfig.getAccountId());
    }
    int pageCount = 1;
    while (true) {
      apiCallLog = apiCallLog.copy();
      apiCallLog.setTitle("Fetching application instances from " + newRelicConfig.getNewRelicUrl());
      apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toEpochSecond());
      apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                       .name(URL_STRING)
                                       .value(newRelicConfig.getNewRelicUrl() + "/v2/applications/"
                                           + newRelicApplicationId + "/instances.json?page=" + pageCount)
                                       .type(FieldType.URL)
                                       .build());
      final Call<NewRelicApplicationInstancesResponse> request =
          getNewRelicRestClient(newRelicConfig, encryptedDataDetails)
              .listAppInstances(newRelicApplicationId, pageCount);
      final Response<NewRelicApplicationInstancesResponse> response = request.execute();
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toEpochSecond());
      if (response.isSuccessful()) {
        apiCallLog.addFieldToResponse(response.code(), response.body(), FieldType.JSON);
        List<NewRelicApplicationInstance> applicationInstances = response.body().getApplication_instances();
        delegateLogService.save(newRelicConfig.getAccountId(), apiCallLog);
        if (isEmpty(applicationInstances)) {
          break;
        } else {
          rv.addAll(applicationInstances);
        }
      } else {
        apiCallLog.addFieldToResponse(response.code(), response.errorBody().string(), FieldType.TEXT);
        delegateLogService.save(newRelicConfig.getAccountId(), apiCallLog);
        throw new WingsException(response.errorBody().string());
      }

      pageCount++;
    }

    return rv;
  }

  @Override
  public List<NewRelicMetric> getTxnsWithData(NewRelicConfig newRelicConfig,
      List<EncryptedDataDetail> encryptionDetails, long applicationId, ThirdPartyApiCallLog apiCallLog)
      throws IOException {
    Set<NewRelicMetric> txnNameToCollect =
        getTxnNameToCollect(newRelicConfig, encryptionDetails, applicationId, apiCallLog);
    Set<NewRelicMetric> txnsWithDataInLastHour =
        getTxnsWithDataInLastHour(txnNameToCollect, newRelicConfig, encryptionDetails, applicationId, apiCallLog);
    return Lists.newArrayList(txnsWithDataInLastHour);
  }

  @Override
  public Set<NewRelicMetric> getTxnNameToCollect(NewRelicConfig newRelicConfig,
      List<EncryptedDataDetail> encryptedDataDetails, long newRelicAppId, ThirdPartyApiCallLog apiCallLog)
      throws IOException {
    Set<NewRelicMetric> newRelicMetrics = new HashSet<>();
    if (apiCallLog == null) {
      apiCallLog = apiCallLogWithDummyStateExecution(newRelicConfig.getAccountId());
    }
    for (int retry = 0; retry <= NUM_OF_RETRIES; retry++) {
      try {
        apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                         .name(URL_STRING)
                                         .value(newRelicConfig.getNewRelicUrl() + "/v2/applications/" + newRelicAppId
                                             + "/metrics.json?name=WebTransaction")
                                         .type(FieldType.URL)
                                         .build());
        apiCallLog.setTitle("Fetching web transactions names from " + newRelicConfig.getNewRelicUrl());
        apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toEpochSecond());
        final Call<NewRelicMetricResponse> request =
            getNewRelicRestClient(newRelicConfig, encryptedDataDetails).listMetricNames(newRelicAppId);
        final Response<NewRelicMetricResponse> response = request.execute();
        apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toEpochSecond());
        if (response.isSuccessful()) {
          apiCallLog.addFieldToResponse(response.code(), response.body(), FieldType.JSON);
          List<NewRelicMetric> metrics = response.body().getMetrics();
          if (isNotEmpty(metrics)) {
            metrics.forEach(metric -> {
              if (metric.getName().startsWith("WebTransaction/") || metric.getName().equals("WebTransaction")) {
                newRelicMetrics.add(metric);
              }
            });
          }
        } else if (response.code() != HttpServletResponse.SC_NOT_FOUND) {
          apiCallLog.addFieldToResponse(response.code(), response.errorBody().string(), FieldType.TEXT);
          throw new WingsException(response.errorBody().string());
        }
        return newRelicMetrics;
      } catch (Exception e) {
        if (retry < NUM_OF_RETRIES) {
          logger.warn("txn name fetch failed. trial num: {}", retry, e);
          sleep(ofMillis(1000));
        } else {
          logger.error("txn name fetch failed after {} retries ", retry, e);
          throw new IOException("txn name fetch failed after " + NUM_OF_RETRIES + " retries", e);
        }
      } finally {
        delegateLogService.save(newRelicConfig.getAccountId(), apiCallLog);
      }
    }

    throw new IllegalStateException("This state should have never reached ");
  }

  public Set<NewRelicMetric> getTxnsWithDataInLastHour(Collection<NewRelicMetric> metrics,
      NewRelicConfig newRelicConfig, List<EncryptedDataDetail> encryptedDataDetails, long applicationId,
      ThirdPartyApiCallLog apiCallLog) throws IOException {
    if (apiCallLog == null) {
      apiCallLog = ThirdPartyApiCallLog.apiCallLogWithDummyStateExecution(newRelicConfig.getAccountId());
    }
    Map<String, NewRelicMetric> webTransactionMetrics = new HashMap<>();
    for (NewRelicMetric metric : metrics) {
      webTransactionMetrics.put(metric.getName(), metric);
    }
    List<Set<String>> metricBatches = batchMetricsToCollect(metrics);
    List<Callable<Set<String>>> metricDataCallabels = new ArrayList<>();
    for (Collection<String> metricNames : metricBatches) {
      final ThirdPartyApiCallLog apiCallLogCopy = apiCallLog.copy();
      metricDataCallabels.add(
          () -> getMetricsWithNoData(metricNames, newRelicConfig, encryptedDataDetails, applicationId, apiCallLogCopy));
    }
    List<Optional<Set<String>>> results = dataCollectionService.executeParrallel(metricDataCallabels);
    results.forEach(result -> {
      if (result.isPresent()) {
        for (String metricName : result.get()) {
          webTransactionMetrics.remove(metricName);
        }
      }
    });
    return new HashSet<>(webTransactionMetrics.values());
  }

  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  private Set<String> getMetricsWithNoData(Collection<String> metricNames, NewRelicConfig newRelicConfig,
      List<EncryptedDataDetail> encryptedDataDetails, long applicationId, ThirdPartyApiCallLog apiCallLog)
      throws IOException {
    final long currentTime = System.currentTimeMillis();
    Set<String> metricsWithNoData = Sets.newHashSet(metricNames);
    NewRelicMetricData metricData = getMetricDataApplication(newRelicConfig, encryptedDataDetails, applicationId,
        metricNames, currentTime - TimeUnit.HOURS.toMillis(1), currentTime, true, apiCallLog);

    if (metricData == null) {
      throw new WingsException("Unable to get NewRelic metric data for metric name collection " + newRelicConfig);
    }

    metricsWithNoData.removeAll(metricData.getMetrics_found());

    NewRelicMetricData errorMetricData = getMetricDataApplication(newRelicConfig, encryptedDataDetails, applicationId,
        getErrorMetricNames(metricNames), currentTime - TimeUnit.HOURS.toMillis(1), currentTime, true, apiCallLog);

    if (metricData == null) {
      throw new WingsException("Unable to get NewRelic metric data for metric name collection " + newRelicConfig);
    }
    metricsWithNoData.removeAll(metricData.getMetrics_found());

    for (NewRelicMetricData.NewRelicMetricSlice metric : metricData.getMetrics()) {
      for (NewRelicMetricData.NewRelicMetricTimeSlice timeSlice : metric.getTimeslices()) {
        final String webTxnJson = JsonUtils.asJson(timeSlice.getValues());
        NewRelicWebTransactions webTransactions = JsonUtils.asObject(webTxnJson, NewRelicWebTransactions.class);
        if (webTransactions.getRequests_per_minute() < MIN_RPM) {
          metricsWithNoData.add(metric.getName());
        }
      }
    }

    for (NewRelicMetricData.NewRelicMetricSlice metric : errorMetricData.getMetrics()) {
      for (NewRelicMetricData.NewRelicMetricTimeSlice timeSlice : metric.getTimeslices()) {
        final String webTxnJson = JsonUtils.asJson(timeSlice.getValues());
        NewRelicErrors webTransactions = JsonUtils.asObject(webTxnJson, NewRelicErrors.class);
        if (webTransactions.getError_count() > 0 || webTransactions.getErrors_per_minute() > 0) {
          metricsWithNoData.remove(metric.getName());
        }
      }
    }
    return metricsWithNoData;
  }

  @Override
  public NewRelicMetricData getMetricDataApplication(NewRelicConfig newRelicConfig,
      List<EncryptedDataDetail> encryptedDataDetails, long newRelicApplicationId, Collection<String> metricNames,
      long fromTime, long toTime, boolean summarize, ThirdPartyApiCallLog apiCallLog) throws IOException {
    String baseUrl = newRelicConfig.getNewRelicUrl().endsWith("/") ? newRelicConfig.getNewRelicUrl()
                                                                   : newRelicConfig.getNewRelicUrl() + "/";
    baseUrl += "v2/applications/" + newRelicApplicationId + "/metrics/data.json?summarize=" + summarize + "&";
    return getMetricData(newRelicConfig, encryptedDataDetails, baseUrl, metricNames, fromTime, toTime, apiCallLog);
  }

  @Override
  public NewRelicMetricData getMetricDataApplicationInstance(NewRelicConfig newRelicConfig,
      List<EncryptedDataDetail> encryptedDataDetails, long newRelicApplicationId, long instanceId,
      Collection<String> metricNames, long fromTime, long toTime, ThirdPartyApiCallLog apiCallLog) throws IOException {
    String baseUrl = newRelicConfig.getNewRelicUrl().endsWith("/") ? newRelicConfig.getNewRelicUrl()
                                                                   : newRelicConfig.getNewRelicUrl() + "/";
    baseUrl += "v2/applications/" + newRelicApplicationId + "/instances/" + instanceId + "/metrics/data.json?";
    return getMetricData(newRelicConfig, encryptedDataDetails, baseUrl, metricNames, fromTime, toTime, apiCallLog);
  }

  @SuppressFBWarnings({"STCAL_INVOKE_ON_STATIC_DATE_FORMAT_INSTANCE", "SBSC_USE_STRINGBUFFER_CONCATENATION"})
  private NewRelicMetricData getMetricData(NewRelicConfig newRelicConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String baseUrl, Collection<String> metricNames, long fromTime,
      long toTime, ThirdPartyApiCallLog apiCallLog) throws IOException {
    if (apiCallLog == null) {
      apiCallLog = apiCallLogWithDummyStateExecution(newRelicConfig.getAccountId());
    }
    String metricsToCollectString = "";
    for (String metricName : metricNames) {
      metricsToCollectString += "names[]=" + metricName + "&";
    }

    metricsToCollectString = StringUtils.removeEnd(metricsToCollectString, "&");

    final String url = baseUrl + metricsToCollectString;
    apiCallLog.setTitle(
        "Fetching metric data for " + metricNames.size() + " transactions from " + newRelicConfig.getNewRelicUrl());
    apiCallLog.addFieldToRequest(
        ThirdPartyApiCallField.builder().name(URL_STRING).value(url).type(FieldType.URL).build());
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toEpochSecond());
    final Call<NewRelicMetricDataResponse> request =
        getNewRelicRestClient(newRelicConfig, encryptedDataDetails)
            .getRawMetricData(url, dateFormatter.format(new Date(fromTime)), dateFormatter.format(new Date(toTime)));
    final Response<NewRelicMetricDataResponse> response = request.execute();
    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toEpochSecond());
    if (response.isSuccessful()) {
      apiCallLog.addFieldToResponse(response.code(), response.body(), FieldType.JSON);
      delegateLogService.save(newRelicConfig.getAccountId(), apiCallLog);
      return response.body().getMetric_data();
    }

    apiCallLog.addFieldToResponse(response.code(), response.errorBody().string(), FieldType.TEXT);
    delegateLogService.save(newRelicConfig.getAccountId(), apiCallLog);
    throw new WingsException(response.errorBody().string());
  }

  @Override
  public String postDeploymentMarker(NewRelicConfig config, List<EncryptedDataDetail> encryptedDataDetails,
      long newRelicApplicationId, NewRelicDeploymentMarkerPayload body, ThirdPartyApiCallLog apiCallLog)
      throws IOException {
    if (apiCallLog == null) {
      apiCallLog = apiCallLogWithDummyStateExecution(config.getAccountId());
    }
    final String baseUrl =
        config.getNewRelicUrl().endsWith("/") ? config.getNewRelicUrl() : config.getNewRelicUrl() + "/";
    final String url = baseUrl + "v2/applications/" + newRelicApplicationId + "/deployments.json";
    apiCallLog.setTitle("Posting deployment marker to " + config.getNewRelicUrl());
    apiCallLog.addFieldToRequest(
        ThirdPartyApiCallField.builder().name(URL_STRING).value(url).type(FieldType.URL).build());
    apiCallLog.addFieldToRequest(
        ThirdPartyApiCallField.builder().name(PAYLOAD).value(JsonUtils.asJson(body)).type(FieldType.JSON).build());
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toEpochSecond());
    final Call<Object> request = getNewRelicRestClient(config, encryptedDataDetails).postDeploymentMarker(url, body);
    final Response<Object> response = request.execute();
    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toEpochSecond());
    if (response.isSuccessful()) {
      apiCallLog.addFieldToResponse(response.code(), response.body(), FieldType.JSON);
      delegateLogService.save(config.getAccountId(), apiCallLog);
      return "Successfully posted deployment marker to NewRelic";
    }

    apiCallLog.addFieldToResponse(response.code(), response.errorBody(), FieldType.TEXT);
    delegateLogService.save(config.getAccountId(), apiCallLog);
    throw new WingsException(response.errorBody().string());
  }

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(NewRelicConfig newRelicConfig,
      List<EncryptedDataDetail> encryptedDataDetails, long newRelicApplicationId, long instanceId, long fromTime,
      long toTime, ThirdPartyApiCallLog apiCallLog) throws IOException {
    try {
      List<NewRelicMetric> txnsWithData =
          getTxnsWithData(newRelicConfig, encryptedDataDetails, newRelicApplicationId, apiCallLog);
      if (isEmpty(txnsWithData)) {
        return VerificationNodeDataSetupResponse.builder()
            .providerReachable(true)
            .loadResponse(VerificationLoadResponse.builder().isLoadPresent(false).build())
            .build();
      }
      Set<String> metricNames = txnsWithData.stream().map(NewRelicMetric::getName).collect(Collectors.toSet());
      String baseUrl = newRelicConfig.getNewRelicUrl().endsWith("/") ? newRelicConfig.getNewRelicUrl()
                                                                     : newRelicConfig.getNewRelicUrl() + "/";
      baseUrl += "v2/applications/" + newRelicApplicationId + "/instances/" + instanceId + "/metrics/data.json?";
      NewRelicMetricData newRelicMetricData =
          getMetricData(newRelicConfig, encryptedDataDetails, baseUrl, metricNames, fromTime, toTime, apiCallLog);
      return VerificationNodeDataSetupResponse.builder()
          .providerReachable(true)
          .dataForNode(newRelicMetricData)
          .loadResponse(VerificationLoadResponse.builder().isLoadPresent(true).loadResponse(txnsWithData).build())
          .build();
    } catch (Exception e) {
      logger.info("Error while getting data for node", e);
      return VerificationNodeDataSetupResponse.builder().providerReachable(false).build();
    }
  }

  private NewRelicRestClient getNewRelicRestClient(
      final NewRelicConfig newRelicConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    encryptionService.decrypt(newRelicConfig, encryptedDataDetails);
    OkHttpClient.Builder httpClient = Http.getOkHttpClientWithNoProxyValueSet(newRelicConfig.getNewRelicUrl());
    httpClient.addInterceptor(chain -> {
      Request original = chain.request();

      Request request = original.newBuilder()
                            .header("Accept", "application/json")
                            .header("Content-Type", "application/json")
                            .header("X-Api-Key", new String(newRelicConfig.getApiKey()))
                            .method(original.method(), original.body())
                            .build();

      return chain.proceed(request);
    });

    final String baseUrl = newRelicConfig.getNewRelicUrl().endsWith("/") ? newRelicConfig.getNewRelicUrl()
                                                                         : newRelicConfig.getNewRelicUrl() + "/";
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(baseUrl)
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .client(httpClient.build())
                                  .build();
    return retrofit.create(NewRelicRestClient.class);
  }

  public static List<Set<String>> batchMetricsToCollect(Collection<NewRelicMetric> metrics) {
    List<Set<String>> rv = new ArrayList<>();

    Set<String> batchedMetrics = new HashSet<>();
    for (NewRelicMetric metric : metrics) {
      batchedMetrics.add(metric.getName());

      if (batchedMetrics.size() == METRIC_DATA_QUERY_BATCH_SIZE) {
        rv.add(batchedMetrics);
        batchedMetrics = new HashSet<>();
      }
    }

    if (!batchedMetrics.isEmpty()) {
      rv.add(batchedMetrics);
    }

    return rv;
  }

  public static Set<String> getApdexMetricNames(Collection<String> metricNames) {
    final Set<String> rv = new HashSet<>();
    for (String metricName : metricNames) {
      rv.add(metricName.replace("WebTransaction", "Apdex"));
    }

    return rv;
  }

  public static Set<String> getErrorMetricNames(Collection<String> metricNames) {
    final Set<String> rv = new HashSet<>();
    for (String metricName : metricNames) {
      rv.add("Errors/" + metricName);
    }

    return rv;
  }
}
