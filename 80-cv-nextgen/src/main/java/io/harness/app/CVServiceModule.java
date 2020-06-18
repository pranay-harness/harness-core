package io.harness.app;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.cvng.core.services.api.VerificationServiceSecretManager;
import io.harness.cvng.core.services.impl.TimeSeriesServiceImpl;
import io.harness.cvng.core.services.impl.VerificationServiceSecretManagerImpl;
import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;
import io.harness.threading.ThreadPool;
import io.harness.version.VersionInfoManager;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        .annotatedWith(Names.named("cvConcurrentExecutor"))
        .toInstance(ThreadPool.create(1, 20, 5, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("cv-concurrent-processor-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));

    try {
      VersionInfoManager versionInfoManager = new VersionInfoManager(IOUtils.toString(
          getClass().getClassLoader().getResourceAsStream("versionInfo.yaml"), StandardCharsets.UTF_8));
      bind(VersionInfoManager.class).toInstance(versionInfoManager);
      bind(HPersistence.class).to(MongoPersistence.class);
      bind(VerificationServiceSecretManager.class).to(VerificationServiceSecretManagerImpl.class);
      bind(FeatureFlagService.class).to(FeatureFlagServiceImpl.class);
      bind(TimeSeriesService.class).to(TimeSeriesServiceImpl.class);
    } catch (IOException e) {
      throw new IllegalStateException("Could not load versionInfo.yaml", e);
    }
  }
}
