/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.loginSettings;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.service.intfc.ownership.OwnedByAccount;

import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public interface LoginSettingsService extends OwnedByAccount {
  LoginSettings getLoginSettings(@NotNull String accountId);

  LoginSettings updatePasswordExpirationPolicy(String accountId, PasswordExpirationPolicy passwordExpirationPolicy);

  LoginSettings updatePasswordStrengthPolicy(String accountId, PasswordStrengthPolicy passwordStrengthPolicy);

  LoginSettings updateUserLockoutPolicy(String accountId, UserLockoutPolicy userLockoutPolicy);

  void createDefaultLoginSettings(Account account);

  @Override void deleteByAccountId(@NotNull String accountId);

  boolean verifyPasswordStrength(Account account, char[] password);

  boolean isUserLocked(User user, Account account);

  void updateUserLockoutInfo(User user, Account primaryAccount, int newCountOfFailedLoginAttempts);

  PasswordStrengthViolations getPasswordStrengthCheckViolations(Account account, char[] password);

  LoginSettings updateLoginSettings(String loginSettingsId, String accountId, LoginSettings loginSettings);

  PasswordStrengthPolicy getPasswordStrengthPolicy(String accountId);
}
