package io.harness.app;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;

import io.harness.app.cvng.api.DataCollectionTaskService;
import io.harness.app.cvng.client.VerificationManagerService;
import io.harness.app.cvng.client.VerificationManagerServiceImpl;
import io.harness.app.cvng.impl.DataCollectionTaskServiceImpl;
import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.cvng.core.services.api.VerificationServiceSecretManager;
import io.harness.cvng.core.services.impl.TimeSeriesServiceImpl;
import io.harness.cvng.core.services.impl.VerificationServiceSecretManagerImpl;
import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.threading.ThreadPool;
import io.harness.version.VersionInfoManager;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Guice Module for initializing all beans.
 *
 * @author Raghu
 */
public class CVServiceModule extends AbstractModule {
  /* (non-Javadoc)
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    bind(ExecutorService.class)
        .toInstance(ThreadPool.create(1, 20, 5, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("default-cv-nextgen-executor-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));
    try {
      VersionInfoManager versionInfoManager = new VersionInfoManager(IOUtils.toString(
          getClass().getClassLoader().getResourceAsStream("versionInfo.yaml"), StandardCharsets.UTF_8));
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
      bind(VersionInfoManager.class).toInstance(versionInfoManager);
      bind(HPersistence.class).to(MongoPersistence.class);
      bind(VerificationServiceSecretManager.class).to(VerificationServiceSecretManagerImpl.class);
      bind(FeatureFlagService.class).to(FeatureFlagServiceImpl.class);
      bind(TimeSeriesService.class).to(TimeSeriesServiceImpl.class);
      bind(DataCollectionTaskService.class).to(DataCollectionTaskServiceImpl.class);
      bind(VerificationManagerService.class).to(VerificationManagerServiceImpl.class);
      bind(Clock.class).toInstance(Clock.systemUTC());
    } catch (IOException e) {
      throw new IllegalStateException("Could not load versionInfo.yaml", e);
    }
  }
}
