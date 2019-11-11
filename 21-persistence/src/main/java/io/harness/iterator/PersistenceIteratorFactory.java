package io.harness.iterator;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.config.WorkersConfiguration;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.MongoPersistenceIteratorBuilder;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public final class PersistenceIteratorFactory {
  private static final Random rand = new Random();

  @Inject Injector injector;

  @Inject WorkersConfiguration workersConfiguration;

  public <T extends PersistentIterable> PersistenceIterator createIterator(
      Class cls, MongoPersistenceIteratorBuilder<T> builder) {
    if (!workersConfiguration.confirmWorkerIsActive(cls)) {
      logger.info("Worker {} is disabled in this setup", cls.getName());
      return null;
    }

    logger.info("Worker {} is enabled in this setup", cls.getName());
    final MongoPersistenceIterator<T> iterator = builder.build();
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

    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
        options.getPoolSize(), new ThreadFactoryBuilder().setNameFormat("Iterator-" + options.getName()).build());

    logger.info("Worker {} is enabled in this setup", cls.getName());
    final MongoPersistenceIterator<T> iterator =
        builder.executorService(executor).semaphore(new Semaphore(options.getPoolSize())).build();
    injector.injectMembers(iterator);

    final long millis = options.interval.toMillis();
    executor.scheduleAtFixedRate(
        () -> iterator.process(ProcessMode.PUMP), rand.nextInt((int) millis), millis, TimeUnit.MILLISECONDS);

    return iterator;
  }
}
