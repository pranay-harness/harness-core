/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
