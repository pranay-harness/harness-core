/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.metrics.jobs;

import io.harness.metrics.service.api.MetricsPublisher;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import java.security.SecureRandom;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

@Slf4j
public class RecordMetricsJob {
  public static final int METRICS_RECORD_PERIOD_SECONDS = 60;

  @Inject private Injector injector;
  @Inject @Named("metricsPublisherExecutor") protected ScheduledExecutorService executorService;

  public void scheduleMetricsTasks() {
    long initialDelay = new SecureRandom().nextInt(60);
    Reflections reflections = new Reflections("io.harness");
    Set<Class<? extends MetricsPublisher>> classes = reflections.getSubTypesOf(MetricsPublisher.class);

    try {
      classes.forEach(subClass -> {
        try {
          MetricsPublisher publisher = injector.getInstance(subClass);
          executorService.scheduleAtFixedRate(
              () -> publisher.recordMetrics(), initialDelay, METRICS_RECORD_PERIOD_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
          log.error("Exception while creating a scheduled metrics recorder", e);
        }
      });
    } catch (Exception ex) {
      log.error("Exception while instantiating an instance of MetricsPublisher", ex);
    }
  }
}
