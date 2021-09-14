/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.client;

import static io.harness.ng.core.CorrelationContext.getCorrelationIdInterceptor;
import static io.harness.request.RequestContextFilter.getRequestContextInterceptor;

import io.harness.AuthorizationServiceHeader;
import io.harness.cvng.core.NGManagerServiceConfig;
import io.harness.exception.GeneralException;
import io.harness.network.Http;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.github.resilience4j.retrofit.CircuitBreakerCallAdapter;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
public class NextGenClientFactory extends AbstractHttpClientFactory implements Provider<NextGenClient> {
  private NGManagerServiceConfig ngManagerServiceConfig;
  private ClientMode clientMode;
  private final ObjectMapper objectMapper;

  public NextGenClientFactory(NGManagerServiceConfig ngManagerServiceConfig, ServiceTokenGenerator tokenGenerator) {
    super(ServiceHttpClientConfig.builder().baseUrl(ngManagerServiceConfig.getNgManagerUrl()).build(),
        ngManagerServiceConfig.getManagerServiceSecret(), tokenGenerator, null,
        AuthorizationServiceHeader.CV_NEXT_GEN.getServiceId(), true, ClientMode.PRIVILEGED);
    this.ngManagerServiceConfig = ngManagerServiceConfig;
    this.objectMapper = new ObjectMapper();
    NGObjectMapperHelper.configureNGObjectMapper(objectMapper);
    // TODO: this change is for the hotfix. We need to have 2 clients (Previleged and nonprevileged (For client
    // requests))
    this.clientMode = ClientMode.PRIVILEGED;
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
