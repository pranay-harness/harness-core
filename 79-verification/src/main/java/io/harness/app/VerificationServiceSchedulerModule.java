package io.harness.app;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import io.harness.scheduler.PersistentScheduler;
import io.harness.scheduler.VerificationJobScheduler;

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
        .annotatedWith(Names.named("JobScheduler"))
        .to(VerificationJobScheduler.class)
        .asEagerSingleton();
  }
}
