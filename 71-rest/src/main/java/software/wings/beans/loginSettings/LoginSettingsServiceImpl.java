package software.wings.beans.loginSettings;

import static io.harness.mongo.MongoUtils.setUnset;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.passay.PasswordData;
import software.wings.beans.Account;
import software.wings.beans.loginSettings.LoginSettings.LoginSettingKeys;
import software.wings.dl.WingsPersistence;

import java.util.EnumSet;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginSettingsServiceImpl implements LoginSettingsService {
  static final int DEFAULT_LOCK_OUT_PERIOD = 24;
  static final boolean NOTIFY_USER = true;
  static final int NUMBER_OF_FAILED_ATTEMPTS_ALLOWED_BEFORE_LOCKOUT = 5;
  static final int DAYS_BEFORE_USER_NOTIFIED = 5;
  static final int DAYS_BEFORE_PASSWORD_EXPIRES = 90;

  @Inject WingsPersistence wingsPersistence;

  private PasswordStrengthPolicy getDefaultPasswordStrengthPolicy() {
    return PasswordStrengthPolicy.builder().enabled(false).build();
  }

  private PasswordExpirationPolicy getDefaultPasswordExpirationPolicy() {
    return PasswordExpirationPolicy.builder()
        .enabled(false)
        .daysBeforeUserNotifiedOfPasswordExpiration(DAYS_BEFORE_USER_NOTIFIED)
        .daysBeforePasswordExpires(DAYS_BEFORE_PASSWORD_EXPIRES)
        .build();
  }

  private UserLockoutPolicy getDefaultUserLockoutPolicy() {
    return UserLockoutPolicy.builder()
        .enableLockoutPolicy(false)
        .lockOutPeriod(DEFAULT_LOCK_OUT_PERIOD)
        .notifyUser(NOTIFY_USER)
        .numberOfFailedAttemptsBeforeLockout(NUMBER_OF_FAILED_ATTEMPTS_ALLOWED_BEFORE_LOCKOUT)
        .build();
  }

  @Override
  public void createDefaultLoginSettings(Account account) {
    LoginSettings loginSettings = LoginSettings.builder()
                                      .passwordExpirationPolicy(getDefaultPasswordExpirationPolicy())
                                      .userLockoutPolicy(getDefaultUserLockoutPolicy())
                                      .passwordStrengthPolicy(getDefaultPasswordStrengthPolicy())
                                      .accountId(account.getUuid())
                                      .build();
    wingsPersistence.save(loginSettings);
  }

  @Override
  public LoginSettings getLoginSettings(String accountId) {
    return wingsPersistence.createQuery(LoginSettings.class).field(LoginSettingKeys.accountId).equal(accountId).get();
  }

  @Override
  public LoginSettings updatePasswordExpirationPolicy(
      String accountId, PasswordExpirationPolicy newPasswordExpirationPolicy) {
    UpdateOperations<LoginSettings> operations = wingsPersistence.createUpdateOperations(LoginSettings.class);
    setUnset(operations, LoginSettingKeys.passwordExpirationPolicy, newPasswordExpirationPolicy);
    return updateAndGetLoginSettings(accountId, operations);
  }

  @Override
  public LoginSettings updatePasswordStrengthPolicy(String accountId, PasswordStrengthPolicy passwordStrengthPolicy) {
    UpdateOperations<LoginSettings> operations = wingsPersistence.createUpdateOperations(LoginSettings.class);
    setUnset(operations, LoginSettingKeys.passwordStrengthPolicy, passwordStrengthPolicy);
    return updateAndGetLoginSettings(accountId, operations);
  }

  @Override
  public LoginSettings updateUserLockoutPolicy(String accountId, UserLockoutPolicy newUserLockoutPolicy) {
    UpdateOperations<LoginSettings> operations = wingsPersistence.createUpdateOperations(LoginSettings.class);
    setUnset(operations, LoginSettingKeys.userLockoutPolicy, newUserLockoutPolicy);
    return updateAndGetLoginSettings(accountId, operations);
  }

  private LoginSettings updateAndGetLoginSettings(String accountId, UpdateOperations<LoginSettings> operations) {
    update(accountId, operations);
    return getLoginSettings(accountId);
  }

  private void update(String accountId, UpdateOperations<LoginSettings> operations) {
    Query<LoginSettings> query =
        wingsPersistence.createQuery(LoginSettings.class).filter(LoginSettingKeys.accountId, accountId);
    wingsPersistence.update(query, operations);
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(LoginSettings.class).filter(LoginSettingKeys.accountId, accountId));
  }

  @Override
  public boolean verifyPasswordStrength(Account account, char[] password) {
    LoginSettings loginSettings = getLoginSettings(account.getUuid());
    PasswordStrengthPolicy passwordStrengthPolicy = loginSettings.getPasswordStrengthPolicy();
    if (passwordStrengthPolicy.isEnabled()) {
      validatePasswordStrength(passwordStrengthPolicy, password);
    }
    return true;
  }

  private void validatePasswordStrength(PasswordStrengthPolicy passwordStrengthPolicy, char[] password) {
    EnumSet.allOf(PasswordStrengthChecks.class).forEach(passwordStrengthChecks -> {
      PasswordData passwordData = new PasswordData(new String(password));

      if (!passwordStrengthChecks.validate(passwordData, passwordStrengthPolicy)) {
        throw new WingsException(
            String.format("Password validation for check %s failed.", passwordStrengthChecks), WingsException.USER);
      }
    });
  }
}
