package io.harness.cvng.client;

import static io.harness.ng.core.CorrelationContext.getCorrelationIdInterceptor;
import static io.harness.request.RequestContextFilter.getRequestContextInterceptor;

import io.harness.exception.GeneralException;
import io.harness.network.Http;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;
import io.github.resilience4j.retrofit.CircuitBreakerCallAdapter;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class CVNGClientFactory extends AbstractHttpClientFactory implements Provider<CVNGClient> {
  private ServiceHttpClientConfig serviceHttpClientConfig;
  private String serviceSecret;
  private ClientMode clientMode;
  private final ObjectMapper objectMapper;

  public CVNGClientFactory(ServiceHttpClientConfig serviceHttpClientConfig, String serviceSecret, String serviceId,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory) {
    super(serviceHttpClientConfig, serviceSecret, tokenGenerator, kryoConverterFactory, serviceId, true,
        ClientMode.PRIVILEGED);
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.objectMapper = new ObjectMapper();
    NGObjectMapperHelper.configureNGObjectMapper(objectMapper);
    this.clientMode = ClientMode.PRIVILEGED;
  }

  @Override
  public CVNGClient get() {
    String baseUrl = serviceHttpClientConfig.getBaseUrl();
    // https://resilience4j.readme.io/docs/retrofit
    final Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(getUnsafeOkHttpClient(baseUrl))
            .addCallAdapterFactory(CircuitBreakerCallAdapter.of(getCircuitBreaker(), response -> response.code() < 500))
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build();
    return retrofit.create(CVNGClient.class);
  }

  private OkHttpClient getUnsafeOkHttpClient(String baseUrl) {
    try {
      return Http.getUnsafeOkHttpClientBuilder(baseUrl, 60, 60)
          .connectionPool(new ConnectionPool())
          .retryOnConnectionFailure(false)
          .addInterceptor(getAuthorizationInterceptor(clientMode))
          .addInterceptor(getCorrelationIdInterceptor())
          .addInterceptor(getRequestContextInterceptor())
          .addInterceptor(chain -> {
            Request original = chain.request();

            // Request customization: add connection close headers
            Request.Builder requestBuilder = original.newBuilder().header("Connection", "close");

            Request request = requestBuilder.build();
            return chain.proceed(request);
          })
          .build();
    } catch (Exception e) {
      throw new GeneralException("error while creating okhttp client for Command library service", e);
    }
  }
}
