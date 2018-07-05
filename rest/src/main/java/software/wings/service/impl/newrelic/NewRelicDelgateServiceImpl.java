package software.wings.service.impl.newrelic;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.service.impl.ThirdPartyApiCallLog.apiCallLogWithDummyStateExecution;
import static software.wings.service.impl.security.SecretManagementDelegateServiceImpl.NUM_OF_RETRIES;

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
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.newrelic.NewRelicRestClient;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by rsingh on 8/28/17.
 */
@SuppressFBWarnings("STCAL_STATIC_SIMPLE_DATE_FORMAT_INSTANCE")
public class NewRelicDelgateServiceImpl implements NewRelicDelegateService {
  private static final String NEW_RELIC_DATE_FORMAT = "YYYY-MM-dd'T'HH:mm:ssZ";
  public static final SimpleDateFormat dateFormatter = new SimpleDateFormat(NEW_RELIC_DATE_FORMAT);

  private static final Logger logger = LoggerFactory.getLogger(NewRelicDelgateServiceImpl.class);
  @Inject private EncryptionService encryptionService;
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
      apiCallLog = (ThirdPartyApiCallLog) apiCallLog.clone();
      apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toEpochSecond());
      apiCallLog.setRequest(newRelicConfig.getNewRelicUrl() + "/v2/applications.json?page=" + pageCount);
      final Call<NewRelicApplicationsResponse> request =
          getNewRelicRestClient(newRelicConfig, encryptedDataDetails).listAllApplications(pageCount);
      final Response<NewRelicApplicationsResponse> response = request.execute();
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toEpochSecond());
      apiCallLog.setStatusCode(response.code());
      if (response.isSuccessful()) {
        apiCallLog.setJsonResponse(response.body());
        List<NewRelicApplication> applications = response.body().getApplications();
        delegateLogService.save(newRelicConfig.getAccountId(), apiCallLog);
        if (isEmpty(applications)) {
          break;
        } else {
          rv.addAll(applications);
        }
      } else {
        apiCallLog.setResponse(response.errorBody().string());
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
      apiCallLog = (ThirdPartyApiCallLog) apiCallLog.clone();
      apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toEpochSecond());
      apiCallLog.setRequest(newRelicConfig.getNewRelicUrl() + "/v2/applications/" + newRelicApplicationId
          + "/instances.json?page=" + pageCount);
      final Call<NewRelicApplicationInstancesResponse> request =
          getNewRelicRestClient(newRelicConfig, encryptedDataDetails)
              .listAppInstances(newRelicApplicationId, pageCount);
      final Response<NewRelicApplicationInstancesResponse> response = request.execute();
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toEpochSecond());
      apiCallLog.setStatusCode(response.code());
      if (response.isSuccessful()) {
        apiCallLog.setJsonResponse(response.body());
        List<NewRelicApplicationInstance> applicationInstances = response.body().getApplication_instances();
        delegateLogService.save(newRelicConfig.getAccountId(), apiCallLog);
        if (isEmpty(applicationInstances)) {
          break;
        } else {
          rv.addAll(applicationInstances);
        }
      } else {
        apiCallLog.setResponse(response.errorBody().string());
        delegateLogService.save(newRelicConfig.getAccountId(), apiCallLog);
        throw new WingsException(response.errorBody().string());
      }

      pageCount++;
    }

    return rv;
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
        apiCallLog.setRequest(newRelicConfig.getNewRelicUrl() + "/v2/applications/" + newRelicAppId
            + "/metrics.json?name=WebTransaction");
        apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toEpochSecond());
        final Call<NewRelicMetricResponse> request =
            getNewRelicRestClient(newRelicConfig, encryptedDataDetails).listMetricNames(newRelicAppId);
        final Response<NewRelicMetricResponse> response = request.execute();
        apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toEpochSecond());
        apiCallLog.setStatusCode(response.code());
        if (response.isSuccessful()) {
          apiCallLog.setJsonResponse(response.body());
          List<NewRelicMetric> metrics = response.body().getMetrics();
          if (isNotEmpty(metrics)) {
            metrics.forEach(metric -> {
              if (metric.getName().startsWith("WebTransaction/") || metric.getName().equals("WebTransaction")) {
                newRelicMetrics.add(metric);
              }
            });
          }
        } else if (response.code() != HttpServletResponse.SC_NOT_FOUND) {
          apiCallLog.setResponse(response.errorBody().string());
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
    apiCallLog.setRequest(url);
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toEpochSecond());
    final Call<NewRelicMetricDataResponse> request =
        getNewRelicRestClient(newRelicConfig, encryptedDataDetails)
            .getRawMetricData(url, dateFormatter.format(new Date(fromTime)), dateFormatter.format(new Date(toTime)));
    final Response<NewRelicMetricDataResponse> response = request.execute();
    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toEpochSecond());
    apiCallLog.setStatusCode(response.code());
    if (response.isSuccessful()) {
      apiCallLog.setJsonResponse(response.body());
      delegateLogService.save(newRelicConfig.getAccountId(), apiCallLog);
      return response.body().getMetric_data();
    }

    apiCallLog.setResponse(response.errorBody().string());
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
    apiCallLog.setRequest("ur: " + url + " body: " + body);
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toEpochSecond());
    final Call<Object> request = getNewRelicRestClient(config, encryptedDataDetails).postDeploymentMarker(url, body);
    final Response<Object> response = request.execute();
    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toEpochSecond());
    apiCallLog.setStatusCode(response.code());
    if (response.isSuccessful()) {
      apiCallLog.setJsonResponse(response.body());
      delegateLogService.save(config.getAccountId(), apiCallLog);
      return "Successfully posted deployment marker to NewRelic";
    }

    apiCallLog.setResponse(response.errorBody().string());
    delegateLogService.save(config.getAccountId(), apiCallLog);
    throw new WingsException(response.errorBody().string());
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
}
