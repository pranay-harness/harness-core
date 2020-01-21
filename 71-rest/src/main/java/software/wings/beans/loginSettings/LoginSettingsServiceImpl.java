package software.wings.beans.loginSettings;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
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
import software.wings.beans.Event;
import software.wings.beans.InformationNotification;
import software.wings.beans.Notification;
import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.beans.loginSettings.LoginSettings.LoginSettingKeys;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.AuditServiceHelper;
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
import javax.annotation.Nonnull;

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
  @Inject AuditServiceHelper auditServiceHelper;

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
    LoginSettings loginSettings = updateAndGetLoginSettings(accountId, operations);
    auditPasswordExpirationPolicy(accountId, loginSettings);
    logger.info("Auditing updation of Password Expiration Policy for account={}", accountId);
    return loginSettings;
  }

  @Override
  public LoginSettings updatePasswordStrengthPolicy(String accountId, PasswordStrengthPolicy passwordStrengthPolicy) {
    UpdateOperations<LoginSettings> operations = wingsPersistence.createUpdateOperations(LoginSettings.class);
    setUnset(operations, LoginSettingKeys.passwordStrengthPolicy, passwordStrengthPolicy);
    LoginSettings loginSettings = updateAndGetLoginSettings(accountId, operations);
    auditPasswordStrengthPolicy(accountId, loginSettings);
    logger.info("Auditing updation of Password Strength Policy for account={}", accountId);
    return loginSettings;
  }

  private void auditPasswordStrengthPolicy(String accountId, LoginSettings loginSettings) {
    LoginSettings auditLoginSettings = loginSettings;
    auditLoginSettings.setPasswordExpirationPolicy(null);
    auditServiceHelper.reportForAuditingUsingAccountId(accountId, null, auditLoginSettings, Event.Type.UPDATE);
  }

  private void auditPasswordExpirationPolicy(String accountId, LoginSettings loginSettings) {
    LoginSettings auditLoginSettings = loginSettings;
    auditLoginSettings.setPasswordStrengthPolicy(null);
    auditServiceHelper.reportForAuditingUsingAccountId(accountId, null, auditLoginSettings, Event.Type.UPDATE);
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
    logger.info("Updating user lockout info: {} for user: {}, new failedCount: {}",
        userLockoutPolicy.isEnableLockoutPolicy(), user.getEmail(), newCountOfFailedLoginAttempts);

    createUserLockoutInfoOperationsAndUpdateUser(user, newCountOfFailedLoginAttempts, userLockoutPolicy);
  }

  private void createUserLockoutInfoOperationsAndUpdateUser(
      User user, int newCountOfFailedLoginAttempts, UserLockoutPolicy userLockoutPolicy) {
    UpdateOperations<User> operations = wingsPersistence.createUpdateOperations(User.class);
    updateUserLockoutInfoOperations(user, newCountOfFailedLoginAttempts, userLockoutPolicy, operations);
    userService.applyUpdateOperations(user, operations);
  }

  private void updateUserLockoutInfoOperations(User user, int newCountOfFailedLoginAttempts,
      UserLockoutPolicy userLockoutPolicy, UpdateOperations<User> operations) {
    boolean shouldLockUser = userLockoutPolicy.isEnableLockoutPolicy()
        && newCountOfFailedLoginAttempts >= userLockoutPolicy.getNumberOfFailedAttemptsBeforeLockout();

    //    boolean shouldUnlockUser =
    //        newCountOfFailedLoginAttempts < userLockoutPolicy.getNumberOfFailedAttemptsBeforeLockout();

    if (shouldLockUser) {
      logger.info("Locking user: [{}] because of {} incorrect password attempts", user.getUuid(),
          newCountOfFailedLoginAttempts);
      UserLockoutInfo userLockoutInfo =
          createUserLockoutInfoInstance(newCountOfFailedLoginAttempts, System.currentTimeMillis());
      updateUserLockoutOperations(operations, true, userLockoutInfo);
      sendNotifications(user, userLockoutPolicy);
      if (isNotEmpty(user.getAccounts())) {
        user.getAccounts().forEach(account -> {
          auditServiceHelper.reportForAuditingUsingAccountId(account.getUuid(), null, user, Event.Type.LOCK);
          logger.info("Auditing locking of user={} in account={}", user.getName(), account.getAccountName());
        });
      }
    } else {
      logger.info("Unlocking user: {}, current lock state = {}", user.getUuid(), user.isUserLocked());
      UserLockoutInfo userLockoutInfo =
          createUserLockoutInfoInstance(newCountOfFailedLoginAttempts, user.getUserLockoutInfo().getUserLockedAt());
      updateUserLockoutOperations(operations, false, userLockoutInfo);
    }
  }

  private void sendNotifications(User user, UserLockoutPolicy userLockoutPolicy) {
    if (userLockoutPolicy.isNotifyUser()) {
      userService.sendAccountLockedNotificationMail(user, userLockoutPolicy.getLockOutPeriod());
    }

    List<UserGroup> userGroupsToNotify = userLockoutPolicy.getUserGroupsToNotify();
    if (userGroupsToNotify != null) {
      Notification notification = InformationNotification.builder()
                                      .notificationTemplateId(USER_LOCKED_NOTIFICATION.name())
                                      .notificationTemplateVariables(ImmutableMap.of("lockedUser", user.getEmail()))
                                      .accountId(user.getDefaultAccountId())
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

  private void updateUserLockoutOperations(
      UpdateOperations<User> operations, boolean isUserLocked, @Nonnull UserLockoutInfo userLockoutInfo) {
    operations.set(UserKeys.userLocked, isUserLocked);
    operations.set(UserKeys.userLockoutInfo, userLockoutInfo);
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

    // auto unlock
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
