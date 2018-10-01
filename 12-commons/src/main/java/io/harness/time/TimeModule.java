package io.harness.time;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

import io.harness.govern.DependencyModule;

import java.util.Set;

public class TimeModule extends DependencyModule {
  private static TimeModule instance;

  public static TimeModule getInstance() {
    if (instance == null) {
      instance = new TimeModule();
    }
    return instance;
  }
  @Override
  protected void configure() {
    bind(TimeLimiter.class).toInstance(new SimpleTimeLimiter());
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return null;
  }
}
