package software.wings.security.authentication;

import static io.harness.eraro.ErrorCode.EMAIL_NOT_VERIFIED;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.PASSWORD_EXPIRED;
import static io.harness.eraro.ErrorCode.USER_DOES_NOT_EXIST;
import static io.harness.eraro.ErrorCode.USER_LOCKED;
import static io.harness.exception.WingsException.USER;
import static org.mindrot.jbcrypt.BCrypt.checkpw;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.loginSettings.LoginSettingsService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;

@Singleton
public class PasswordBasedAuthHandler implements AuthHandler {
  private UserService userService;
  private LoginSettingsService loginSettingsService;
  private AuthenticationUtils authenticationUtils;
  private AccountService accountService;
  private DomainWhitelistCheckerService domainWhitelistCheckerService;

  @Inject
  public PasswordBasedAuthHandler(UserService userService, LoginSettingsService loginSettingsService,
      AuthenticationUtils authenticationUtils, AccountService accountService,
      DomainWhitelistCheckerService domainWhitelistCheckerService) {
    this.userService = userService;
    this.loginSettingsService = loginSettingsService;
    this.authenticationUtils = authenticationUtils;
    this.accountService = accountService;
    this.domainWhitelistCheckerService = domainWhitelistCheckerService;
  }

  @Override
  public AuthenticationResponse authenticate(String... credentials) {
    return authenticateInternal(false, credentials);
  }

  private AuthenticationResponse authenticateInternal(boolean isPasswordHash, String... credentials) {
    if (credentials == null || credentials.length != 2) {
      throw new WingsException(INVALID_ARGUMENT);
    }

    String userName = credentials[0];
    String password = credentials[1];

    User user = getUser(userName);
    if (user == null) {
      throw new WingsException(USER_DOES_NOT_EXIST, USER);
    }
    if (!user.isEmailVerified()) {
      throw new WingsException(EMAIL_NOT_VERIFIED, USER);
    }

    if (!domainWhitelistCheckerService.isDomainWhitelisted(user)) {
      domainWhitelistCheckerService.throwDomainWhitelistFilterException();
    }

    if (user.isPasswordExpired() == true) {
      throw new WingsException(PASSWORD_EXPIRED, USER);
    }

    if (isPasswordHash) {
      if (password.equals(user.getPasswordHash())) {
        return getAuthenticationResponse(user);
      } else {
        updateFailedLoginAttemptCount(user);
      }
    } else {
      if (checkpw(password, user.getPasswordHash())) {
        return getAuthenticationResponse(user);
      } else {
        updateFailedLoginAttemptCount(user);
      }
    }
    throw new WingsException(INVALID_CREDENTIAL, USER);
  }

  private void updateFailedLoginAttemptCount(User user) {
    int newCountOfFailedLoginAttempts = user.getUserLockoutInfo().getNumberOfFailedLoginAttempts() + 1;
    loginSettingsService.updateUserLockoutInfo(
        user, authenticationUtils.getPrimaryAccount(user), newCountOfFailedLoginAttempts);
  }

  private AuthenticationResponse getAuthenticationResponse(User user) {
    checkUserLockoutStatus(user);
    loginSettingsService.updateUserLockoutInfo(user, authenticationUtils.getPrimaryAccount(user), 0);
    return new AuthenticationResponse(user);
  }

  private void checkUserLockoutStatus(User user) {
    Account primaryAccount = accountService.get(user.getDefaultAccountId());
    if (loginSettingsService.isUserLocked(user, primaryAccount)) {
      throw new WingsException(USER_LOCKED, USER);
    }
  }

  public AuthenticationResponse authenticateWithPasswordHash(String... credentials) {
    return authenticateInternal(true, credentials);
  }

  @Override
  public AuthenticationMechanism getAuthenticationMechanism() {
    return AuthenticationMechanism.USER_PASSWORD;
  }

  protected User getUser(String email) {
    return userService.getUserByEmail(email);
  }
}
