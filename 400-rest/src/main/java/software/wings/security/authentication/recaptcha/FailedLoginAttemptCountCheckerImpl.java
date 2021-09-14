/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.security.authentication.recaptcha;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.User;

import com.google.inject.Singleton;

@OwnedBy(PL)
@Singleton
public class FailedLoginAttemptCountCheckerImpl implements FailedLoginAttemptCountChecker {
  @Override
  public void check(User user) throws MaxLoginAttemptExceededException {
    int attempts = user.getUserLockoutInfo().getNumberOfFailedLoginAttempts();
    boolean countExceeded = false;

    if (null != user.getUserLockoutInfo()) {
      countExceeded = attempts > MAX_FAILED_ATTEMPTS_BEFORE_CAPTCHA;
    }

    if (countExceeded) {
      // FE should should show captcha on this error code
      throw new MaxLoginAttemptExceededException(MAX_FAILED_ATTEMPTS_BEFORE_CAPTCHA, attempts);
    }
  }
}
