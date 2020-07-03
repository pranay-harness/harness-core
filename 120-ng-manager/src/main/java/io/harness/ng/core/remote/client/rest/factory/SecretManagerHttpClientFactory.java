package io.harness.ng.core.remote.client.rest.factory;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.network.Http;
import io.harness.ng.core.SecretManagerClientConfig;
import io.harness.ng.core.remote.client.rest.SecretManagerClient;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.JsonSubtypeResolver;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.jersey.JsonViews;

import java.util.function.Supplier;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
public class SecretManagerHttpClientFactory implements Provider<SecretManagerClient> {
  private final SecretManagerClientConfig secretManagerConfig;
  private final String serviceSecret;
  private final ServiceTokenGenerator tokenGenerator;
  private static final String CLIENT_ID = "NextGenManager";

  public SecretManagerHttpClientFactory(
      SecretManagerClientConfig secretManagerConfig, String serviceSecret, ServiceTokenGenerator tokenGenerator) {
    this.secretManagerConfig = secretManagerConfig;
    this.serviceSecret = serviceSecret;
    this.tokenGenerator = tokenGenerator;
  }

  @Override
  public SecretManagerClient get() {
    String baseUrl = secretManagerConfig.getBaseUrl();
    ObjectMapper objectMapper = getObjectMapper();
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(baseUrl)
                                  .client(getUnsafeOkHttpClient(baseUrl))
                                  .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                                  .build();
    return retrofit.create(SecretManagerClient.class);
  }

  private ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSubtypeResolver(new JsonSubtypeResolver(objectMapper.getSubtypeResolver()));
    objectMapper.setConfig(objectMapper.getSerializationConfig().withView(JsonViews.Public.class));
    objectMapper.disable(FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  private OkHttpClient getUnsafeOkHttpClient(String baseUrl) {
    try {
      return Http
          .getUnsafeOkHttpClientBuilder(
              baseUrl, secretManagerConfig.getConnectTimeOutSeconds(), secretManagerConfig.getReadTimeOutSeconds())
          .connectionPool(new ConnectionPool())
          .retryOnConnectionFailure(true)
          .addInterceptor(getAuthorizationInterceptor())
          .build();
    } catch (Exception e) {
      throw new GeneralException("error while creating okhttp client for Command library service", e);
    }
  }

  @NotNull
  private Interceptor getAuthorizationInterceptor() {
    final Supplier<String> secretKeySupplier = this ::getServiceSecret;
    return chain -> {
      String token = tokenGenerator.getServiceToken(secretKeySupplier.get());
      Request request = chain.request();
      return chain.proceed(request.newBuilder().header("Authorization", CLIENT_ID + StringUtils.SPACE + token).build());
    };
  }

  private String getServiceSecret() {
    String managerServiceSecret = this.serviceSecret;
    if (StringUtils.isNotBlank(managerServiceSecret)) {
      return managerServiceSecret.trim();
    }
    throw new InvalidRequestException("No secret key for client for " + CLIENT_ID);
  }
}
