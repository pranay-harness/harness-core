package io.harness.annotations.retry;

import com.google.inject.Singleton;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * This class if you want to execute your methods with Retry see {@link RetryOnException}
 * You can either annotate your method with {@link RetryOnException} or Directly Inject this class.
 */
@Slf4j
@Singleton
public class MethodExecutionHelper {
  /**
   * This method executes provided method in {@link IMethodWrapper} and Retries it for specified
   * noOfRetryAttempts for configured Exceptions with provided delay in between retires.
   * @param method Method you want to enable retry for
   * @param noOfRetryAttempts Number of times you want to retry before failing (Default : 1 )
   * @param sleepInterval Delay between retires in milliseconds (Default:0ms)
   * @param retryOnExceptions Configure specific exceptions you want to retryOn (Default: Exception.class)
   * @param <T> Configured Type inside methodWrapper
   * @return
   * @throws Throwable
   */
  @SafeVarargs
  public final <T> T execute(IMethodWrapper<T> method, int noOfRetryAttempts, long sleepInterval,
      Class<? extends Throwable>... retryOnExceptions) throws Throwable {
    // if someone calls this method directly instead of going via @RetryOnException rout

    if (noOfRetryAttempts < 1) {
      noOfRetryAttempts = 1;
    }
    Set<Class<? extends Throwable>> retryOnExceptionSet = new HashSet<Class<? extends Throwable>>();
    populateRetryableExceptionSet(retryOnExceptionSet, retryOnExceptions);

    log.debug("noOfRetryAttempts = " + noOfRetryAttempts);
    log.debug("retryOnExceptionsSet = " + retryOnExceptionSet);

    T result = null;
    for (int retryCount = 1; retryCount <= noOfRetryAttempts; retryCount++) {
      log.debug("Executing the method. Attempt #" + retryCount);
      try {
        result = method.execute();
        break;
      } catch (Throwable t) {
        Throwable e = t.getCause() != null ? t.getCause() : t;
        log.error("Caught Exception class : " + e.getClass());
        for (Class<? extends Throwable> exception : retryOnExceptionSet) {
          log.error(" Comparing with Configured Exception To Retry For : " + exception.getName());

          if (!exception.isAssignableFrom(e.getClass())) {
            log.error("Encountered exception which is not retryable: " + e.getClass());
            log.error("Throwing exception to the caller");
            throw t;
          }
        }
        log.error("Failed at Retry attempt :" + retryCount + " of : " + noOfRetryAttempts);
        if (retryCount >= noOfRetryAttempts) {
          log.error("Maximum retry attempts exceeded.");
          log.error("Throwing exception to the caller");
          throw t;
        }
        try {
          Thread.sleep(sleepInterval);
        } catch (InterruptedException e1) {
          // Intentionally left blank
        }
      }
    }
    return result;
  }

  private void populateRetryableExceptionSet(
      Set<Class<? extends Throwable>> retryOnExceptionSet, Class<? extends Throwable>[] retryOnExceptions) {
    if (retryOnExceptions != null && retryOnExceptions.length > 0) {
      for (Class<? extends Throwable> exception : retryOnExceptions) {
        retryOnExceptionSet.add(exception);
      }
    } else {
      // default ot Exception
      retryOnExceptionSet.add(Exception.class);
    }
  }
}
