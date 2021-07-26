package io.harness.concurent;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.TimeLimiter;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;
import org.mockito.stubbing.OngoingStubbing;

@UtilityClass
public class HTimeLimiterMocker {
  public <T> OngoingStubbing<T> mockCallInterruptible(TimeLimiter timeLimiter, Duration duration) throws Exception {
    return (OngoingStubbing<T>) when(
        timeLimiter.callWithTimeout(any(Callable.class), eq(duration.toMillis()), eq(TimeUnit.MILLISECONDS)));
  }

  public <T> OngoingStubbing<T> mockCallInterruptible(TimeLimiter timeLimiter) throws Exception {
    return (OngoingStubbing<T>) when(
        timeLimiter.callWithTimeout(any(Callable.class), anyLong(), eq(TimeUnit.MILLISECONDS)));
  }

  public void verifyTimeLimiterCalled(TimeLimiter timeLimiter) throws Exception {
    verify(timeLimiter).callWithTimeout(any(), anyLong(), any());
  }
}
