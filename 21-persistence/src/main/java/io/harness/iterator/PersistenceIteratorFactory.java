package io.harness.iterator;

import static io.harness.iterator.PersistenceIterator.ProcessMode.PUMP;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import io.harness.config.WorkersConfiguration;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.MongoPersistenceIteratorBuilder;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public final class PersistenceIteratorFactory {
  private static final SecureRandom random = new SecureRandom();

  @Inject Injector injector;

  @Inject WorkersConfiguration workersConfiguration;
  @Inject HarnessMetricRegistry harnessMetricRegistry;

  public <T extends PersistentIterable> PersistenceIterator createIterator(
      Class cls, MongoPersistenceIteratorBuilder<T> builder) {
    if (!workersConfiguration.confirmWorkerIsActive(cls)) {
      logger.info("Worker {} is disabled in this setup", cls.getName());
      return null;
    }

    logger.info("Worker {} is enabled in this setup", cls.getName());
    MongoPersistenceIterator<T> iterator = builder.build();
    injector.injectMembers(iterator);
    return iterator;
  }

  @Value
  @Builder
  public static class PumpExecutorOptions {
    private String name;
    private int poolSize;
    private Duration interval;
  }

  public <T extends PersistentIterable> PersistenceIterator createPumpIteratorWithDedicatedThreadPool(
      PumpExecutorOptions options, Class cls, MongoPersistenceIteratorBuilder<T> builder) {
    if (!workersConfiguration.confirmWorkerIsActive(cls)) {
      logger.info("Worker {} is disabled in this setup", cls.getName());
      return null;
    }

    String iteratorName = "Iterator-" + options.getName();
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
        options.getPoolSize(), new ThreadFactoryBuilder().setNameFormat(iteratorName).build());
    logger.info("Worker {} is enabled in this setup", cls.getName());

    MetricRegistry metricRegistry = harnessMetricRegistry.getThreadPoolMetricRegistry();
    InstrumentedExecutorService instrumentedExecutorService =
        new InstrumentedExecutorService(executor, metricRegistry, iteratorName);

    MongoPersistenceIterator<T> iterator = builder.mode(PUMP)
                                               .executorService(instrumentedExecutorService)
                                               .semaphore(new Semaphore(options.getPoolSize()))
                                               .build();
    injector.injectMembers(iterator);
    long millis = options.interval.toMillis();
    executor.scheduleAtFixedRate(() -> iterator.process(), random.nextInt((int) millis), millis, TimeUnit.MILLISECONDS);

    return iterator;
  }
}
