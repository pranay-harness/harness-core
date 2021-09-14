/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.scheduler;

import io.harness.app.VerificationServiceConfiguration;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;

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
      if (configuration.getSchedulerConfig().getAutoStart().equals("true")) {
        scheduler = createScheduler(properties);
        scheduler.start();
      }
    } catch (SchedulerException exception) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, exception)
          .addParam("message", "Could not initialize cron scheduler");
    }
  }
}
