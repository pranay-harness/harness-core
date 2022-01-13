/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.logstreaming;

import io.harness.network.Http;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class LogStreamingClientFactory implements Provider<LogStreamingClient> {
  @Inject private KryoConverterFactory kryoConverterFactory;

  private String logStreamingServiceBaseUrl;

  public LogStreamingClientFactory(String logStreamingServiceBaseUrl) {
    this.logStreamingServiceBaseUrl = logStreamingServiceBaseUrl;
  }

  @Override
  public LogStreamingClient get() {
    if (StringUtils.isBlank(logStreamingServiceBaseUrl)) {
      return null;
    }

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    Retrofit retrofit = new Retrofit.Builder()
                            .client(getSafeOkHttpClient())
                            .baseUrl(logStreamingServiceBaseUrl)
                            .addConverterFactory(kryoConverterFactory)
                            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                            .build();

    return retrofit.create(LogStreamingClient.class);
  }

  private OkHttpClient getSafeOkHttpClient() {
    try {
      KeyStore keyStore = getKeyStore();

      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(keyStore);
      TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustManagers, null);

      return Http.getOkHttpClientWithProxyAuthSetup()
          .connectionPool(new ConnectionPool())
          .connectTimeout(5, TimeUnit.SECONDS)
          .readTimeout(10, TimeUnit.SECONDS)
          .retryOnConnectionFailure(true)
          .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0])
          .build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private KeyStore getKeyStore() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    keyStore.load(null, null);

    // Load self-signed certificate created only for the purpose of local development
    try (InputStream certInputStream = getClass().getClassLoader().getResourceAsStream("localhost.pem")) {
      keyStore.setCertificateEntry(
          "localhost", (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(certInputStream));
    }

    // Load all trusted issuers from default java trust store
    TrustManagerFactory defaultTrustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    defaultTrustManagerFactory.init((KeyStore) null);
    for (TrustManager trustManager : defaultTrustManagerFactory.getTrustManagers()) {
      if (trustManager instanceof X509TrustManager) {
        for (X509Certificate acceptedIssuer : ((X509TrustManager) trustManager).getAcceptedIssuers()) {
          keyStore.setCertificateEntry(acceptedIssuer.getSubjectDN().getName(), acceptedIssuer);
        }
      }
    }

    return keyStore;
  }
}
