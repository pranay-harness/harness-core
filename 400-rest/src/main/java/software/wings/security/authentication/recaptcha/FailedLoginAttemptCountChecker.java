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

@OwnedBy(PL)
public interface FailedLoginAttemptCountChecker {
  int MAX_FAILED_ATTEMPTS_BEFORE_CAPTCHA = 3;

  /**
   * Checks if user has had too many failed login attempts.
   * @throws MaxLoginAttemptExceededException in case failed attempts reach max attempts
   */
  void check(User user) throws MaxLoginAttemptExceededException;
}
