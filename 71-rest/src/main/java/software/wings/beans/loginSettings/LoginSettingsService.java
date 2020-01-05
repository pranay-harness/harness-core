package software.wings.beans.loginSettings;

import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.service.intfc.ownership.OwnedByAccount;

import javax.validation.constraints.NotNull;

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
}