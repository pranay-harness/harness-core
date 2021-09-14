/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cf;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.cf.client.api.CfClient;
import io.harness.cf.client.api.Config;
import io.harness.cf.openapi.ApiClient;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CfClientModule extends AbstractModule {
  private static volatile CfClientModule instance;

  public static CfClientModule getInstance() {
    if (instance == null) {
      instance = new CfClientModule();
    }
    return instance;
  }

  private CfClientModule() {}

  @Provides
  @Singleton
  CfClient provideCfClient(CfClientConfig cfClientConfig) {
    log.info("Using CF API key {}", cfClientConfig.getApiKey());
    String apiKey = cfClientConfig.getApiKey();
    if (isEmpty(apiKey)) {
      apiKey = "fake";
    }

    Config config = Config.builder()
                        .analyticsEnabled(cfClientConfig.isAnalyticsEnabled())
                        .configUrl(cfClientConfig.getConfigUrl())
                        .eventUrl(cfClientConfig.getEventUrl())
                        .readTimeout(cfClientConfig.getReadTimeout())
                        .connectionTimeout(cfClientConfig.getConnectionTimeout())
                        .build();

    return new CfClient(apiKey, config);
  }

  @Provides
  @Singleton
  @Named("cfMigrationAPI")
  CFApi providesCfAPI(CfMigrationConfig migrationConfig) {
    ApiClient apiClient = new ApiClient();
    apiClient.setReadTimeout(migrationConfig.getReadTimeout());
    apiClient.setConnectTimeout(migrationConfig.getConnectionTimeout());
    apiClient.setBasePath(migrationConfig.getAdminUrl());
    return new CFApi(apiClient);
  }
}
