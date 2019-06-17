package software.wings.beans.loginSettings;

import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.USER_LOCKED_NOTIFICATION;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.exception.WingsException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.passay.PasswordData;
import software.wings.beans.Account;
import software.wings.beans.Notification;
import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.beans.loginSettings.LoginSettings.LoginSettingKeys;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.notifications.UserGroupBasedDispatcher;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginSettingsServiceImpl implements LoginSettingsService {
  static final int DEFAULT_LOCK_OUT_PERIOD = 24;
  static final boolean NOTIFY_USER = true;
  static final int NUMBER_OF_FAILED_ATTEMPTS_ALLOWED_BEFORE_LOCKOUT = 5;
  static final int DAYS_BEFORE_USER_NOTIFIED = 5;
  static final int DAYS_BEFORE_PASSWORD_EXPIRES = 90;

  @Inject WingsPersistence wingsPersistence;
  @Inject UserService userService;
  @Inject UserGroupBasedDispatcher userGroupBasedDispatcher;
  @Inject UserGroupService userGroupService;

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
    LoginSettings loginSettings =
        wingsPersistence.createQuery(LoginSettings.class).field(LoginSettingKeys.accountId).equal(accountId).get();
    if (loginSettings == null) {
      throw new WingsException(String.format("Login settings not found for account Id: {}", accountId), USER);
    }
    return loginSettings;
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
    updateLoginSettings(accountId, operations);
    return getLoginSettings(accountId);
  }

  private void updateLoginSettings(String accountId, UpdateOperations<LoginSettings> operations) {
    update(accountId, operations);
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
      List<PasswordStrengthViolation> passwordPolicyViolations =
          getPasswordStrengthCheckViolationsInternal(passwordStrengthPolicy, password);
      if (!passwordPolicyViolations.isEmpty()) {
        throw new WingsException(
            String.format("Password validation checks failed for account :[%s].", account.getUuid()),
            WingsException.USER);
      }
      validatePasswordStrength(passwordStrengthPolicy, password);
    }
    return true;
  }

  @Override
  public PasswordStrengthViolations getPasswordStrengthCheckViolations(Account account, char[] password) {
    LoginSettings loginSettings = getLoginSettings(account.getUuid());
    PasswordStrengthPolicy passwordStrengthPolicy = loginSettings.getPasswordStrengthPolicy();
    List<PasswordStrengthViolation> passwordStrengthViolationsList = null;
    if (passwordStrengthPolicy.isEnabled()) {
      passwordStrengthViolationsList = getPasswordStrengthCheckViolationsInternal(passwordStrengthPolicy, password);
      return new PasswordStrengthViolations(passwordStrengthViolationsList, true);
    }
    return new PasswordStrengthViolations(new ArrayList<>(), false);
  }

  private List<PasswordStrengthViolation> getPasswordStrengthCheckViolationsInternal(
      PasswordStrengthPolicy passwordStrengthPolicy, char[] password) {
    PasswordData passwordData = new PasswordData(new String(password));
    return EnumSet.allOf(PasswordStrengthChecks.class)
        .stream()
        .filter(passwordStrengthCheck -> !passwordStrengthCheck.validate(passwordData, passwordStrengthPolicy))
        .map(passwordStrengthChecks
            -> new PasswordStrengthViolation(
                passwordStrengthChecks, passwordStrengthChecks.getMinimumCount(passwordStrengthPolicy)))
        .collect(Collectors.toList());
  }

  @Override
  public void updateUserLockoutInfo(User user, Account account, int newCountOfFailedLoginAttempts) {
    UserLockoutPolicy userLockoutPolicy = getLoginSettings(account.getUuid()).getUserLockoutPolicy();
    logger.info(String.format("Updating user lockout info: [%s] for user: [%s], new failedCount: [%s] ",
        userLockoutPolicy.isEnableLockoutPolicy(), user.getEmail(), newCountOfFailedLoginAttempts));
    if (userLockoutPolicy.isEnableLockoutPolicy()) {
      createUserLockoutInfoOperationsAndUpdateUser(user, newCountOfFailedLoginAttempts, userLockoutPolicy);
    }
  }

  private void createUserLockoutInfoOperationsAndUpdateUser(
      User user, int newCountOfFailedLoginAttempts, UserLockoutPolicy userLockoutPolicy) {
    UpdateOperations<User> operations = wingsPersistence.createUpdateOperations(User.class);
    updateUserLockoutInfoOperations(user, newCountOfFailedLoginAttempts, userLockoutPolicy, operations);
    userService.applyUpdateOperations(user, operations);
  }

  private void updateUserLockoutInfoOperations(User user, int newCountOfFailedLoginAttempts,
      UserLockoutPolicy userLockoutPolicy, UpdateOperations<User> operations) {
    if (lockUser(newCountOfFailedLoginAttempts, userLockoutPolicy)) {
      logger.info(String.format("Locking user: [%s] because of %s incorrect password attempts", user.getUuid(),
          newCountOfFailedLoginAttempts));
      UserLockoutInfo userLockoutInfo =
          createUserLockoutInfoInstance(newCountOfFailedLoginAttempts, System.currentTimeMillis());
      updateUserLockoutOperations(operations, true, userLockoutInfo);
      sendNotifications(user, userLockoutPolicy);
    } else if (unlockUser(newCountOfFailedLoginAttempts)) {
      logger.info(String.format("Unlocking user: [%s]", user.getUuid()));
      UserLockoutInfo userLockoutInfo =
          createUserLockoutInfoInstance(newCountOfFailedLoginAttempts, user.getUserLockoutInfo().getUserLockedAt());
      updateUserLockoutOperations(operations, false, userLockoutInfo);
    } else {
      logger.info(String.format("Updating user lockout info for user: [%s], incorrect attempt count [%s]",
          user.getUuid(), newCountOfFailedLoginAttempts));
      UserLockoutInfo userLockoutInfo =
          createUserLockoutInfoInstance(newCountOfFailedLoginAttempts, user.getUserLockoutInfo().getUserLockedAt());
      updateUserLockoutOperations(operations, user.isUserLocked(), userLockoutInfo);
    }
  }

  private void sendNotifications(User user, UserLockoutPolicy userLockoutPolicy) {
    if (userLockoutPolicy.isNotifyUser()) {
      userService.sendAccountLockedNotificationMail(user, userLockoutPolicy.getLockOutPeriod());
    }

    List<UserGroup> userGroupsToNotify = userLockoutPolicy.getUserGroupsToNotify();
    if (userGroupsToNotify != null) {
      Notification notification = anInformationNotification()
                                      .withNotificationTemplateId(USER_LOCKED_NOTIFICATION.name())
                                      .withNotificationTemplateVariables(ImmutableMap.of("lockedUser", user.getEmail()))
                                      .withAccountId(user.getDefaultAccountId())
                                      .build();

      userGroupsToNotify.forEach(userGroup -> {
        UserGroup userGroupToBeNotified = getCompleteUserGroup(user.getDefaultAccountId(), userGroup.getUuid());
        userGroupBasedDispatcher.dispatch(Arrays.asList(notification), userGroupToBeNotified);
      });
    }
  }

  private UserGroup getCompleteUserGroup(String accountId, String userGroupId) {
    return userGroupService.get(accountId, userGroupId, true);
  }

  private UserLockoutInfo createUserLockoutInfoInstance(int newCountOfFailedLoginAttempts, long userLockedAt) {
    return UserLockoutInfo.builder()
        .numberOfFailedLoginAttempts(newCountOfFailedLoginAttempts)
        .userLockedAt(userLockedAt)
        .build();
  }

  private boolean unlockUser(int numberOfFailedLoginAttempts) {
    return numberOfFailedLoginAttempts == 0;
  }

  private boolean lockUser(int numberOfFailedLoginAttempts, UserLockoutPolicy userLockoutPolicy) {
    return numberOfFailedLoginAttempts == userLockoutPolicy.getNumberOfFailedAttemptsBeforeLockout();
  }

  private void updateUserLockoutOperations(
      UpdateOperations<User> operations, boolean isUserLocked, UserLockoutInfo userLockoutInfo) {
    setUnset(operations, UserKeys.userLocked, isUserLocked);
    setUnset(operations, UserKeys.userLockoutInfo, userLockoutInfo);
  }

  @Override
  public boolean isUserLocked(User user, Account account) {
    UserLockoutPolicy userLockoutPolicy = getLoginSettings(account.getUuid()).getUserLockoutPolicy();
    // if the settings are not enforced or user is not locked return false
    if (!userLockoutPolicy.isEnableLockoutPolicy() || !user.isUserLocked()) {
      return false;
    }
    int maxLockOutPeriod = userLockoutPolicy.getLockOutPeriod();
    long userLockoutPeriod =
        Instant.ofEpochMilli(user.getUserLockoutInfo().getUserLockedAt()).until(Instant.now(), ChronoUnit.HOURS);
    return userLockoutPeriod < maxLockOutPeriod;
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
