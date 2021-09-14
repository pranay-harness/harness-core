/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.metrics.modules;

import io.harness.metrics.service.api.MetricService;
import io.harness.metrics.service.impl.MetricServiceImpl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class MetricsModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(MetricService.class).to(MetricServiceImpl.class);

    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("metricsPublisherExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder()
                .setNameFormat("metrics-publisher-Thread-%d")
                .setPriority(Thread.NORM_PRIORITY)
                .build()));
  }
}
