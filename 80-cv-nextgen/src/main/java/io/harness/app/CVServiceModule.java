package io.harness.app;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

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
  private VerificationConfiguration configuration;

  /**
   * Creates a guice module for portal app.
   *
   * @param configuration Dropwizard configuration
   */
  public CVServiceModule(VerificationConfiguration configuration) {
    this.configuration = configuration;
  }

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
    } catch (IOException e) {
      throw new IllegalStateException("Could not load versionInfo.yaml", e);
    }
  }
}
