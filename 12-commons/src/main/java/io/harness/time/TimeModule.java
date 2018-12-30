package io.harness.time;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import io.harness.govern.DependencyModule;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TimeModule extends DependencyModule {
  private static volatile TimeModule instance;

  public static TimeModule getInstance() {
    if (instance == null) {
      instance = new TimeModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    // Create a dedicated executor service for TimeLimiter: it has 2 core threads anc can scale up to 32 threads.
    // Its task queue can grow up to 100k, then all subsequent tasks submission will be blocked if queue is full.
    ExecutorService timeLimiterExecutorService =
        new ThreadPoolExecutor(2, 32, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100000));
    bind(TimeLimiter.class).toInstance(new SimpleTimeLimiter(timeLimiterExecutorService));
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return null;
  }
}
