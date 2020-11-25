package io.harness.cvng.client;

import io.harness.exception.InvalidRequestException;
import io.harness.network.Http;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.JsonSubtypeResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provider;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Slf4j
public class CVNGServiceClientFactory implements Provider<CVNGServiceClient> {
  public static final ImmutableList<TrustManager> TRUST_ALL_CERTS =
      ImmutableList.of(new ManagerClientX509TrustManager());
  static class ManagerClientX509TrustManager implements X509TrustManager {
    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[] {
          // internal network
      };
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certs, String authType) {
      // internal network so no need to check
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType) {
      // internal network so no need to check
    }
  }
  private static final String CLIENT_ID = "manager";
  private String baseUrl;
  private ServiceTokenGenerator tokenGenerator;
  private String cvNgServiceSecret;
  public CVNGServiceClientFactory(String baseUrl, String cvNgServiceSecret, ServiceTokenGenerator tokenGenerator) {
    this.baseUrl = baseUrl;
    this.tokenGenerator = tokenGenerator;
    this.cvNgServiceSecret = cvNgServiceSecret;
  }

  @Override
  public CVNGServiceClient get() {
    // Install the all-trusting trust manager
    final SSLContext sslContext;
    try {
      sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, TRUST_ALL_CERTS.toArray(new TrustManager[1]), new java.security.SecureRandom());
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new IllegalStateException(e);
    }

    final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
    OkHttpClient okHttpClient = Http.getOkHttpClientWithProxyAuthSetup()
                                    .connectionPool(new ConnectionPool(0, 5, TimeUnit.MINUTES))
                                    .readTimeout(30, TimeUnit.SECONDS)
                                    .retryOnConnectionFailure(true)
                                    .addInterceptor(getAuthorizationInterceptor())
                                    .hostnameVerifier((hostname, session) -> true)
                                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) TRUST_ALL_CERTS.get(0))
                                    .build();

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.setSubtypeResolver(new JsonSubtypeResolver(objectMapper.getSubtypeResolver()));
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(baseUrl)
                            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                            .build();

    return retrofit.create(CVNGServiceClient.class);
  }

  @NotNull
  private Interceptor getAuthorizationInterceptor() {
    final Supplier<String> secretKeySupplier = this::getServiceSecret;
    return chain -> {
      String token = tokenGenerator.getServiceToken(secretKeySupplier.get());
      Request request = chain.request();
      return chain.proceed(request.newBuilder().header("Authorization", CLIENT_ID + StringUtils.SPACE + token).build());
    };
  }

  private String getServiceSecret() {
    if (StringUtils.isNotBlank(cvNgServiceSecret)) {
      return cvNgServiceSecret.trim();
    }
    throw new InvalidRequestException("No secret key for client for " + CLIENT_ID);
  }
}
