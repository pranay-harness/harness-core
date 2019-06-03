package io.harness.testframework.framework;

import io.harness.testframework.framework.matchers.ArtifactMatcher;
import io.harness.testframework.framework.matchers.BooleanMatcher;
import io.harness.testframework.framework.matchers.EmailMatcher;
import io.harness.testframework.framework.matchers.MailinatorEmailMatcher;
import io.harness.testframework.framework.matchers.Matcher;
import io.harness.testframework.framework.matchers.SettingsAttributeMatcher;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class Retry<T> {
  private int retryCounter;
  private int maxRetries;
  private int introduceDelayInMS;

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
    T actual = null;
    while (retryCounter < maxRetries) {
      logger.info("Retry Attempt : " + retryCounter);
      try {
        TimeUnit.MILLISECONDS.sleep(this.introduceDelayInMS);
        actual = function.get();
        if (matcher instanceof EmailMatcher) {
          if (matcher.matches(expected, actual)) {
            return actual;
          }
        } else if (matcher instanceof SettingsAttributeMatcher) {
          if (matcher.matches(expected, actual)) {
            return actual;
          }
        } else if (matcher instanceof MailinatorEmailMatcher) {
          if (matcher.matches(expected, actual)) {
            return actual;
          }
        } else if (matcher instanceof ArtifactMatcher) {
          if (matcher.matches(expected, actual)) {
            return actual;
          }
        } else if (matcher instanceof BooleanMatcher) {
          if (matcher.matches(expected, actual)) {
            return actual;
          }
        }
      } catch (Exception ex) {
        logger.info("Execution failed on retry " + retryCounter + " of " + maxRetries + " error: " + ex);
        if (retryCounter >= maxRetries) {
          logger.warn("Max retries exceeded.");
          break;
        }
      }
      retryCounter++;
    }
    throw new RuntimeException("Command failed on all of " + maxRetries + " retries");
  }
}
