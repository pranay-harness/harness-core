package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

import software.wings.app.MainConfiguration;

public class VerificationJobScheduler extends AbstractQuartzScheduler {
  /**
   * Instantiates a new Cron scheduler.
   *
   * @param injector      the injector
   * @param configuration the configuration
   */
  private VerificationJobScheduler(Injector injector, MainConfiguration configuration) {
    super(injector, configuration);
  }

  public static class JobSchedulerProvider implements Provider<JobScheduler> {
    @javax.inject.Inject Injector injector;
    @javax.inject.Inject MainConfiguration configuration;

    @Override
    public JobScheduler get() {
      configuration.getSchedulerConfig().setSchedulerName("verification_scheduler");
      configuration.getSchedulerConfig().setInstanceId("verification");
      configuration.getSchedulerConfig().setTablePrefix("quartz_verification");
      configuration.getSchedulerConfig().setThreadCount("15");
      return new JobScheduler(injector, configuration);
    }
  }
}
