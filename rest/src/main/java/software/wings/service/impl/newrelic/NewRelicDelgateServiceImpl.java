package software.wings.service.impl.newrelic;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
    final Call<NewRelicApplicationsResponse> request = getNewRelicRestClient(newRelicConfig).listAllApplications();
    final Response<NewRelicApplicationsResponse> response = request.execute();
    if (response.isSuccessful()) {
      return response.body().getApplications();
    }

    JSONObject errorObject = new JSONObject(response.errorBody().string());
    throw new WingsException(errorObject.getJSONObject("error").getString("title"));
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
  public NewRelicMetricData getMetricData(NewRelicConfig newRelicConfig, long newRelicApplicationId, long instanceId,
      String metricName, List<String> valuesToCollect, long fromTime, long toTime) throws IOException {
    String valuesToCollectString = "";
    for (String valueToCollect : valuesToCollect) {
      valuesToCollectString += "values[]=" + valueToCollect + "&";
    }

    valuesToCollectString = StringUtils.removeEnd(valuesToCollectString, "&");

    final String baseUrl = newRelicConfig.getNewRelicUrl().endsWith("/") ? newRelicConfig.getNewRelicUrl()
                                                                         : newRelicConfig.getNewRelicUrl() + "/";
    final String url = baseUrl + "v2/applications/" + newRelicApplicationId + "/instances/" + instanceId
        + "/metrics/data.json?" + valuesToCollectString;
    final Call<NewRelicMetricDataResponse> request =
        getNewRelicRestClient(newRelicConfig)
            .getRawMetricData(
                url, metricName, dateFormatter.format(new Date(fromTime)), dateFormatter.format(new Date(toTime)));
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

  public static void main(String[] args) throws ParseException {
    //    2017-08-29T22:48:00-0700
    System.out.println(new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ssZ").parse("2017-08-29T22:48:00-0700"));
  }
}
