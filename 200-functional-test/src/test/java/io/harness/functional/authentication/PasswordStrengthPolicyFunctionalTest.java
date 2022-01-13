/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.authentication;

import static io.harness.rule.OwnerRule.UTKARSH;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.exception.WingsException;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.LoginSettingsUtils;

import software.wings.beans.loginSettings.LoginSettings;
import software.wings.beans.loginSettings.LoginSettingsService;
import software.wings.beans.loginSettings.PasswordStrengthPolicy;

import com.google.inject.Inject;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PasswordStrengthPolicyFunctionalTest extends AbstractFunctionalTest {
  static final boolean PASSWORD_STRENGTH_POLICY_ENABLED = true;
  static final boolean PASSWORD_STRENGTH_POLICY_DISABLED = false;
  static final int MINIMUM_NUMBER_OF_CHARACTERS = 15;
  static final int MINIMUM_NUMBER_OF_UPPERCASE_CHARACTERS = 1;
  static final int MINIMUM_NUMBER_OF_LOWERCASE_CHARACTERS = 1;
  static final int MINIMUM_NUMBER_OF_SPECIAL_CHARACTERS = 1;
  static final int MINIMUM_NUMBER_OF_DIGITS = 1;
  static final int UPDATED_NUMBER_OF_CHARACTERS = 12;
  PasswordStrengthPolicy passwordStrengthPolicy;
  static String LoginSettingsId;

  @Inject private LoginSettingsService loginSettingsService;

  @Test
  @Owner(developers = UTKARSH, intermittent = true)
  @Category(FunctionalTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void TC0_setPasswordPolicy() {
    passwordStrengthPolicy = PasswordStrengthPolicy.builder()
                                 .enabled(PASSWORD_STRENGTH_POLICY_ENABLED)
                                 .minNumberOfCharacters(MINIMUM_NUMBER_OF_CHARACTERS)
                                 .minNumberOfUppercaseCharacters(MINIMUM_NUMBER_OF_UPPERCASE_CHARACTERS)
                                 .minNumberOfLowercaseCharacters(MINIMUM_NUMBER_OF_LOWERCASE_CHARACTERS)
                                 .minNumberOfSpecialCharacters(MINIMUM_NUMBER_OF_SPECIAL_CHARACTERS)
                                 .minNumberOfDigits(MINIMUM_NUMBER_OF_DIGITS)
                                 .build();

    LoginSettings loginSettings =
        LoginSettingsUtils.passwordStrengthPolicyUpdate(bearerToken, getAccount().getUuid(), passwordStrengthPolicy);
    assertThat(loginSettings.getUuid()).isNotNull();
    LoginSettingsId = loginSettings.getUuid();

    PasswordStrengthPolicy passwordStrengthPolicyResponse = loginSettings.getPasswordStrengthPolicy();
    assertThat(PASSWORD_STRENGTH_POLICY_ENABLED).isEqualTo(passwordStrengthPolicyResponse.isEnabled());
    assertThat(MINIMUM_NUMBER_OF_CHARACTERS).isEqualTo(passwordStrengthPolicyResponse.getMinNumberOfCharacters());
    assertThat(MINIMUM_NUMBER_OF_UPPERCASE_CHARACTERS)
        .isEqualTo(passwordStrengthPolicyResponse.getMinNumberOfUppercaseCharacters());
    assertThat(MINIMUM_NUMBER_OF_LOWERCASE_CHARACTERS)
        .isEqualTo(passwordStrengthPolicyResponse.getMinNumberOfLowercaseCharacters());
    assertThat(MINIMUM_NUMBER_OF_DIGITS).isEqualTo(passwordStrengthPolicyResponse.getMinNumberOfDigits());
    assertThat(MINIMUM_NUMBER_OF_SPECIAL_CHARACTERS)
        .isEqualTo(passwordStrengthPolicyResponse.getMinNumberOfSpecialCharacters());
  }

  @Test
  @Owner(developers = UTKARSH, intermittent = true)
  @Category(FunctionalTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void TC1_updatePasswordPolicy() {
    passwordStrengthPolicy = PasswordStrengthPolicy.builder()
                                 .enabled(PASSWORD_STRENGTH_POLICY_ENABLED)
                                 .minNumberOfCharacters(UPDATED_NUMBER_OF_CHARACTERS)
                                 .minNumberOfUppercaseCharacters(MINIMUM_NUMBER_OF_UPPERCASE_CHARACTERS)
                                 .minNumberOfLowercaseCharacters(MINIMUM_NUMBER_OF_LOWERCASE_CHARACTERS)
                                 .minNumberOfSpecialCharacters(MINIMUM_NUMBER_OF_SPECIAL_CHARACTERS)
                                 .minNumberOfDigits(MINIMUM_NUMBER_OF_DIGITS)
                                 .build();

    LoginSettings loginSettings =
        LoginSettingsUtils.passwordStrengthPolicyUpdate(bearerToken, getAccount().getUuid(), passwordStrengthPolicy);
    assertThat(LoginSettingsId).isEqualTo(loginSettings.getUuid());

    PasswordStrengthPolicy passwordStrengthPolicyResponse = loginSettings.getPasswordStrengthPolicy();
    assertThat(PASSWORD_STRENGTH_POLICY_ENABLED).isEqualTo(passwordStrengthPolicyResponse.isEnabled());
    assertThat(UPDATED_NUMBER_OF_CHARACTERS).isEqualTo(passwordStrengthPolicyResponse.getMinNumberOfCharacters());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(FunctionalTests.class)
  public void TC2_changePasswordSuccess() {
    final String TEST_PASSWORD = "Helloafsddsfasdsas1@";
    assertThat(true).isEqualTo(loginSettingsService.verifyPasswordStrength(getAccount(), TEST_PASSWORD.toCharArray()));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(FunctionalTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void TC3_changePasswordFailure() {
    final String TEST_PASSWORD = "Helloafsddsfasdsas1";
    try {
      loginSettingsService.verifyPasswordStrength(getAccount(), TEST_PASSWORD.toCharArray());
      fail("Password should not have been accepted");
    } catch (WingsException e) {
      assertThat(
          String.format("io.harness.exception.WingsException: Password validation checks failed for account :[%s].",
              getAccount().getUuid()))
          .isEqualTo(String.valueOf(e));
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(FunctionalTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void TC4_disablePasswordPolicy() {
    passwordStrengthPolicy = PasswordStrengthPolicy.builder().enabled(PASSWORD_STRENGTH_POLICY_DISABLED).build();

    LoginSettings loginSettings =
        LoginSettingsUtils.passwordStrengthPolicyUpdate(bearerToken, getAccount().getUuid(), passwordStrengthPolicy);
    assertThat(LoginSettingsId).isEqualTo(loginSettings.getUuid());

    PasswordStrengthPolicy passwordStrengthPolicyResponse = loginSettings.getPasswordStrengthPolicy();
    assertThat(PASSWORD_STRENGTH_POLICY_DISABLED).isEqualTo(passwordStrengthPolicyResponse.isEnabled());
  }
}
