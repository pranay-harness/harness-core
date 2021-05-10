package io.harness.concurrent;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PL)
public class HTimeLimiter {
  @CanIgnoreReturnValue
  public static <T> T callInterruptible(TimeLimiter timeLimiter, Duration duration, Callable<T> callable)
      throws Exception {
    return timeLimiter.callWithTimeout(callable, duration.toMillis(), TimeUnit.MILLISECONDS, true);
  }

  @CanIgnoreReturnValue
  public static <T> T callUninterruptible(TimeLimiter timeLimiter, Duration duration, Callable<T> callable)
      throws Exception {
    return timeLimiter.callWithTimeout(callable, duration.toMillis(), TimeUnit.MILLISECONDS, false);
  }
}
