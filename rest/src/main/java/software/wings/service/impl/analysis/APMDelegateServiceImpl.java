package software.wings.service.impl.analysis;

import io.harness.network.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.apm.APMRestClient;

import java.util.concurrent.TimeUnit;

public class APMDelegateServiceImpl implements APMDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(APMDelegateServiceImpl.class);

  @Override
  public boolean validateCollector(APMValidateCollectorConfig config) {
    config.getHeaders().put("Accept", "application/json");
    final Call<Object> request =
        getAPMRestClient(config).validate(config.getUrl(), config.getHeaders(), config.getOptions());
    final Response<Object> response;
    try {
      response = request.execute();
      if (response.isSuccessful()) {
        return true;
      } else {
        logger.error("Request not successful. Reason: {}", response);
        throw new WingsException(response.errorBody().string());
      }
    } catch (Exception e) {
      throw new WingsException(e);
    }
  }

  private APMRestClient getAPMRestClient(final APMValidateCollectorConfig config) {
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(config.getBaseUrl())
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .client(Http.getOkHttpClientWithNoProxyValueSet(config.getBaseUrl())
                                              .connectTimeout(30, TimeUnit.SECONDS)
                                              .build())
                                  .build();
    return retrofit.create(APMRestClient.class);
  }
}
