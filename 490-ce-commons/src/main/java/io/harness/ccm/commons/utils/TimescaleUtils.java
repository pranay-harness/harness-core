package io.harness.ccm.commons.utils;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.concurrent.Callable;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class TimescaleUtils {
  @SneakyThrows
  public <E> E retryRun(@NonNull Callable<E> callable) {
    return retryRun(callable, 3, ImmutableSet.of(Exception.class));
  }

  @SneakyThrows
  public <E> E retryRun(
      @NonNull Callable<E> callable, int retryCount, @NonNull final Set<Class<? extends Exception>> exceptions) {
    for (int i = 1; i <= retryCount; i++) {
      try {
        return callable.call();
      } catch (Exception ex) {
        if (!shouldRetryOn(ex, exceptions)) {
          log.info("Caught {} and its not set to retry on", ex.getClass());
          throw ex;
        }
        log.warn("Failed to execute, attempt {}, caused by ", i, ex);

        if (retryCount == i) {
          log.error("Retry exhausted, couldnt execute");
          throw ex;
        }

        Thread.sleep(100L);
      }
    }

    throw new Exception("Unreachable statement, unknown error occurred");
  }

  private static boolean shouldRetryOn(Exception ex, @NonNull final Set<Class<? extends Exception>> exceptions) {
    return exceptions.stream().anyMatch(c -> c.isAssignableFrom(ex.getClass()));
  }
}
