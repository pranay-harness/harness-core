package software.wings.service.impl.newrelic;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.NewRelicDeploymentMarkerPayload;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.newrelic.NewRelicRestClient;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.HttpUtil;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
/**
 * Created by rsingh on 8/28/17.
 */
public class NewRelicDelgateServiceImpl implements NewRelicDelegateService {
  private static final String NEW_RELIC_DATE_FORMAT = "YYYY-MM-dd'T'HH:mm:ssZ";
  public static final SimpleDateFormat dateFormatter = new SimpleDateFormat(NEW_RELIC_DATE_FORMAT);

  private static final Logger logger = LoggerFactory.getLogger(NewRelicDelgateServiceImpl.class);
  @Inject private EncryptionService encryptionService;

  @Override
  public boolean validateConfig(NewRelicConfig newRelicConfig) throws IOException {
    getAllApplications(newRelicConfig, Collections.emptyList());
    return true;
  }

  @Override
  public List<NewRelicApplication> getAllApplications(
      NewRelicConfig newRelicConfig, List<EncryptedDataDetail> encryptedDataDetails) throws IOException {
    List<NewRelicApplication> rv = new ArrayList<>();
    int pageCount = 1;
    while (true) {
      final Call<NewRelicApplicationsResponse> request =
          getNewRelicRestClient(newRelicConfig, encryptedDataDetails).listAllApplications(pageCount);
      final Response<NewRelicApplicationsResponse> response = request.execute();
      if (response.isSuccessful()) {
        List<NewRelicApplication> applications = response.body().getApplications();
        if (isEmpty(applications)) {
          break;
        } else {
          rv.addAll(applications);
        }
      } else {
        JSONObject errorObject = new JSONObject(response.errorBody().string());
        throw new WingsException(errorObject.getJSONObject("error").getString("title"));
      }

      pageCount++;
    }

    return rv;
  }

  @Override
  public List<NewRelicApplicationInstance> getApplicationInstances(NewRelicConfig newRelicConfig,
      List<EncryptedDataDetail> encryptedDataDetails, long newRelicApplicationId) throws IOException {
    List<NewRelicApplicationInstance> rv = new ArrayList<>();
    int pageCount = 1;
    while (true) {
      final Call<NewRelicApplicationInstancesResponse> request =
          getNewRelicRestClient(newRelicConfig, encryptedDataDetails)
              .listAppInstances(newRelicApplicationId, pageCount);
      final Response<NewRelicApplicationInstancesResponse> response = request.execute();
      if (response.isSuccessful()) {
        List<NewRelicApplicationInstance> applicationInstances = response.body().getApplication_instances();
        if (isEmpty(applicationInstances)) {
          break;
        } else {
          rv.addAll(applicationInstances);
        }
      } else {
        JSONObject errorObject = new JSONObject(response.errorBody().string());
        throw new WingsException(errorObject.getJSONObject("error").getString("title"));
      }

      pageCount++;
    }

    return rv;
  }

  @Override
  public Collection<NewRelicMetric> getMetricsNameToCollect(NewRelicConfig newRelicConfig,
      List<EncryptedDataDetail> encryptedDataDetails, long newRelicAppId) throws IOException {
    final Call<NewRelicMetricResponse> request =
        getNewRelicRestClient(newRelicConfig, encryptedDataDetails).listMetricNames(newRelicAppId);
    final Response<NewRelicMetricResponse> response = request.execute();
    if (response.isSuccessful()) {
      List<NewRelicMetric> metrics = response.body().getMetrics();
      if (metrics == null) {
        return Collections.emptyList();
      }
      List<NewRelicMetric> newRelicMetrics = new ArrayList<>();
      for (NewRelicMetric metric : metrics) {
        if (metric.getName().startsWith("WebTransaction/")) {
          newRelicMetrics.add(metric);
        }
      }
      return newRelicMetrics;
    }

    JSONObject errorObject = new JSONObject(response.errorBody().string());
    throw new WingsException(errorObject.getJSONObject("error").getString("title"));
  }

  @Override
  public NewRelicMetricData getMetricData(NewRelicConfig newRelicConfig, List<EncryptedDataDetail> encryptedDataDetails,
      long newRelicApplicationId, long instanceId, Collection<String> metricNames, long fromTime, long toTime)
      throws IOException {
    String metricsToCollectString = "";
    for (String metricName : metricNames) {
      metricsToCollectString += "names[]=" + metricName + "&";
    }

    metricsToCollectString = StringUtils.removeEnd(metricsToCollectString, "&");

    final String baseUrl = newRelicConfig.getNewRelicUrl().endsWith("/") ? newRelicConfig.getNewRelicUrl()
                                                                         : newRelicConfig.getNewRelicUrl() + "/";
    final String url = baseUrl + "v2/applications/" + newRelicApplicationId + "/instances/" + instanceId
        + "/metrics/data.json?" + metricsToCollectString;
    final Call<NewRelicMetricDataResponse> request =
        getNewRelicRestClient(newRelicConfig, encryptedDataDetails)
            .getRawMetricData(url, dateFormatter.format(new Date(fromTime)), dateFormatter.format(new Date(toTime)));
    final Response<NewRelicMetricDataResponse> response = request.execute();
    if (response.isSuccessful()) {
      return response.body().getMetric_data();
    }

    JSONObject errorObject = new JSONObject(response.errorBody().string());
    throw new WingsException(errorObject.getJSONObject("error").getString("title"));
  }

  @Override
  public String postDeploymentMarker(NewRelicConfig config, List<EncryptedDataDetail> encryptedDataDetails,
      long newRelicApplicationId, NewRelicDeploymentMarkerPayload body) throws IOException {
    final String baseUrl =
        config.getNewRelicUrl().endsWith("/") ? config.getNewRelicUrl() : config.getNewRelicUrl() + "/";
    final String url = baseUrl + "v2/applications/" + newRelicApplicationId + "/deployments.json";
    final Call<Object> request = getNewRelicRestClient(config, encryptedDataDetails).postDeploymentMarker(url, body);
    final Response<Object> response = request.execute();
    if (response.isSuccessful()) {
      return "Successfully posted deployment marker to NewRelic";
    }

    JSONObject errorObject = new JSONObject(response.errorBody().string());
    throw new WingsException(errorObject.getJSONObject("error").getString("title"));
  }

  private NewRelicRestClient getNewRelicRestClient(
      final NewRelicConfig newRelicConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    encryptionService.decrypt(newRelicConfig, encryptedDataDetails);
    OkHttpClient.Builder httpClient = HttpUtil.getOkHttpClientWithNoProxyValueSet(newRelicConfig.getNewRelicUrl());
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
