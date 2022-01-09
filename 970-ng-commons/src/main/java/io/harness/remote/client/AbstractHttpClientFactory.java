/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.remote.client;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.CorrelationContext.getCorrelationIdInterceptor;
import static io.harness.request.RequestContextFilter.getRequestContextInterceptor;
import static io.harness.security.JWTAuthenticationFilter.X_SOURCE_PRINCIPAL;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.apache.http.HttpHeaders.AUTHORIZATION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContextData;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.manage.GlobalContextManager;
import io.harness.network.Http;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.ServiceTokenGenerator;
import io.harness.security.SourcePrincipalContextBuilder;
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
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import javax.validation.constraints.NotNull;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@OwnedBy(PL)
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

  protected AbstractHttpClientFactory(ServiceHttpClientConfig secretManagerConfig, String serviceSecret,
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
    /*
    .baseUrl(baseUrl)
    .addConverterFactory(kryoConverterFactory)
    .client(getUnsafeOkHttpClient(baseUrl))
    .addCallAdapterFactory(CircuitBreakerCallAdapter.of(getCircuitBreaker()))
    .addConverterFactory(JacksonConverterFactory.create(objectMapper))
    .build();

     Order of factories of a particular type is important while creating the builder, please do not change the order
     */
    String baseUrl = serviceHttpClientConfig.getBaseUrl();
    Retrofit.Builder retrofitBuilder = new Retrofit.Builder().baseUrl(baseUrl);
    if (this.kryoConverterFactory != null) {
      retrofitBuilder.addConverterFactory(kryoConverterFactory);
    }
    retrofitBuilder.client(getUnsafeOkHttpClient(
        baseUrl, this.clientMode, Boolean.TRUE.equals(this.serviceHttpClientConfig.getEnableHttpLogging())));
    if (this.enableCircuitBreaker) {
      retrofitBuilder.addCallAdapterFactory(CircuitBreakerCallAdapter.of(getCircuitBreaker()));
    }
    retrofitBuilder.addConverterFactory(JacksonConverterFactory.create(objectMapper));

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

  protected OkHttpClient getUnsafeOkHttpClient(String baseUrl, ClientMode clientMode, boolean addHttpLogging) {
    try {
      OkHttpClient.Builder builder =
          Http.getUnsafeOkHttpClientBuilder(baseUrl, serviceHttpClientConfig.getConnectTimeOutSeconds(),
                  serviceHttpClientConfig.getReadTimeOutSeconds())
              .connectionPool(new ConnectionPool())
              .retryOnConnectionFailure(true)
              .addInterceptor(getAuthorizationInterceptor(clientMode))
              .addInterceptor(getCorrelationIdInterceptor())
              .addInterceptor(getGitContextInterceptor())
              .addInterceptor(getRequestContextInterceptor());
      if (addHttpLogging) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        builder.addInterceptor(loggingInterceptor);
      }
      builder.addInterceptor(chain -> {
        Request original = chain.request();

        // Request customization: add connection close headers
        Request.Builder requestBuilder = original.newBuilder().header("Connection", "close");

        Request request = requestBuilder.build();
        return chain.proceed(request);
      });
      return builder.build();
    } catch (Exception e) {
      throw new GeneralException(String.format("error while creating okhttp client for %s service", clientId), e);
    }
  }

  @NotNull
  protected Interceptor getGitContextInterceptor() {
    return chain -> {
      Request request = chain.request();
      GlobalContextData globalContextData = GlobalContextManager.get(GitSyncBranchContext.NG_GIT_SYNC_CONTEXT);

      if (globalContextData != null) {
        final GitEntityInfo gitBranchInfo =
            ((GitSyncBranchContext) Objects.requireNonNull(globalContextData)).getGitBranchInfo();
        if (gitBranchInfo != null && gitBranchInfo.getYamlGitConfigId() != null && gitBranchInfo.getBranch() != null) {
          HttpUrl url = request.url()
                            .newBuilder()
                            .addQueryParameter("repoIdentifier", gitBranchInfo.getYamlGitConfigId())
                            .addQueryParameter("branch", gitBranchInfo.getBranch())
                            .addQueryParameter(
                                "getDefaultFromOtherRepo", String.valueOf(gitBranchInfo.isFindDefaultFromOtherRepos()))
                            .build();
          return chain.proceed(request.newBuilder().url(url).build());
        } else {
          return chain.proceed(request);
        }
      } else {
        return chain.proceed(request);
      }
    };
  }

  @NotNull
  protected Interceptor getAuthorizationInterceptor(ClientMode clientMode) {
    final Supplier<String> secretKeySupplier = this::getServiceSecret;
    return chain -> {
      Request.Builder builder = chain.request().newBuilder();
      String authorizationToken;
      if (ClientMode.PRIVILEGED == clientMode) {
        authorizationToken = tokenGenerator.getServiceTokenWithDuration(
            secretKeySupplier.get(), Duration.ofHours(4), new ServicePrincipal(this.clientId));
      } else {
        authorizationToken = tokenGenerator.getServiceTokenWithDuration(
            secretKeySupplier.get(), Duration.ofHours(4), SecurityContextBuilder.getPrincipal());
      }
      Principal sourcePrincipal = SourcePrincipalContextBuilder.getSourcePrincipal() != null
          ? SourcePrincipalContextBuilder.getSourcePrincipal()
          : SecurityContextBuilder.getPrincipal();
      String sourcePrincipalToken =
          tokenGenerator.getServiceTokenWithDuration(secretKeySupplier.get(), Duration.ofHours(4), sourcePrincipal);
      builder.header(X_SOURCE_PRINCIPAL, clientId + StringUtils.SPACE + sourcePrincipalToken);
      builder.header(AUTHORIZATION, clientId + StringUtils.SPACE + authorizationToken);
      return chain.proceed(builder.build());
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
