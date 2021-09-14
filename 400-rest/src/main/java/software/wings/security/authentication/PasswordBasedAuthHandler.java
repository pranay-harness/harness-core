/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.EMAIL_NOT_VERIFIED;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.PASSWORD_EXPIRED;
import static io.harness.eraro.ErrorCode.USER_DOES_NOT_EXIST;
import static io.harness.eraro.ErrorCode.USER_LOCKED;
import static io.harness.exception.WingsException.USER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static org.mindrot.jbcrypt.BCrypt.checkpw;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.ng.core.account.AuthenticationMechanism;

import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.loginSettings.LoginSettingsService;
import software.wings.logcontext.UserLogContext;
import software.wings.security.authentication.recaptcha.FailedLoginAttemptCountChecker;
import software.wings.security.authentication.recaptcha.MaxLoginAttemptExceededException;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
@Singleton
@Slf4j
public class PasswordBasedAuthHandler implements AuthHandler {
  private UserService userService;
  private LoginSettingsService loginSettingsService;
  private AuthenticationUtils authenticationUtils;
  private AccountService accountService;
  private DomainWhitelistCheckerService domainWhitelistCheckerService;
  private FailedLoginAttemptCountChecker failedLoginAttemptCountChecker;

  @Inject
  public PasswordBasedAuthHandler(UserService userService, LoginSettingsService loginSettingsService,
      AuthenticationUtils authenticationUtils, AccountService accountService,
      DomainWhitelistCheckerService domainWhitelistCheckerService,
      FailedLoginAttemptCountChecker failedLoginAttemptCountChecker) {
    this.userService = userService;
    this.loginSettingsService = loginSettingsService;
    this.authenticationUtils = authenticationUtils;
    this.accountService = accountService;
    this.domainWhitelistCheckerService = domainWhitelistCheckerService;
    this.failedLoginAttemptCountChecker = failedLoginAttemptCountChecker;
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
    String accountId = user == null ? null : user.getDefaultAccountId();
    String uuid = user == null ? null : user.getUuid();

    try (AutoLogContext ignore = new UserLogContext(accountId, uuid, OVERRIDE_ERROR)) {
      log.info("Authenticating via Username Password");

      if (!user.isEmailVerified()) {
        throw new WingsException(EMAIL_NOT_VERIFIED, USER);
      }

      if (!domainWhitelistCheckerService.isDomainWhitelisted(user)) {
        domainWhitelistCheckerService.throwDomainWhitelistFilterException();
      }

      if (isPasswordHash) {
        if (password.equals(user.getPasswordHash())) {
          return getAuthenticationResponse(user);
        } else {
          updateFailedLoginAttemptCount(user);
        }
      } else {
        boolean validCredentials = checkpw(password, user.getPasswordHash());
        if (validCredentials) {
          return getAuthenticationResponse(user);
        } else {
          checkUserLockoutStatus(user);
          updateFailedLoginAttemptCount(user);

          try {
            failedLoginAttemptCountChecker.check(user);
          } catch (MaxLoginAttemptExceededException e) {
            throw new WingsException(ErrorCode.MAX_FAILED_ATTEMPT_COUNT_EXCEEDED, "Too many incorrect login attempts");
          }
        }
      }
      throw new WingsException(INVALID_CREDENTIAL, USER);
    }
  }

  private void updateFailedLoginAttemptCount(User user) {
    String accountId = user == null ? null : user.getDefaultAccountId();
    String uuid = user == null ? null : user.getUuid();
    try (AutoLogContext ignore = new UserLogContext(accountId, uuid, OVERRIDE_ERROR)) {
      int newCountOfFailedLoginAttempts = user.getUserLockoutInfo().getNumberOfFailedLoginAttempts() + 1;
      loginSettingsService.updateUserLockoutInfo(
          user, accountService.get(user.getDefaultAccountId()), newCountOfFailedLoginAttempts);
    }
  }

  private AuthenticationResponse getAuthenticationResponse(User user) {
    checkUserLockoutStatus(user);
    checkPasswordExpiry(user);
    loginSettingsService.updateUserLockoutInfo(user, accountService.get(user.getDefaultAccountId()), 0);
    return new AuthenticationResponse(user);
  }

  private void checkPasswordExpiry(User user) {
    // throwing password expiration status only in case of password is correct. Other-wise invalidCredential exception
    // should be thrown.
    String accountId = user == null ? null : user.getDefaultAccountId();
    String uuid = user == null ? null : user.getUuid();
    try (AutoLogContext ignore = new UserLogContext(accountId, uuid, OVERRIDE_ERROR)) {
      if (user.isPasswordExpired()) {
        throw new

            WingsException(PASSWORD_EXPIRED, USER);
      }
    }
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
  public io.harness.ng.core.account.AuthenticationMechanism getAuthenticationMechanism() {
    return AuthenticationMechanism.USER_PASSWORD;
  }

  protected User getUser(String email) {
    return userService.getUserByEmail(email);
  }

  protected Account getAccount(String accountId) {
    return accountService.getFromCacheWithFallback(accountId);
  }
}
