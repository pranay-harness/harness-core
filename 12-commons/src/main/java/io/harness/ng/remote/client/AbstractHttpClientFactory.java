package io.harness.ng.remote.client;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retrofit.CircuitBreakerCallAdapter;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.network.Http;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.JsonSubtypeResolver;
import io.harness.serializer.kryo.KryoConverterFactory;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.jersey.JsonViews;

import java.util.function.Supplier;
import javax.validation.constraints.NotNull;

public abstract class AbstractHttpClientFactory {
  public static final String NG_MANAGER_CIRCUIT_BREAKER = "ng-manager";
  private final ServiceHttpClientConfig secretManagerConfig;
  private final String serviceSecret;
  private final ServiceTokenGenerator tokenGenerator;
  private final KryoConverterFactory kryoConverterFactory;
  private static final String CLIENT_ID = "NextGenManager";

  public AbstractHttpClientFactory(ServiceHttpClientConfig secretManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory) {
    this.secretManagerConfig = secretManagerConfig;
    this.serviceSecret = serviceSecret;
    this.tokenGenerator = tokenGenerator;
    this.kryoConverterFactory = kryoConverterFactory;
  }

  protected Retrofit getRetrofit() {
    String baseUrl = secretManagerConfig.getBaseUrl();
    ObjectMapper objectMapper = getObjectMapper();
    return new Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(kryoConverterFactory)
        .client(getUnsafeOkHttpClient(baseUrl))
        .addCallAdapterFactory(CircuitBreakerCallAdapter.of(getCircuitBreaker()))
        .addConverterFactory(JacksonConverterFactory.create(objectMapper))
        .build();
  }

  protected CircuitBreaker getCircuitBreaker() {
    return CircuitBreaker.ofDefaults(NG_MANAGER_CIRCUIT_BREAKER);
  }

  protected ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSubtypeResolver(new JsonSubtypeResolver(objectMapper.getSubtypeResolver()));
    objectMapper.setConfig(objectMapper.getSerializationConfig().withView(JsonViews.Public.class));
    objectMapper.disable(FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  protected OkHttpClient getUnsafeOkHttpClient(String baseUrl) {
    try {
      return Http
          .getUnsafeOkHttpClientBuilder(
              baseUrl, secretManagerConfig.getConnectTimeOutSeconds(), secretManagerConfig.getReadTimeOutSeconds())
          .connectionPool(new ConnectionPool())
          .retryOnConnectionFailure(true)
          .addInterceptor(getAuthorizationInterceptor())
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

  @NotNull
  protected Interceptor getAuthorizationInterceptor() {
    final Supplier<String> secretKeySupplier = this ::getServiceSecret;
    return chain -> {
      String token = tokenGenerator.getServiceToken(secretKeySupplier.get());
      Request request = chain.request();
      return chain.proceed(request.newBuilder().header("Authorization", CLIENT_ID + StringUtils.SPACE + token).build());
    };
  }

  protected String getServiceSecret() {
    String managerServiceSecret = this.serviceSecret;
    if (StringUtils.isNotBlank(managerServiceSecret)) {
      return managerServiceSecret.trim();
    }
    throw new InvalidRequestException("No secret key for client for " + CLIENT_ID);
  }
}
