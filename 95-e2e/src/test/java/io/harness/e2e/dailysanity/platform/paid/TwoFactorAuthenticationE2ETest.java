package io.harness.e2e.dailysanity.platform.paid;

import static io.harness.rule.OwnerRule.NATARAJA;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.E2ETests;
import io.harness.e2e.AbstractE2ETest;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.utils.TestUtils;
import io.harness.testframework.restutils.TwoFactorAuthRestUtils;
import io.harness.testframework.restutils.UserRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.User;
import software.wings.security.authentication.TwoFactorAuthenticationSettings;

import java.util.List;

@Slf4j
public class TwoFactorAuthenticationE2ETest extends AbstractE2ETest {
  @Inject ScmSecret scmSecret;
  String defaultUser = "autouser2@harness.io";
  String defaultPassword = TestUtils.getDecryptedValue("e2etest_autouser_password");

  @Test()
  @Owner(developers = NATARAJA)
  @Category(E2ETests.class)
  public void twoFactorAuthTest() {
    logger.info("Login to harness account");
    User user = Setup.retryLogin(defaultUser, defaultPassword);
    TwoFactorAuthenticationSettings otpSettings =
        TwoFactorAuthRestUtils.getOtpSettings(getAccount().getUuid(), user.getToken());
    user = TwoFactorAuthRestUtils.enableTwoFactorAuthentication(getAccount().getUuid(), user.getToken(), otpSettings);
    assertThat(user.getEmail()).isNotNull();
    logger.info("Two FA Authentication Enabled");
    Setup.signOut(user.getUuid(), user.getToken());
    user = TwoFactorAuthRestUtils.retryTwoFaLogin(
        defaultUser, defaultPassword, getAccount().getUuid(), otpSettings.getTotpSecretKey());
    logger.info("Two FA Login Successfull");
    assertThat(user.getToken()).isNotNull();
    UserRestUtils urUtil = new UserRestUtils();
    List<User> userList = urUtil.getUserList(user.getToken(), getAccount().getUuid());
    logger.info("Getting the User List to ensure 2fa login success");
    assertThat(userList.size() > 0).isTrue();
    TwoFactorAuthRestUtils.disableTwoFactorAuthentication(getAccount().getUuid(), user.getToken());
    Setup.signOut(user.getUuid(), user.getToken());
    logger.info("Disabled 2FA Login");
    user = Setup.loginUser(defaultUser, defaultPassword);
    userList = urUtil.getUserList(user.getToken(), getAccount().getUuid());
    logger.info("Getting the User List to ensure 2fa login disabled");
    assertThat(userList.size() > 0).isTrue();
  }
}
