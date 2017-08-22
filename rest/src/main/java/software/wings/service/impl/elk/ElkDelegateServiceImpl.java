package software.wings.service.impl.elk;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.codec.binary.Base64;
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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
    final Call<Object> request =
        getElkRestClient(elkConfig, logFetchRequest.getIndices()).search(logFetchRequest.toElasticSearchJsonObject());
    final Response<Object> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    }

    throw new WingsException(response.errorBody().string());
  }

  private ElkRestClient getElkRestClient(final ElkConfig elkConfig) {
    return getElkRestClient(elkConfig, "");
  }
  private ElkRestClient getElkRestClient(final ElkConfig elkConfig, String indices) {
    OkHttpClient.Builder httpClient =
        elkConfig.getElkUrl().startsWith("https") ? getUnsafeOkHttpClient() : new OkHttpClient.Builder();
    httpClient
        .addInterceptor(chain -> {
          Request original = chain.request();

          Request request = original.newBuilder()
                                .header("Accept", "application/json")
                                .header("Content-Type", "application/json")
                                .header("Authorization", getHeaderWithCredentials(elkConfig))
                                .method(original.method(), original.body())
                                .build();

          return chain.proceed(request);
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS);

    String baseUrl = elkConfig.getElkUrl();
    if (baseUrl.charAt(baseUrl.length() - 1) != '/') {
      baseUrl = baseUrl + "/";
    }
    baseUrl = !indices.isEmpty() ? baseUrl + indices + "/" : baseUrl;

    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(baseUrl)
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

  private static OkHttpClient.Builder getUnsafeOkHttpClient() {
    try {
      // Create a trust manager that does not validate certificate chains
      final TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager(){
          @Override public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
              throws CertificateException{}

          @Override public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
              throws CertificateException{}

                     @Override public java.security.cert.X509Certificate[] getAcceptedIssuers(){
                         return new X509Certificate[] {};
    }
  }
};

// Install the all-trusting trust manager
final SSLContext sslContext = SSLContext.getInstance("SSL");
sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
// Create an ssl socket factory with our all-trusting manager
final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

OkHttpClient.Builder builder = new OkHttpClient.Builder();
builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
builder.hostnameVerifier((hostname, session) -> true);

return builder;
}
catch (Exception e) {
  throw new RuntimeException(e);
}
}
}
