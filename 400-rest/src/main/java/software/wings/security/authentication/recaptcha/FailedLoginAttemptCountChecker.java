/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
