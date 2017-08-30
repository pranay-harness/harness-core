package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.Injector;

import software.wings.app.MainConfiguration;

public class VerificationJobScheduler extends AbstractQuartzScheduler {
  /**
   * Instantiates a new Cron scheduler.
   *
   * @param injector      the injector
   * @param configuration the configuration
   */
  @Inject
  public VerificationJobScheduler(Injector injector, MainConfiguration configuration) {
    super(injector, configuration);
  }
}
