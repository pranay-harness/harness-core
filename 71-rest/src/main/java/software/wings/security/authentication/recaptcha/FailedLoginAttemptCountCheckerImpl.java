package software.wings.security.authentication.recaptcha;

import com.google.inject.Singleton;

import software.wings.beans.User;

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
