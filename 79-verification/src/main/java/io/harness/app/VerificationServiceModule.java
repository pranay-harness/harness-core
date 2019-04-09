package io.harness.app;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import io.harness.security.NoOpSecretManagerImpl;
import io.harness.service.ContinuousVerificationServiceImpl;
import io.harness.service.LearningEngineAnalysisServiceImpl;
import io.harness.service.LogAnalysisServiceImpl;
import io.harness.service.NoOpCvValidationServiceImpl;
import io.harness.service.TimeSeriesAnalysisServiceImpl;
import io.harness.service.VerificationMigrationServiceImpl;
import io.harness.service.intfc.ContinuousVerificationService;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import io.harness.version.VersionInfoManager;
import org.apache.commons.io.IOUtils;
import software.wings.DataStorageMode;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.GoogleDataStoreServiceImpl;
import software.wings.service.impl.MongoDataStoreServiceImpl;
import software.wings.service.impl.verification.CVConfigurationServiceImpl;
import software.wings.service.impl.verification.CvValidationService;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.MigrationService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.verification.CVConfigurationService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

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
    bind(CVConfigurationService.class).to(CVConfigurationServiceImpl.class);
    bind(ContinuousVerificationService.class).to(ContinuousVerificationServiceImpl.class);
    bind(CvValidationService.class).to(NoOpCvValidationServiceImpl.class);

    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("verificationServiceExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder()
                .setNameFormat("Verification-service-Thread-%d")
                .setPriority(Thread.NORM_PRIORITY)
                .build()));

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
          this.getClass().getClassLoader().getResourceAsStream("versionInfo.yaml"), StandardCharsets.UTF_8));
      bind(VersionInfoManager.class).toInstance(versionInfoManager);
    } catch (IOException e) {
      throw new RuntimeException("Could not load versionInfo.yaml", e);
    }
  }
}
