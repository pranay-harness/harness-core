package software.wings.service.impl.newrelic;

import com.google.common.collect.Sets;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.NewRelicConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.newrelic.NewRelicRestClient;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 8/28/17.
 */
public class NewRelicDelgateServiceImpl implements NewRelicDelegateService {
  private static final String NEW_RELIC_DATE_FORMAT = "YYYY-MM-dd'T'HH:mm:ssZ";
  private static final SimpleDateFormat dateFormatter = new SimpleDateFormat(NEW_RELIC_DATE_FORMAT);

  private static final Logger logger = LoggerFactory.getLogger(NewRelicDelgateServiceImpl.class);

  @Override
  public void validateConfig(NewRelicConfig newRelicConfig) throws IOException {
    getAllApplications(newRelicConfig);
  }

  @Override
  public List<NewRelicApplication> getAllApplications(NewRelicConfig newRelicConfig) throws IOException {
    List<NewRelicApplication> rv = new ArrayList<>();
    int pageCount = 1;
    while (true) {
      final Call<NewRelicApplicationsResponse> request =
          getNewRelicRestClient(newRelicConfig).listAllApplications(pageCount);
      final Response<NewRelicApplicationsResponse> response = request.execute();
      if (response.isSuccessful()) {
        List<NewRelicApplication> applications = response.body().getApplications();
        if (applications == null || applications.isEmpty()) {
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
  public List<NewRelicApplicationInstance> getApplicationInstances(
      NewRelicConfig newRelicConfig, long newRelicApplicationId) throws IOException {
    final Call<NewRelicApplicationInstancesResponse> request =
        getNewRelicRestClient(newRelicConfig).listAppInstances(newRelicApplicationId);
    final Response<NewRelicApplicationInstancesResponse> response = request.execute();
    if (response.isSuccessful()) {
      return response.body().getApplication_instances();
    }

    JSONObject errorObject = new JSONObject(response.errorBody().string());
    throw new WingsException(errorObject.getJSONObject("error").getString("title"));
  }

  @Override
  public Collection<NewRelicMetric> getMetricsNameToCollect(NewRelicConfig newRelicConfig, long newRelicAppId)
      throws IOException {
    final Call<NewRelicMetricResponse> request = getNewRelicRestClient(newRelicConfig).listMetricNames(newRelicAppId);
    final Response<NewRelicMetricResponse> response = request.execute();
    if (response.isSuccessful()) {
      List<NewRelicMetric> metrics = response.body().getMetrics();
      Map<String, NewRelicMetric> webTransactionMetrics = new HashMap<>();

      if (metrics == null) {
        return Collections.emptyList();
      }

      for (NewRelicMetric metric : metrics) {
        if (metric.getName().startsWith("WebTransaction/")) {
          webTransactionMetrics.put(metric.getName(), metric);
        }
      }

      // find and remove metrics which have no data in last 24 hours
      List<NewRelicApplicationInstance> applicationInstances = getApplicationInstances(newRelicConfig, newRelicAppId);
      final long currentTime = System.currentTimeMillis();
      Set<String> metricsWithNoData = Sets.newHashSet(webTransactionMetrics.keySet());
      for (NewRelicApplicationInstance applicationInstance : applicationInstances) {
        NewRelicMetricData metricData = getMetricData(newRelicConfig, newRelicAppId, applicationInstance.getId(),
            metricsWithNoData, currentTime - TimeUnit.DAYS.toMillis(1), currentTime);
        metricsWithNoData.removeAll(metricData.getMetrics_found());

        if (metricsWithNoData.isEmpty()) {
          break;
        }
      }
      for (String metricName : metricsWithNoData) {
        webTransactionMetrics.remove(metricName);
      }
      return webTransactionMetrics.values();
    }

    JSONObject errorObject = new JSONObject(response.errorBody().string());
    throw new WingsException(errorObject.getJSONObject("error").getString("title"));
  }

  @Override
  public NewRelicMetricData getMetricData(NewRelicConfig newRelicConfig, long newRelicApplicationId, long instanceId,
      Collection<String> metricNames, long fromTime, long toTime) throws IOException {
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
        getNewRelicRestClient(newRelicConfig)
            .getRawMetricData(url, dateFormatter.format(new Date(fromTime)), dateFormatter.format(new Date(toTime)));
    final Response<NewRelicMetricDataResponse> response = request.execute();
    if (response.isSuccessful()) {
      return response.body().getMetric_data();
    }

    JSONObject errorObject = new JSONObject(response.errorBody().string());
    throw new WingsException(errorObject.getJSONObject("error").getString("title"));
  }

  private NewRelicRestClient getNewRelicRestClient(final NewRelicConfig newRelicConfig) {
    OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
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
