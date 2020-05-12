package io.harness.managerclient;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.harness.network.FibonacciBackOff;
import io.harness.network.Http;
import io.harness.security.TokenGenerator;
import io.harness.version.VersionInfoManager;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

class ManagerClientX509TrustManager implements X509TrustManager {
  @Override
  public java.security.cert.X509Certificate[] getAcceptedIssuers() {
    return new java.security.cert.X509Certificate[] {};
  }

  @Override
  public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}

  @Override
  public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
}

public class ManagerClientFactory implements Provider<ManagerClient> {
  public static final ImmutableList<TrustManager> TRUST_ALL_CERTS =
      ImmutableList.of(new ManagerClientX509TrustManager());

  private static boolean sendVersionHeader = true;

  @Inject private VersionInfoManager versionInfoManager;

  private String baseUrl;
  private TokenGenerator tokenGenerator;

  public static void setSendVersionHeader(boolean send) {
    sendVersionHeader = send;
  }

  ManagerClientFactory(String baseUrl, TokenGenerator tokenGenerator) {
    this.baseUrl = baseUrl;
    this.tokenGenerator = tokenGenerator;
  }

  @Override
  public ManagerClient get() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new JavaTimeModule());
    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .client(getUnsafeOkHttpClient())
                            .addConverterFactory(new KryoConverterFactory())
                            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                            .build();
    return retrofit.create(ManagerClient.class);
  }

  private OkHttpClient getUnsafeOkHttpClient() {
    try {
      // Install the all-trusting trust manager
      final SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, TRUST_ALL_CERTS.toArray(new TrustManager[1]), new java.security.SecureRandom());
      // Create an ssl socket factory with our all-trusting manager
      final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

      return Http.getOkHttpClientWithProxyAuthSetup()
          .connectionPool(new ConnectionPool())
          .retryOnConnectionFailure(true)
          .addInterceptor(new DelegateAuthInterceptor(tokenGenerator))
          .sslSocketFactory(sslSocketFactory, (X509TrustManager) TRUST_ALL_CERTS.get(0))
          .addInterceptor(chain -> {
            Builder request = chain.request().newBuilder().addHeader(
                "User-Agent", "delegate/" + versionInfoManager.getVersionInfo().getVersion());
            if (sendVersionHeader) {
              request.addHeader("Version", versionInfoManager.getVersionInfo().getVersion());
            }
            return chain.proceed(request.build());
          })
          .addInterceptor(chain -> FibonacciBackOff.executeForEver(() -> chain.proceed(chain.request())))
          .hostnameVerifier((hostname, session) -> true)
          // During this call we not just query the task but we also obtain the secret on the manager side
          // we need to give enough time for the call to finish.
          .readTimeout(2, TimeUnit.MINUTES)
          .build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
