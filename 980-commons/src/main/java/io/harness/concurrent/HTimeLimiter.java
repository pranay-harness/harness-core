package io.harness.concurrent;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PL)
public class HTimeLimiter {
  // Warning: using a bounded executor may be counterproductive!
  // If the thread pool fills up, any time callers spend waiting for a thread may count toward their time limit,
  // and in this case the call may even time out before the target method is ever invoked.
  @Deprecated
  public TimeLimiter create() {
    return new SimpleTimeLimiter(Executors.newCachedThreadPool());
  }

  public TimeLimiter create(ExecutorService executor) {
    return new SimpleTimeLimiter(executor);
  }

  @CanIgnoreReturnValue
  public static <T> T callInterruptible21(TimeLimiter timeLimiter, Duration duration, Callable<T> callable)
      throws Exception {
    return timeLimiter.callWithTimeout(callable, duration.toMillis(), TimeUnit.MILLISECONDS, true);
  }

  @CanIgnoreReturnValue
  public static <T> T callUninterruptible21(TimeLimiter timeLimiter, Duration duration, Callable<T> callable)
      throws Exception {
    return timeLimiter.callWithTimeout(callable, duration.toMillis(), TimeUnit.MILLISECONDS, false);
  }
}
