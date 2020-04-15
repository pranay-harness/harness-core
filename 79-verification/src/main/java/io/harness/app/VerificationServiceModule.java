package io.harness.app;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.service.ContinuousVerificationServiceImpl;
import io.harness.service.LearningEngineAnalysisServiceImpl;
import io.harness.service.LogAnalysisServiceImpl;
import io.harness.service.NoOpAlertService;
import io.harness.service.NoOpCvValidationServiceImpl;
import io.harness.service.NoOpYamlPushService;
import io.harness.service.TimeSeriesAnalysisServiceImpl;
import io.harness.service.VerificationMigrationServiceImpl;
import io.harness.service.intfc.ContinuousVerificationService;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import io.harness.threading.ThreadPool;
import io.harness.version.VersionInfoManager;
import org.apache.commons.io.IOUtils;
import software.wings.DataStorageMode;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.FeatureFlagServiceImpl;
import software.wings.service.impl.GoogleDataStoreServiceImpl;
import software.wings.service.impl.MongoDataStoreServiceImpl;
import software.wings.service.impl.analysis.VerificationServiceImpl;
import software.wings.service.impl.datadog.DatadogServiceImpl;
import software.wings.service.impl.security.NoOpSecretManagerImpl;
import software.wings.service.impl.verification.CVActivityLogServiceImpl;
import software.wings.service.impl.verification.CVConfigurationServiceImpl;
import software.wings.service.impl.verification.CVTaskServiceImpl;
import software.wings.service.impl.verification.CvValidationService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.MigrationService;
import software.wings.service.intfc.VerificationService;
import software.wings.service.intfc.datadog.DatadogService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.service.intfc.verification.CVTaskService;
import software.wings.service.intfc.yaml.YamlPushService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Guice Module for initializing all beans.
 *
 * @author Raghu
 */
public class VerificationServiceModule extends AbstractModule {
  private VerificationServiceConfiguration configuration;

  /**
   * Creates a guice module for portal app.
   *
   * @param configuration Dropwizard configuration
   */
  public VerificationServiceModule(VerificationServiceConfiguration configuration) {
    this.configuration = configuration;
  }

  /* (non-Javadoc)
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    bind(VerificationServiceConfiguration.class).toInstance(configuration);
    bind(HPersistence.class).to(WingsMongoPersistence.class);
    bind(WingsPersistence.class).to(WingsMongoPersistence.class);
    bind(LearningEngineService.class).to(LearningEngineAnalysisServiceImpl.class);
    bind(SecretManager.class).to(NoOpSecretManagerImpl.class);
    bind(MigrationService.class).to(VerificationMigrationServiceImpl.class);
    bind(Clock.class).toInstance(Clock.systemUTC());

    bind(TimeLimiter.class).toInstance(new SimpleTimeLimiter());
    bind(TimeSeriesAnalysisService.class).to(TimeSeriesAnalysisServiceImpl.class);
    bind(LogAnalysisService.class).to(LogAnalysisServiceImpl.class);
    bind(CVTaskService.class).to(CVTaskServiceImpl.class);
    bind(FeatureFlagService.class).to(FeatureFlagServiceImpl.class);
    bind(CVActivityLogService.class).to(CVActivityLogServiceImpl.class);
    bind(CVConfigurationService.class).to(CVConfigurationServiceImpl.class);
    bind(DatadogService.class).to(DatadogServiceImpl.class);
    bind(ContinuousVerificationService.class).to(ContinuousVerificationServiceImpl.class);
    bind(VerificationService.class).to(VerificationServiceImpl.class);
    bind(CvValidationService.class).to(NoOpCvValidationServiceImpl.class);
    bind(YamlPushService.class).to(NoOpYamlPushService.class);
    bind(AlertService.class).to(NoOpAlertService.class);

    bind(ExecutorService.class)
        .toInstance(ThreadPool.create(1, 20, 5, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("Default-Verification-Executor-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));

    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("verificationServiceExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder()
                .setNameFormat("Verification-service-Thread-%d")
                .setPriority(Thread.NORM_PRIORITY)
                .build()));

    bind(ExecutorService.class)
        .annotatedWith(Names.named("verificationDataCollectorExecutor"))
        .toInstance(ThreadPool.create(1, 20, 5, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("Verification-Data-Collector-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));

    bind(QueueController.class).toInstance(new QueueController() {
      @Override
      public boolean isPrimary() {
        return true;
      }

      @Override
      public boolean isNotPrimary() {
        return false;
      }
    });

    if (configuration.getDataStorageMode() == null) {
      configuration.setDataStorageMode(DataStorageMode.MONGO);
    }

    switch (configuration.getDataStorageMode()) {
      case GOOGLE_CLOUD_DATA_STORE:
        bind(DataStoreService.class).to(GoogleDataStoreServiceImpl.class);
        break;
      case MONGO:
        bind(DataStoreService.class).to(MongoDataStoreServiceImpl.class);
        break;
      default:
        throw new WingsException("Invalid execution log data storage mode: " + configuration.getDataStorageMode());
    }

    try {
      VersionInfoManager versionInfoManager = new VersionInfoManager(IOUtils.toString(
          getClass().getClassLoader().getResourceAsStream("versionInfo.yaml"), StandardCharsets.UTF_8));
      bind(VersionInfoManager.class).toInstance(versionInfoManager);
    } catch (IOException e) {
      throw new RuntimeException("Could not load versionInfo.yaml", e);
    }
  }
}
