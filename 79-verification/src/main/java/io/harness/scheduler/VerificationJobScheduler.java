package io.harness.scheduler;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.app.VerificationServiceConfiguration;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class VerificationJobScheduler extends AbstractQuartzScheduler {
  private static final Logger logger = LoggerFactory.getLogger(VerificationJobScheduler.class);

  /**
   * Instantiates a new Cron scheduler.
   *
   * @param injector      the injector
   * @param configuration the configuration
   */
  @Inject
  private VerificationJobScheduler(Injector injector, VerificationServiceConfiguration configuration) {
    super(injector, configuration.getSchedulerConfig(), configuration.getMongoConnectionFactory());
    try {
      final Properties properties = getDefaultProperties();
      properties.setProperty("org.quartz.jobStore.collectionPrefix", "quartz_verification");
      scheduler = createScheduler(properties);
      scheduler.start();
    } catch (SchedulerException exception) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, exception)
          .addParam("message", "Could not initialize cron scheduler");
    }
  }
}
