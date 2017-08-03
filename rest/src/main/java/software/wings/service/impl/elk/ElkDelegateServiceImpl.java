package software.wings.service.impl.elk;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.entity.ContentType;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.ElkConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.elk.ElkRestClient;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created by rsingh on 8/01/17.
 */
public class ElkDelegateServiceImpl implements ElkDelegateService {
  @Override
  public void validateConfig(ElkConfig elkConfig) {
    try {
      final Call<ElkAuthenticationResponse> request =
          getElkRestClient(elkConfig).authenticate(getHeaderWithCredentials(elkConfig));
      final Response<ElkAuthenticationResponse> response = request.execute();
      if (response.isSuccessful()) {
        return;
      }

      throw new WingsException(
          JsonUtils.asObject(response.errorBody().string(), ElkAuthenticationResponse.class).getError().getReason());
    } catch (Throwable t) {
      throw new WingsException(t.getMessage());
    }
  }

  @Override
  public Object search(ElkConfig elkConfig, ElkLogFetchRequest logFetchRequest) throws IOException {
    final Call<Object> request = getElkRestClient(elkConfig).search(logFetchRequest.toElasticSearchJson());
    final Response<Object> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    }

    throw new WingsException(response.errorBody().string());
  }

  private ElkRestClient getElkRestClient(final ElkConfig elkConfig) {
    OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
    httpClient.addInterceptor(chain -> {
      Request original = chain.request();

      Request request = original.newBuilder()
                            .header("Accept", "application/json")
                            .header("Content-Type", "application/json")
                            .header("Authorization", getHeaderWithCredentials(elkConfig))
                            .method(original.method(), original.body())
                            .build();

      return chain.proceed(request);
    });

    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl("http://" + elkConfig.getHost() + ":" + elkConfig.getPort() + "/")
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .client(httpClient.build())
                                  .build();
    return retrofit.create(ElkRestClient.class);
  }

  private String getHeaderWithCredentials(ElkConfig elkConfig) {
    return "Basic "
        + Base64.encodeBase64String(String.format("%s:%s", elkConfig.getUsername(), new String(elkConfig.getPassword()))
                                        .getBytes(StandardCharsets.UTF_8));
  }
}
