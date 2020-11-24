package io.harness.remote.client;

import static io.harness.ng.core.CorrelationContext.getCorrelationIdInterceptor;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.network.Http;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.JsonSubtypeResolver;
import io.harness.serializer.kryo.KryoConverterFactory;

import software.wings.jersey.JsonViews;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retrofit.CircuitBreakerCallAdapter;
import java.util.function.Supplier;
import javax.validation.constraints.NotNull;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public abstract class AbstractHttpClientFactory {
  public static final String NG_MANAGER_CIRCUIT_BREAKER = "ng-manager";
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;
  private final ServiceTokenGenerator tokenGenerator;
  private final KryoConverterFactory kryoConverterFactory;
  private String clientId = "NextGenManager";

  public AbstractHttpClientFactory(ServiceHttpClientConfig secretManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    this.serviceHttpClientConfig = secretManagerConfig;
    this.serviceSecret = serviceSecret;
    this.tokenGenerator = tokenGenerator;
    this.kryoConverterFactory = kryoConverterFactory;
    this.clientId = clientId;
  }

  public AbstractHttpClientFactory(ServiceHttpClientConfig secretManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory) {
    this.serviceHttpClientConfig = secretManagerConfig;
    this.serviceSecret = serviceSecret;
    this.tokenGenerator = tokenGenerator;
    this.kryoConverterFactory = kryoConverterFactory;
  }

  protected Retrofit getRetrofit() {
    String baseUrl = serviceHttpClientConfig.getBaseUrl();
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
    objectMapper.registerModule(new ProtobufModule());
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  protected OkHttpClient getUnsafeOkHttpClient(String baseUrl) {
    try {
      return Http
          .getUnsafeOkHttpClientBuilder(baseUrl, serviceHttpClientConfig.getConnectTimeOutSeconds(),
              serviceHttpClientConfig.getReadTimeOutSeconds())
          .connectionPool(new ConnectionPool())
          .retryOnConnectionFailure(true)
          .addInterceptor(getAuthorizationInterceptor())
          .addInterceptor(getCorrelationIdInterceptor())
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
    final Supplier<String> secretKeySupplier = this::getServiceSecret;
    return chain -> {
      String token = tokenGenerator.getServiceToken(secretKeySupplier.get());
      Request request = chain.request();
      return chain.proceed(request.newBuilder().header("Authorization", clientId + StringUtils.SPACE + token).build());
    };
  }

  protected String getServiceSecret() {
    String managerServiceSecret = this.serviceSecret;
    if (StringUtils.isNotBlank(managerServiceSecret)) {
      return managerServiceSecret.trim();
    }
    throw new InvalidRequestException("No secret key for client for " + clientId);
  }
}
