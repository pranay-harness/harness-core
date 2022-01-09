/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.users;

import static io.harness.rule.OwnerRule.NATARAJA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.restutils.TwoFactorAuthRestUtils;
import io.harness.testframework.restutils.UserRestUtils;

import software.wings.beans.PublicUser;
import software.wings.beans.User;
import software.wings.security.authentication.TwoFactorAuthenticationSettings;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class TwoFactorAuthenticationTest extends AbstractFunctionalTest {
  @Inject ScmSecret scmSecret;
  String defaultUser = "default2fa@harness.io";
  String defaultPassword = "";

  @Test()
  @Owner(developers = NATARAJA, intermittent = true)
  @Category(FunctionalTests.class)
  public void verifyTwoFactorAuthLogin() {
    defaultPassword = scmSecret.decryptToString(new SecretName("user_default_password"));
    User user = Setup.retryLogin(defaultUser, defaultPassword);
    assertThat(user.getToken()).isNotNull();
    TwoFactorAuthenticationSettings otpSettings =
        TwoFactorAuthRestUtils.getOtpSettings(getAccount().getUuid(), user.getToken());
    user = TwoFactorAuthRestUtils.enableTwoFactorAuthentication(getAccount().getUuid(), user.getToken(), otpSettings);
    assertThat(user.getEmail()).isNotNull();
    Setup.signOut(user.getUuid(), user.getToken());
    user = TwoFactorAuthRestUtils.retryTwoFaLogin(
        defaultUser, defaultPassword, getAccount().getUuid(), otpSettings.getTotpSecretKey());
    assertThat("bearer token should not be null" + user.getToken()).isNotNull();
    UserRestUtils urUtil = new UserRestUtils();
    List<PublicUser> userList = urUtil.getUserList(user.getToken(), getAccount().getUuid());
    assertThat(userList.size() > 0).isTrue();
    TwoFactorAuthRestUtils.disableTwoFactorAuthentication(getAccount().getUuid(), user.getToken());
    Setup.signOut(user.getUuid(), user.getToken());
    log.info("Disabled 2FA Login");
    user = Setup.loginUser(defaultUser, defaultPassword);
    userList = urUtil.getUserList(user.getToken(), getAccount().getUuid());
    log.info("Getting the User List to ensure 2fa login disabled");
    assertThat(userList.size() > 0).isTrue();
  }
}
