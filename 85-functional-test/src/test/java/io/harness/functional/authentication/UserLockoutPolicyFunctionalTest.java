package io.harness.functional.authentication;

import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.testframework.restutils.UserRestUtils.loginUserOrNull;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.restutils.LoginSettingsUtils;
import io.harness.testframework.restutils.UserRestUtils;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import software.wings.beans.User;
import software.wings.beans.loginSettings.LoginSettings;
import software.wings.beans.loginSettings.UserLockoutPolicy;
import software.wings.service.intfc.UserService;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UserLockoutPolicyFunctionalTest extends AbstractFunctionalTest {
  static final boolean ENABLE_LOCKOUT_POLICY = true;
  static final boolean DISABLE_LOCKOUT_POLICY = false;
  static final int MAX_NUMBER_OF_ATTEMPTS = 3;
  static final int UPDATED_MAX_NUMBER_OF_ATTEMPTS = 2;
  static final int LOCKOUT_CLEARING_PERIOD = 1;
  static final boolean ENABLE_NOTIFY_USER = true;
  static final String TEST_EMAIL = "default@harness.io";
  static final String CORRECT_PASSWORD = "default";
  static final String WRONG_PASSWORD = "defaul";
  static String LoginSettingsId;
  static UserLockoutPolicy userLockoutPolicy;
  static User user;

  @Inject private UserService userService;

  @Test
  @Owner(emails = UTKARSH, resent = false)
  @Category(FunctionalTests.class)
  public void TC0_setUserLockoutPolicy() {
    userLockoutPolicy = UserLockoutPolicy.builder()
                            .enableLockoutPolicy(ENABLE_LOCKOUT_POLICY)
                            .numberOfFailedAttemptsBeforeLockout(MAX_NUMBER_OF_ATTEMPTS)
                            .lockOutPeriod(LOCKOUT_CLEARING_PERIOD)
                            .notifyUser(ENABLE_NOTIFY_USER)
                            .build();

    LoginSettings loginSettings =
        LoginSettingsUtils.userLockoutPolicyUpdate(bearerToken, getAccount().getUuid(), userLockoutPolicy);
    assertThat(loginSettings.getUuid()).isNotNull();
    LoginSettingsId = loginSettings.getUuid();

    UserLockoutPolicy userLockoutPolicyResponse = loginSettings.getUserLockoutPolicy();
    assertThat(ENABLE_LOCKOUT_POLICY).isEqualTo(userLockoutPolicyResponse.isEnableLockoutPolicy());
    assertThat(MAX_NUMBER_OF_ATTEMPTS).isEqualTo(userLockoutPolicyResponse.getNumberOfFailedAttemptsBeforeLockout());
    assertThat(LOCKOUT_CLEARING_PERIOD).isEqualTo(userLockoutPolicyResponse.getLockOutPeriod());
    assertThat(ENABLE_NOTIFY_USER).isEqualTo(userLockoutPolicyResponse.isNotifyUser());
  }

  @Test
  @Owner(emails = UTKARSH, resent = false)
  @Category(FunctionalTests.class)
  public void TC1_updateUserLockoutPolicy() {
    userLockoutPolicy = UserLockoutPolicy.builder()
                            .enableLockoutPolicy(ENABLE_LOCKOUT_POLICY)
                            .numberOfFailedAttemptsBeforeLockout(UPDATED_MAX_NUMBER_OF_ATTEMPTS)
                            .lockOutPeriod(LOCKOUT_CLEARING_PERIOD)
                            .notifyUser(ENABLE_NOTIFY_USER)
                            .build();

    LoginSettings loginSettings =
        LoginSettingsUtils.userLockoutPolicyUpdate(bearerToken, getAccount().getUuid(), userLockoutPolicy);
    assertThat(LoginSettingsId).isEqualTo(loginSettings.getUuid());

    UserLockoutPolicy userLockoutPolicyResponse = loginSettings.getUserLockoutPolicy();
    assertThat(ENABLE_LOCKOUT_POLICY).isEqualTo(userLockoutPolicyResponse.isEnableLockoutPolicy());
    assertThat(UPDATED_MAX_NUMBER_OF_ATTEMPTS)
        .isEqualTo(userLockoutPolicyResponse.getNumberOfFailedAttemptsBeforeLockout());
  }

  @Test
  @Owner(emails = UTKARSH, resent = false)
  @Category(FunctionalTests.class)
  public void TC2_checkUserLockout() {
    user = loginUserOrNull(TEST_EMAIL, CORRECT_PASSWORD);
    assertThat(user).isNotNull();
    Setup.signOut(user.getUuid(), user.getToken());
    loginUserOrNull(TEST_EMAIL, WRONG_PASSWORD);
    loginUserOrNull(TEST_EMAIL, WRONG_PASSWORD);
    user = userService.getUserByEmail(TEST_EMAIL);
    assertThat(true).isEqualTo(user.isUserLocked());
    user = UserRestUtils.unlockUser(getAccount().getUuid(), bearerToken, TEST_EMAIL);
    assertThat(false).isEqualTo(user.isUserLocked());
  }

  @Test
  @Owner(emails = UTKARSH, resent = false)
  @Category(FunctionalTests.class)
  public void TC3_disableUserLockoutPolicy() {
    userLockoutPolicy = UserLockoutPolicy.builder().enableLockoutPolicy(DISABLE_LOCKOUT_POLICY).build();

    LoginSettings loginSettings =
        LoginSettingsUtils.userLockoutPolicyUpdate(bearerToken, getAccount().getUuid(), userLockoutPolicy);
    assertThat(LoginSettingsId).isEqualTo(loginSettings.getUuid());

    UserLockoutPolicy userLockoutPolicyResponse = loginSettings.getUserLockoutPolicy();
    assertThat(DISABLE_LOCKOUT_POLICY).isEqualTo(userLockoutPolicyResponse.isEnableLockoutPolicy());
  }
}