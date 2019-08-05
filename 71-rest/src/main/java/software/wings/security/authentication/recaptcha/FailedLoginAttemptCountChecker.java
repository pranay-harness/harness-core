package software.wings.security.authentication.recaptcha;

import software.wings.beans.User;

public interface FailedLoginAttemptCountChecker {
  int MAX_FAILED_ATTEMPTS_BEFORE_CAPTCHA = 3;

  /**
   * Checks if user has had too many failed login attempts.
   * @throws MaxLoginAttemptExceededException in case failed attempts reach max attempts
   */
  void check(User user) throws MaxLoginAttemptExceededException;
}
