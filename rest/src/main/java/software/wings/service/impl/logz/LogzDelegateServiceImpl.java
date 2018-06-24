package software.wings.service.impl.logz;

import static io.harness.network.Http.getOkHttpClientBuilder;

import com.google.inject.Inject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.config.LogzConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.logz.LogzRestClient;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.elk.ElkLogFetchRequest;
import software.wings.service.intfc.logz.LogzDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.JsonUtils;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.List;

/**
 * Created by rsingh on 8/21/17.
 */
public class LogzDelegateServiceImpl implements LogzDelegateService {
  private static final Object logzQuery = JsonUtils.asObject(
      "{ \"size\": 0, \"query\": { \"bool\": { \"must\": [{ \"range\": { \"@timestamp\": { \"gte\": \"now-5m\", \"lte\": \"now\" } } }] } }, \"aggs\": { \"byType\": { \"terms\": { \"field\": \"type\", \"size\": 5 } } } }",
      Object.class);
  @Inject private EncryptionService encryptionService;
  @Override
  public boolean validateConfig(LogzConfig logzConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      final Call<Object> request = getLogzRestClient(logzConfig, encryptedDataDetails).search(logzQuery);
      final Response<Object> response = request.execute();
      if (response.isSuccessful()) {
        return true;
      }
      throw new WingsException(response.errorBody().string());
    } catch (Exception exception) {
      throw new WingsException("Error validating LOGZ config " + Misc.getMessage(exception), exception);
    }
  }

  @Override
  public Object search(LogzConfig logzConfig, List<EncryptedDataDetail> encryptedDataDetails,
      ElkLogFetchRequest logFetchRequest) throws IOException {
    final Call<Object> request =
        getLogzRestClient(logzConfig, encryptedDataDetails).search(logFetchRequest.toElasticSearchJsonObject());
    final Response<Object> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    }

    throw new WingsException(response.errorBody().string());
  }

  @Override
  public Object getLogSample(LogzConfig logzConfig, List<EncryptedDataDetail> encryptedDataDetails) throws IOException {
    final Call<Object> request =
        getLogzRestClient(logzConfig, encryptedDataDetails).getLogSample(ElkLogFetchRequest.lastInsertedRecordObject());
    final Response<Object> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    }
    throw new WingsException(response.errorBody().string());
  }

  private LogzRestClient getLogzRestClient(
      final LogzConfig logzConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    encryptionService.decrypt(logzConfig, encryptedDataDetails);
    OkHttpClient.Builder httpClient = getOkHttpClientBuilder();
    httpClient.addInterceptor(chain -> {
      Request original = chain.request();

      Request request = original.newBuilder()
                            .header("Accept", "application/json")
                            .header("Content-Type", "application/json")
                            .header("X-USER-TOKEN", new String(logzConfig.getToken()))
                            .method(original.method(), original.body())
                            .build();

      return chain.proceed(request);
    });

    final String baseUrl =
        logzConfig.getLogzUrl().endsWith("/") ? logzConfig.getLogzUrl() : logzConfig.getLogzUrl() + "/";
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(baseUrl)
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .client(httpClient.build())
                                  .build();
    return retrofit.create(LogzRestClient.class);
  }
}
