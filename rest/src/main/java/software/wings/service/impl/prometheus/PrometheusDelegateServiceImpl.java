package software.wings.service.impl.prometheus;

import io.harness.network.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.PrometheusConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.prometheus.PrometheusRestClient;
import software.wings.service.intfc.prometheus.PrometheusDelegateService;
import software.wings.utils.Misc;

import java.io.IOException;

/**
 * Created by rsingh on 3/14/18.
 */
public class PrometheusDelegateServiceImpl implements PrometheusDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(PrometheusDelegateServiceImpl.class);

  @Override
  public boolean validateConfig(PrometheusConfig prometheusConfig) throws IOException {
    try {
      final Call<PrometheusMetricDataResponse> request =
          getRestClient(prometheusConfig).fetchMetricData("api/v1/query?query=up");
      final Response<PrometheusMetricDataResponse> response = request.execute();
      if (response.isSuccessful()) {
        return true;
      } else {
        logger.error("Request not successful. Reason: {}", response);
        throw new WingsException(response.errorBody().string());
      }
    } catch (Exception e) {
      throw new WingsException("Could not validate prometheus server. " + Misc.getMessage(e), e);
    }
  }

  @Override
  public PrometheusMetricDataResponse fetchMetricData(PrometheusConfig prometheusConfig, String url)
      throws IOException {
    final Call<PrometheusMetricDataResponse> request = getRestClient(prometheusConfig).fetchMetricData(url);
    final Response<PrometheusMetricDataResponse> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    } else {
      logger.error("Request not successful. Reason: {}, url: {}", response, url);
      throw new WingsException(response.errorBody().string());
    }
  }

  private PrometheusRestClient getRestClient(PrometheusConfig prometheusConfig) {
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(prometheusConfig.getUrl())
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .client(Http.getOkHttpClientWithNoProxyValueSet(prometheusConfig.getUrl()).build())
                                  .build();
    return retrofit.create(PrometheusRestClient.class);
  }
}
