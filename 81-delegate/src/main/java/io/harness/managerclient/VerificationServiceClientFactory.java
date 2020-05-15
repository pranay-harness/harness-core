package io.harness.managerclient;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.harness.network.Http;
import io.harness.security.TokenGenerator;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class VerificationServiceClientFactory implements Provider<VerificationServiceClient> {
  public static final ImmutableList<TrustManager> TRUST_ALL_CERTS =
      ImmutableList.of(new DelegateAgentManagerClientX509TrustManager());

  private String baseUrl;
  private TokenGenerator tokenGenerator;

  public VerificationServiceClientFactory(String baseUrl, TokenGenerator delegateTokenGenerator) {
    this.baseUrl = baseUrl;
    this.tokenGenerator = delegateTokenGenerator;
  }

  @Override
  public VerificationServiceClient get() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new JavaTimeModule());
    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .client(getUnsafeOkHttpClient())
                            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                            .build();
    return retrofit.create(VerificationServiceClient.class);
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
          .hostnameVerifier((hostname, session) -> true)
          .build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
