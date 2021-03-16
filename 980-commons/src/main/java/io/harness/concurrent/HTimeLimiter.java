package io.harness.concurrent;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Singleton
public class HTimeLimiter {
  private final SimpleTimeLimiter simpleTimeLimiter;

  @Inject
  public HTimeLimiter() {
    simpleTimeLimiter = new SimpleTimeLimiter();
  }

  @CanIgnoreReturnValue
  public <T> T callInterruptible(Duration duration, Callable<T> callable) throws Exception {
    return simpleTimeLimiter.callWithTimeout(callable, duration.toMillis(), TimeUnit.MILLISECONDS, true);
  }

  @CanIgnoreReturnValue
  public <T> T callUninterruptible(Duration duration, Callable<T> callable) throws Exception {
    return simpleTimeLimiter.callWithTimeout(callable, duration.toMillis(), TimeUnit.MILLISECONDS, false);
  }
}
