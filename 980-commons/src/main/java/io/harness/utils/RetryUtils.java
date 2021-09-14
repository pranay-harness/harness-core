/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.time.Duration;
import java.util.List;
import lombok.experimental.UtilityClass;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;

@UtilityClass
@OwnedBy(PL)
public class RetryUtils {
  public RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage,
      List<Class> exceptionClasses, Duration retrySleepDuration, int maxAttempts, Logger log) {
    RetryPolicy<Object> retryPolicy =
        new RetryPolicy<>()
            .withDelay(retrySleepDuration)
            .withMaxAttempts(maxAttempts)
            .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
            .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
    exceptionClasses.forEach(retryPolicy::handle);
    return retryPolicy;
  }
}
