package io.harness.time;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import io.harness.govern.DependencyModule;
import io.harness.threading.ExecutorModule;

import java.util.Set;

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
    bind(TimeLimiter.class).toInstance(new SimpleTimeLimiter(ExecutorModule.getInstance().getExecutorService()));
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(ExecutorModule.getInstance());
  }
}
