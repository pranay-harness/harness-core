/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logserviceclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.beans.entities.LogServiceConfig;
import io.harness.network.Http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@OwnedBy(HarnessTeam.CI)
public class CILogServiceClientFactory implements Provider<CILogServiceClient> {
  private LogServiceConfig logConfig;

  @Inject
  public CILogServiceClientFactory(LogServiceConfig logConfig) {
    this.logConfig = logConfig;
  }

  @Override
  public CILogServiceClient get() {
    Gson gson = new GsonBuilder().setLenient().create();
    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(logConfig.getBaseUrl())
                            .client(Http.getUnsafeOkHttpClient(logConfig.getBaseUrl()))
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();
    return retrofit.create(CILogServiceClient.class);
  }
}
