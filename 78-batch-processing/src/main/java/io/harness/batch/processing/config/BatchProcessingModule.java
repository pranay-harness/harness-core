package io.harness.batch.processing.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.mongo.MongoConfig;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.instance.CloudToHarnessMappingServiceImpl;
import software.wings.service.impl.instance.DeploymentServiceImpl;
import software.wings.service.impl.security.NoOpSecretManagerImpl;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.service.intfc.instance.DeploymentService;
import software.wings.service.intfc.security.SecretManager;

@Slf4j
public class BatchProcessingModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(SecretManager.class).to(NoOpSecretManagerImpl.class);
    bind(HPersistence.class).to(WingsMongoPersistence.class);
    bind(WingsPersistence.class).to(WingsMongoPersistence.class);
    bind(DeploymentService.class).to(DeploymentServiceImpl.class);
    bind(CloudToHarnessMappingService.class).to(CloudToHarnessMappingServiceImpl.class);
  }

  @Provides
  @Singleton
  MongoConfig mongoConfig(BatchMainConfig batchMainConfig) {
    return batchMainConfig.getHarnessMongo();
  }
}
