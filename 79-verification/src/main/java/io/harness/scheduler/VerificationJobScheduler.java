package io.harness.scheduler;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.app.VerificationServiceConfiguration;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;

import java.util.Properties;

@Slf4j
public class VerificationJobScheduler extends HQuartzScheduler {
  /**
   * Instantiates a new Cron scheduler.
   *
   * @param injector      the injector
   * @param configuration the configuration
   */
  @Inject
  private VerificationJobScheduler(Injector injector, VerificationServiceConfiguration configuration) {
    super(injector, configuration.getSchedulerConfig(), configuration.getMongoConnectionFactory().getUri());
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
