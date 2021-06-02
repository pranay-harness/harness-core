package io.harness.cvng.client;

import static io.harness.ng.core.CorrelationContext.getCorrelationIdInterceptor;
import static io.harness.request.RequestContextFilter.getRequestContextInterceptor;

import io.harness.cvng.core.NGManagerServiceConfig;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.network.Http;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.security.ServiceTokenGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retrofit.CircuitBreakerCallAdapter;
import java.util.function.Supplier;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
public class NextGenClientFactory implements Provider<NextGenClient> {
  public static final String NG_MANAGER_CIRCUIT_BREAKER = "ng-manager";
  private static final String CLIENT_ID = "NextGenManager";
  private NGManagerServiceConfig ngManagerServiceConfig;
  private ServiceTokenGenerator tokenGenerator;
  private final ObjectMapper objectMapper;

  public NextGenClientFactory(NGManagerServiceConfig ngManagerServiceConfig, ServiceTokenGenerator tokenGenerator) {
    this.tokenGenerator = tokenGenerator;
    this.ngManagerServiceConfig = ngManagerServiceConfig;
    this.objectMapper = new ObjectMapper();
    NGObjectMapperHelper.configureNGObjectMapper(objectMapper);
  }

  private CircuitBreaker getCircuitBreaker() {
    return CircuitBreaker.ofDefaults(NG_MANAGER_CIRCUIT_BREAKER);
  }

  @Override
  public NextGenClient get() {
    String baseUrl = ngManagerServiceConfig.getNgManagerUrl();
    // https://resilience4j.readme.io/docs/retrofit
    final Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(getUnsafeOkHttpClient(baseUrl))
            .addCallAdapterFactory(CircuitBreakerCallAdapter.of(getCircuitBreaker(), response -> response.code() < 500))
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build();
    return retrofit.create(NextGenClient.class);
  }

  private OkHttpClient getUnsafeOkHttpClient(String baseUrl) {
    try {
      return Http.getUnsafeOkHttpClientBuilder(baseUrl, 60, 60)
          .connectionPool(new ConnectionPool())
          .retryOnConnectionFailure(false)
          .addInterceptor(getAuthorizationInterceptor())
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
    String managerServiceSecret = this.ngManagerServiceConfig.getManagerServiceSecret();
    if (StringUtils.isNotBlank(managerServiceSecret)) {
      return managerServiceSecret.trim();
    }
    throw new InvalidRequestException("No secret key for client for " + CLIENT_ID);
  }
}
