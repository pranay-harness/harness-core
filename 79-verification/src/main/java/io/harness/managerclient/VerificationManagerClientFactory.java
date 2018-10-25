package io.harness.managerclient;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.harness.network.Http;
import io.harness.security.VerificationAuthInterceptor;
import io.harness.security.VerificationTokenGenerator;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by raghu on 11/29/16.
 */

class ManagerClientX509TrustManager implements X509TrustManager {
  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[] {};
  }

  public void checkClientTrusted(X509Certificate[] certs, String authType) {}

  public void checkServerTrusted(X509Certificate[] certs, String authType) {}
}

public class VerificationManagerClientFactory implements Provider<VerificationManagerClient> {
  public static final ImmutableList<TrustManager> TRUST_ALL_CERTS =
      ImmutableList.of(new ManagerClientX509TrustManager());

  private String baseUrl;
  private VerificationTokenGenerator tokenGenerator;

  public VerificationManagerClientFactory(String baseUrl, VerificationTokenGenerator tokenGenerator) {
    this.baseUrl = baseUrl;
    this.tokenGenerator = tokenGenerator;
  }

  @Override
  public VerificationManagerClient get() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new JavaTimeModule());
    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .client(getUnsafeOkHttpClient())
                            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                            .build();
    return retrofit.create(VerificationManagerClient.class);
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
          .addInterceptor(new VerificationAuthInterceptor(tokenGenerator))
          .sslSocketFactory(sslSocketFactory, (X509TrustManager) TRUST_ALL_CERTS.get(0))
          .hostnameVerifier((hostname, session) -> true)
          .build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
