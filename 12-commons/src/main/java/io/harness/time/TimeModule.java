package io.harness.time;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.govern.DependencyModule;
import io.harness.threading.ExecutorModule;

import java.util.Set;
import java.util.concurrent.ExecutorService;

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
    // nothing to configure
  }

  @Provides
  @Singleton
  public TimeLimiter timeLimiter(ExecutorService executorService) {
    return new SimpleTimeLimiter(executorService);
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(ExecutorModule.getInstance());
  }
}
