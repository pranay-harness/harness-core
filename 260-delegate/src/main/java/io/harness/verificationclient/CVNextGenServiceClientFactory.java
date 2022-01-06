/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.verificationclient;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.managerclient.DelegateAgentManagerClientX509TrustManager;
import io.harness.managerclient.DelegateAuthInterceptor;
import io.harness.network.Http;
import io.harness.network.NoopHostnameVerifier;
import io.harness.security.TokenGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provider;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@TargetModule(HarnessModule._420_DELEGATE_AGENT)
@OwnedBy(HarnessTeam.CV)
public class CVNextGenServiceClientFactory implements Provider<CVNextGenServiceClient> {
  public static final ImmutableList<TrustManager> TRUST_ALL_CERTS =
      ImmutableList.of(new DelegateAgentManagerClientX509TrustManager());
  private static final ConnectionPool CONNECTION_POOL = new ConnectionPool(0, 10, TimeUnit.MINUTES);

  private String baseUrl;
  private TokenGenerator tokenGenerator;

  public CVNextGenServiceClientFactory(String baseUrl, TokenGenerator delegateTokenGenerator) {
    this.baseUrl = baseUrl;
    this.tokenGenerator = delegateTokenGenerator;
  }

  @Override
  public CVNextGenServiceClient get() {
    if (isEmpty(baseUrl)) {
      return null;
    }
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new JavaTimeModule());
    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .client(getUnsafeOkHttpClient())
                            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                            .build();
    return retrofit.create(CVNextGenServiceClient.class);
  }

  private OkHttpClient getUnsafeOkHttpClient() {
    try {
      // Install the all-trusting trust manager
      final SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, TRUST_ALL_CERTS.toArray(new TrustManager[1]), new java.security.SecureRandom());
      // Create an ssl socket factory with our all-trusting manager
      final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
      // https://www.baeldung.com/okhttp-timeouts
      return Http.getOkHttpClientWithProxyAuthSetup()
          .connectionPool(CONNECTION_POOL)
          .connectTimeout(10, TimeUnit.SECONDS)
          .readTimeout(10, TimeUnit.SECONDS)
          .writeTimeout(10, TimeUnit.SECONDS) //.callTimeout(60, TimeUnit.SECONDS) // Call timeout is available in 3.12
          .retryOnConnectionFailure(false)
          .addInterceptor(new DelegateAuthInterceptor(tokenGenerator))
          .sslSocketFactory(sslSocketFactory, (X509TrustManager) TRUST_ALL_CERTS.get(0))
          .hostnameVerifier(new NoopHostnameVerifier())
          .build();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
