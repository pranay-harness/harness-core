package io.harness.app;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import io.harness.scheduler.VerificationJobScheduler;
import software.wings.scheduler.QuartzScheduler;

/**
 * @author Raghu
 */
public class VerificationServiceSechedulerModule extends AbstractModule {
  private final VerificationServiceConfiguration configuration;

  public VerificationServiceSechedulerModule(VerificationServiceConfiguration configuration) {
    this.configuration = configuration;
  }

  /* (non-Javadoc)
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    configuration.getSchedulerConfig().setSchedulerName("verification_scheduler");
    configuration.getSchedulerConfig().setInstanceId("verification");
    configuration.getSchedulerConfig().setTablePrefix("quartz_verification");
    configuration.getSchedulerConfig().setThreadCount("25");
    bind(QuartzScheduler.class)
        .annotatedWith(Names.named("JobScheduler"))
        .to(VerificationJobScheduler.class)
        .asEagerSingleton();
  }
}
