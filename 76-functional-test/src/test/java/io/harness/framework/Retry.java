package io.harness.framework;

import io.harness.framework.matchers.EmailMatcher;
import io.harness.framework.matchers.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Retry<T> {
  private int retryCounter;
  private int maxRetries;
  private int introduceDelayInMS;
  private static final Logger logger = LoggerFactory.getLogger(Retry.class);

  public Retry(int maxRetries, int introduceDelayInMS) {
    this.maxRetries = maxRetries;
    this.introduceDelayInMS = introduceDelayInMS;
  }

  public Retry(int maxRetries) {
    this.maxRetries = maxRetries;
    this.introduceDelayInMS = 0;
  }

  // Takes a function and executes it, if fails, passes the function to the retry command
  public T executeWithRetry(Supplier<T> function, Matcher<T> matcher, T expected) {
    return retry(function, matcher, expected);
  }

  public int getRetryCounter() {
    return retryCounter;
  }

  private T retry(Supplier<T> function, Matcher<T> matcher, T expected) throws RuntimeException {
    logger.info("Execution will be retried : " + maxRetries + " times.");
    retryCounter = 0;
    while (retryCounter < maxRetries) {
      try {
        TimeUnit.MILLISECONDS.sleep(this.introduceDelayInMS);
        T actual = function.get();
        if (matcher instanceof EmailMatcher) {
          if (matcher.matches(expected, actual)) {
            return actual;
          }
        }
      } catch (Exception ex) {
        retryCounter++;
        logger.info("Execution failed on retry " + retryCounter + " of " + maxRetries + " error: " + ex);
        if (retryCounter >= maxRetries) {
          logger.warn("Max retries exceeded.");
          break;
        }
      }
    }
    throw new RuntimeException("Command failed on all of " + maxRetries + " retries");
  }
}
