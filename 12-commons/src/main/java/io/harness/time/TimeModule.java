package io.harness.time;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;

public class TimeModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(TimeLimiter.class).toInstance(new SimpleTimeLimiter());
  }
}
