package io.harness.app;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import io.harness.security.NoOpSecretManagerImpl;
import io.harness.service.ContinuousVerificationServiceImpl;
import io.harness.service.LearningEngineAnalysisServiceImpl;
import io.harness.service.LogAnalysisServiceImpl;
import io.harness.service.TimeSeriesAnalysisServiceImpl;
import io.harness.service.VerificationMigrationServiceImpl;
import io.harness.service.intfc.ContinuousVerificationService;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import io.harness.version.VersionInfoManager;
import org.apache.commons.io.IOUtils;
import ro.fortsoft.pf4j.DefaultPluginManager;
import ro.fortsoft.pf4j.PluginManager;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.verification.CVConfigurationServiceImpl;
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
    bind(PluginManager.class).to(DefaultPluginManager.class).asEagerSingleton();
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

    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("verificationServiceExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder()
                .setNameFormat("Verification-service-Thread-%d")
                .setPriority(Thread.NORM_PRIORITY)
                .build()));

    try {
      VersionInfoManager versionInfoManager = new VersionInfoManager(IOUtils.toString(
          this.getClass().getClassLoader().getResourceAsStream("versionInfo.yaml"), StandardCharsets.UTF_8));
      bind(VersionInfoManager.class).toInstance(versionInfoManager);
    } catch (IOException e) {
      throw new RuntimeException("Could not load versionInfo.yaml", e);
    }
  }
}
