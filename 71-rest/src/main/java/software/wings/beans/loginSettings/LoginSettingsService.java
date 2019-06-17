package software.wings.beans.loginSettings;

import software.wings.beans.Account;
import software.wings.service.intfc.ownership.OwnedByAccount;

import javax.validation.constraints.NotNull;

public interface LoginSettingsService extends OwnedByAccount {
  LoginSettings getLoginSettings(@NotNull String accountId);

  LoginSettings updatePasswordExpirationPolicy(String accountId, PasswordExpirationPolicy passwordExpirationPolicy);

  LoginSettings updatePasswordStrengthPolicy(String accountId, PasswordStrengthPolicy passwordStrengthPolicy);

  LoginSettings updateUserLockoutPolicy(String accountId, UserLockoutPolicy userLockoutPolicy);

  void createDefaultLoginSettings(Account account);

  void deleteByAccountId(@NotNull String accountId);

  boolean verifyPasswordStrength(Account account, char[] password);
}