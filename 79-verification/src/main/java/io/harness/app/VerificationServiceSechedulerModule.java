package io.harness.app;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import io.harness.scheduler.VerificationJobScheduler;
import software.wings.scheduler.QuartzScheduler;

/**
 * @author Raghu
 */
public class VerificationServiceSechedulerModule extends AbstractModule {
  /* (non-Javadoc)
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    bind(QuartzScheduler.class)
        .annotatedWith(Names.named("JobScheduler"))
        .to(VerificationJobScheduler.class)
        .asEagerSingleton();
  }
}
