package io.harness.remote.client;

import static io.harness.ng.core.CorrelationContext.getCorrelationIdInterceptor;
import static io.harness.request.RequestContextFilter.getRequestContextInterceptor;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.network.Http;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.ServiceTokenGenerator;
import io.harness.security.dto.Principal;
import io.harness.security.dto.ServicePrincipal;
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
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String serviceSecret;
  private final ServiceTokenGenerator tokenGenerator;
  private final KryoConverterFactory kryoConverterFactory;
  private final String clientId;
  private final ObjectMapper objectMapper;
  private final boolean enableCircuitBreaker;
  private final ClientMode clientMode;

  protected AbstractHttpClientFactory(ServiceHttpClientConfig secretManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    this.serviceHttpClientConfig = secretManagerConfig;
    this.serviceSecret = serviceSecret;
    this.tokenGenerator = tokenGenerator;
    this.kryoConverterFactory = kryoConverterFactory;
    this.clientId = clientId;
    this.objectMapper = getObjectMapper();
    this.enableCircuitBreaker = false;
    this.clientMode = ClientMode.NON_PRIVILEGED;
  }

  public AbstractHttpClientFactory(ServiceHttpClientConfig secretManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId,
      boolean enableCircuitBreaker, ClientMode clientMode) {
    this.serviceHttpClientConfig = secretManagerConfig;
    this.serviceSecret = serviceSecret;
    this.tokenGenerator = tokenGenerator;
    this.kryoConverterFactory = kryoConverterFactory;
    this.clientId = clientId;
    this.objectMapper = getObjectMapper();
    this.enableCircuitBreaker = enableCircuitBreaker;
    this.clientMode = clientMode;
  }

  protected Retrofit getRetrofit() {
    String baseUrl = serviceHttpClientConfig.getBaseUrl();
    Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                                           .baseUrl(baseUrl)
                                           .addConverterFactory(kryoConverterFactory)
                                           .client(getUnsafeOkHttpClient(baseUrl, this.clientMode))
                                           .addConverterFactory(JacksonConverterFactory.create(objectMapper));
    if (this.enableCircuitBreaker) {
      retrofitBuilder.addCallAdapterFactory(CircuitBreakerCallAdapter.of(getCircuitBreaker()));
    }
    return retrofitBuilder.build();
  }

  protected CircuitBreaker getCircuitBreaker() {
    return CircuitBreaker.ofDefaults(this.clientId);
  }

  protected ObjectMapper getObjectMapper() {
    ObjectMapper objMapper = new ObjectMapper();
    objMapper.setSubtypeResolver(new JsonSubtypeResolver(objMapper.getSubtypeResolver()));
    objMapper.setConfig(objMapper.getSerializationConfig().withView(JsonViews.Public.class));
    objMapper.disable(FAIL_ON_UNKNOWN_PROPERTIES);
    objMapper.registerModule(new ProtobufModule());
    objMapper.registerModule(new Jdk8Module());
    objMapper.registerModule(new GuavaModule());
    objMapper.registerModule(new JavaTimeModule());
    return objMapper;
  }

  protected OkHttpClient getUnsafeOkHttpClient(String baseUrl, ClientMode clientMode) {
    try {
      return Http
          .getUnsafeOkHttpClientBuilder(baseUrl, serviceHttpClientConfig.getConnectTimeOutSeconds(),
              serviceHttpClientConfig.getReadTimeOutSeconds())
          .connectionPool(new ConnectionPool())
          .retryOnConnectionFailure(true)
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
      throw new GeneralException(String.format("error while creating okhttp client for %s service", clientId), e);
    }
  }

  @NotNull
  protected Interceptor getAuthorizationInterceptor(ClientMode clientMode) {
    final Supplier<String> secretKeySupplier = this::getServiceSecret;
    return chain -> {
      Principal principal = SecurityContextBuilder.getPrincipal();
      if (ClientMode.PRIVILEGED == clientMode) {
        SecurityContextBuilder.unsetPrincipalContext();
        SecurityContextBuilder.setContext(new ServicePrincipal(clientId));
      }
      String token = tokenGenerator.getServiceToken(secretKeySupplier.get());
      SecurityContextBuilder.unsetPrincipalContext();
      SecurityContextBuilder.setContext(principal);
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
