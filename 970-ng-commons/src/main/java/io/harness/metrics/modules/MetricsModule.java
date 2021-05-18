package io.harness.metrics.modules;

import io.harness.metrics.service.api.MetricService;
import io.harness.metrics.service.impl.MetricServiceImpl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class MetricsModule extends AbstractModule {
  private static MetricsModule instance;

  public static MetricsModule getInstance() {
    if (instance == null) {
      instance = new MetricsModule();
    }
    return instance;
  }

  private MetricsModule() {}

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
