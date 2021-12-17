package io.harness.cf;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.cf.client.api.CfClient;
import io.harness.cf.client.api.Config;
import io.harness.cf.client.dto.Target;
import io.harness.cf.openapi.ApiClient;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.TimeUnit;
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

    CfClient client = new CfClient(apiKey, config);
    waitForInitialization(client, cfClientConfig.getRetries(), cfClientConfig.getSleepInterval());
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

  private void waitForInitialization(CfClient client, Integer retry, Integer sleepInterval) {
    while (retry > 0) {
      try {
        if (client.isInitialized() == true) {
          log.info("CF Client successfully initialized");
          return;
        }
        TimeUnit.MILLISECONDS.sleep(sleepInterval);
        retry--;
      } catch (InterruptedException e) {
        log.error("interrupted while waiting for initialization {}", e.getMessage());
      }
    }
    log.error("The CF Client failed to initialize");
  }
}
