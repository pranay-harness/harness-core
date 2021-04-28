package io.harness.batch.processing.config;

import static io.harness.lock.DistributedLockImplementation.MONGO;

import io.harness.batch.processing.metrics.CeCloudMetricsService;
import io.harness.batch.processing.metrics.CeCloudMetricsServiceImpl;
import io.harness.batch.processing.metrics.ProductMetricsService;
import io.harness.batch.processing.metrics.ProductMetricsServiceImpl;
import io.harness.ccm.anomaly.service.impl.AnomalyServiceImpl;
import io.harness.ccm.anomaly.service.itfc.AnomalyService;
import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.billing.bigquery.BigQueryServiceImpl;
import io.harness.ccm.communication.CESlackWebhookService;
import io.harness.ccm.communication.CESlackWebhookServiceImpl;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.ccm.views.service.ViewsBillingService;
import io.harness.ccm.views.service.impl.CEViewServiceImpl;
import io.harness.ccm.views.service.impl.ViewCustomFieldServiceImpl;
import io.harness.ccm.views.service.impl.ViewsBillingServiceImpl;
import io.harness.ff.FeatureFlagModule;
import io.harness.lock.DistributedLockImplementation;
import io.harness.mongo.MongoConfig;
import io.harness.persistence.HPersistence;
import io.harness.redis.RedisConfig;

import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ce.CeAccountExpirationCheckerImpl;
import software.wings.service.impl.instance.CloudToHarnessMappingServiceImpl;
import software.wings.service.impl.instance.DeploymentServiceImpl;
import software.wings.service.impl.security.NoOpSecretManagerImpl;
import software.wings.service.intfc.ce.CeAccountExpirationChecker;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.service.intfc.instance.DeploymentService;
import software.wings.service.intfc.security.SecretManager;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BatchProcessingModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(SecretManager.class).to(NoOpSecretManagerImpl.class);
    bind(HPersistence.class).to(WingsMongoPersistence.class);
    bind(WingsPersistence.class).to(WingsMongoPersistence.class);
    bind(DeploymentService.class).to(DeploymentServiceImpl.class);
    bind(CloudToHarnessMappingService.class).to(CloudToHarnessMappingServiceImpl.class);
    bind(ProductMetricsService.class).to(ProductMetricsServiceImpl.class);
    bind(CESlackWebhookService.class).to(CESlackWebhookServiceImpl.class);
    bind(BigQueryService.class).to(BigQueryServiceImpl.class);
    bind(CeCloudMetricsService.class).to(CeCloudMetricsServiceImpl.class);
    bind(CEViewService.class).to(CEViewServiceImpl.class);
    bind(ViewsBillingService.class).to(ViewsBillingServiceImpl.class);
    bind(ViewCustomFieldService.class).to(ViewCustomFieldServiceImpl.class);
    bind(CeAccountExpirationChecker.class).to(CeAccountExpirationCheckerImpl.class);
    bind(AnomalyService.class).to(AnomalyServiceImpl.class);
    install(FeatureFlagModule.getInstance());
    bind(TimeLimiter.class).toInstance(new SimpleTimeLimiter());
  }

  @Provides
  @Singleton
  MongoConfig mongoConfig(BatchMainConfig batchMainConfig) {
    return batchMainConfig.getHarnessMongo();
  }

  @Provides
  @Singleton
  DistributedLockImplementation distributedLockImplementation() {
    return MONGO;
  }

  @Provides
  @Named("lock")
  @Singleton
  RedisConfig redisConfig() {
    return RedisConfig.builder().build();
  }
}
