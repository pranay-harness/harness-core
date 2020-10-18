package io.harness.entitysetupusageclient.remote;

import static io.harness.annotations.dev.HarnessTeam.DX;

import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entitysetupusageclient.NGManagerClientConfig;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.network.Http;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.function.Supplier;
import javax.validation.constraints.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
@OwnedBy(DX)
public class EntitySetupUsageHttpClientFactory implements Provider<EntitySetupUsageClient> {
  public static final String NG_MANAGER_CIRCUIT_BREAKER = "ng-manager";
  private final NGManagerClientConfig ngManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;
  private final ServiceTokenGenerator tokenGenerator;
  private final KryoConverterFactory kryoConverterFactory;

  public EntitySetupUsageHttpClientFactory(NGManagerClientConfig ngManagerClientConfig, String serviceSecret,
      String clientId, ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory) {
    this.ngManagerClientConfig = ngManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
    this.tokenGenerator = tokenGenerator;
    this.kryoConverterFactory = kryoConverterFactory;
  }

  @Override
  public EntitySetupUsageClient get() {
    String baseUrl = ngManagerClientConfig.getBaseUrl();
    ObjectMapper objectMapper = getObjectMapper();
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(baseUrl)
                                  .addConverterFactory(kryoConverterFactory)
                                  .client(getUnsafeOkHttpClient(baseUrl))
                                  .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                                  .build();
    return retrofit.create(EntitySetupUsageClient.class);
  }

  private ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    NGObjectMapperHelper.configureNGObjectMapper(objectMapper);
    return objectMapper;
  }

  private OkHttpClient getUnsafeOkHttpClient(String baseUrl) {
    try {
      return Http
          .getUnsafeOkHttpClientBuilder(
              baseUrl, ngManagerClientConfig.getConnectTimeOutSeconds(), ngManagerClientConfig.getReadTimeOutSeconds())
          .connectionPool(new ConnectionPool())
          .retryOnConnectionFailure(false)
          .addInterceptor(getAuthorizationInterceptor())
          .build();
    } catch (Exception e) {
      throw new GeneralException("error while creating okhttp client for organization service", e);
    }
  }

  @NotNull
  private Interceptor getAuthorizationInterceptor() {
    final Supplier<String> secretKeySupplier = this ::getServiceSecret;
    return chain -> {
      String token = tokenGenerator.getServiceToken(secretKeySupplier.get());
      Request request = chain.request();
      return chain.proceed(request.newBuilder().header("Authorization", clientId + StringUtils.SPACE + token).build());
    };
  }

  private String getServiceSecret() {
    String managerServiceSecret = this.serviceSecret;
    if (StringUtils.isNotBlank(managerServiceSecret)) {
      return managerServiceSecret.trim();
    }
    throw new InvalidRequestException("No secret key for client for " + clientId);
  }
}
