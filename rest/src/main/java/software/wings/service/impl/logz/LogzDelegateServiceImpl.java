package software.wings.service.impl.logz;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.config.LogzConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.logz.LogzRestClient;
import software.wings.service.impl.elk.ElkLogFetchRequest;
import software.wings.service.intfc.logz.LogzDelegateService;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 8/21/17.
 */
public class LogzDelegateServiceImpl implements LogzDelegateService {
  @Override
  public void validateConfig(LogzConfig logzConfig) {
    try {
      final ElkLogFetchRequest logFetchRequest = new ElkLogFetchRequest("Exception", null, Collections.EMPTY_SET,
          System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1), System.currentTimeMillis());
      final Call<Object> request = getLogzRestClient(logzConfig).search(logFetchRequest.toElasticSearchJsonObject());
      final Response<Object> response = request.execute();
      if (response.isSuccessful()) {
        return;
      }
      throw new WingsException(response.errorBody().string());
    } catch (Throwable t) {
      throw new WingsException(t.getMessage());
    }
  }

  @Override
  public Object search(LogzConfig logzConfig, ElkLogFetchRequest logFetchRequest) throws IOException {
    final Call<Object> request = getLogzRestClient(logzConfig).search(logFetchRequest.toElasticSearchJsonObject());
    final Response<Object> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    }

    throw new WingsException(response.errorBody().string());
  }

  private LogzRestClient getLogzRestClient(final LogzConfig logzConfig) {
    OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
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
