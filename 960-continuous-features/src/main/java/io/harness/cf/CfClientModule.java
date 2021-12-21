package io.harness.cf;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.harness.cf.client.api.CfClient;
import io.harness.cf.client.api.Config;
import io.harness.cf.client.api.FeatureFlagInitializeException;
import io.harness.cf.openapi.ApiClient;
import lombok.extern.slf4j.Slf4j;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

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

    final Config config = Config.builder()
                        .analyticsEnabled(cfClientConfig.isAnalyticsEnabled())
                        .configUrl(cfClientConfig.getConfigUrl())
                        .eventUrl(cfClientConfig.getEventUrl())
                        .readTimeout(cfClientConfig.getReadTimeout())
                        .connectionTimeout(cfClientConfig.getConnectionTimeout())
                        .build();

    final CfClient client = new CfClient(apiKey, config);

    log.info("CF client is initializing");
    try {

      client.waitForInitialization();

      log.error("CF client is initialized");

    } catch (InterruptedException | FeatureFlagInitializeException e) {

      log.error("CF client was not initialized", e);
    }

    return client;
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
