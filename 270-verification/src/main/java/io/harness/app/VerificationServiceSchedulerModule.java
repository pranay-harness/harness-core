package io.harness.app;

import io.harness.scheduler.PersistentScheduler;
import io.harness.scheduler.VerificationJobScheduler;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

/**
 * @author Raghu
 */
public class VerificationServiceSchedulerModule extends AbstractModule {
  private final VerificationServiceConfiguration configuration;

  public VerificationServiceSchedulerModule(VerificationServiceConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    bind(PersistentScheduler.class)
        .annotatedWith(Names.named("BackgroundJobScheduler"))
        .to(VerificationJobScheduler.class)
        .asEagerSingleton();
  }
}
