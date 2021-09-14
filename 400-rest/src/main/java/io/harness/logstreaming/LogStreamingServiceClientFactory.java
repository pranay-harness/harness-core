/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logstreaming;

import static io.harness.network.Http.getOkHttpClientBuilder;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.network.Http;

import software.wings.app.MainConfiguration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Slf4j
@TargetModule(HarnessModule._980_COMMONS)
@BreakDependencyOn("software.wings.app.MainConfiguration")
public class LogStreamingServiceClientFactory implements Provider<LogStreamingServiceRestClient> {
  @Inject private MainConfiguration mainConfiguration;

  @Override
  public LogStreamingServiceRestClient get() {
    String url = mainConfiguration.getLogStreamingServiceConfig().getBaseUrl();
    OkHttpClient okHttpClient = getOkHttpClientBuilder()
                                    .connectTimeout(5, TimeUnit.SECONDS)
                                    .readTimeout(10, TimeUnit.SECONDS)
                                    .proxy(Http.checkAndGetNonProxyIfApplicable(url))
                                    .retryOnConnectionFailure(true)
                                    .build();

    Gson gson = new GsonBuilder().setLenient().create();

    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(url)
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();

    return retrofit.create(LogStreamingServiceRestClient.class);
  }
}
