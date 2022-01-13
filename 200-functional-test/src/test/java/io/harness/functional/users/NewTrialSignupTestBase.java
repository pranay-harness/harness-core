/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.users;

import static io.harness.rule.OwnerRule.RAMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.rule.Owner;
import io.harness.testframework.framework.utils.TestUtils;
import io.harness.testframework.restutils.UserRestUtils;

import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.beans.UserInvite;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.experimental.categories.Category;

/**
 * @author rktummala on 04/04/19
 */
@Slf4j
public class NewTrialSignupTestBase extends AbstractFunctionalTest {
  @Inject private SettingGenerator settingGenerator;
  @Inject private OwnerManager ownerManager;

  Owners owners;
  final Seed seed = new Seed(0);

  UserRestUtils urUtil = new UserRestUtils();
  TestUtils testUtils = new TestUtils();

  @Before
  public void trialUserTestSetup() {
    owners = ownerManager.create();
    SettingAttribute emailSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.PAID_EMAIL_SMTP_CONNECTOR);
    assertThat(emailSettingAttribute).isNotNull();
    log.info("Setup completed successfully");
  }

  @Owner(developers = RAMA, intermittent = true)
  @Category(FunctionalTests.class)
  public void verifyTrialUserSignup() {
    UserInvite invite = constructInvite("password");
    Boolean isTrialInviteDone = urUtil.createNewTrialInvite(invite);
    assertThat(isTrialInviteDone).isTrue();

    User user = urUtil.completeNewTrialUserSignup(bearerToken, invite.getUuid());
    assertThat(user).isNotNull();
    assertThat(invite.getEmail()).isEqualTo(user.getEmail());
  }

  @Owner(developers = RAMA, intermittent = true)
  @Category(FunctionalTests.class)
  public void verifyTrialUserSignupInvalidPassword() {
    UserInvite invite = constructInvite("pass");

    Boolean isTrialInviteDone = urUtil.createNewTrialInvite(invite);
    assertThat(isTrialInviteDone).isNull();
  }

  private UserInvite constructInvite(String password) {
    String domainName = "@harness.mailinator.com";
    String emailId = testUtils.generateUniqueInboxId();
    String fullEmailId = emailId + domainName;
    log.info("Generating the email id for trial user : " + fullEmailId);
    String inviteId = UUIDGenerator.generateUuid();

    UserInvite invite = new UserInvite();
    invite.setEmail(fullEmailId);
    invite.setName(emailId.replace("@harness.mailinator.com", ""));
    invite.setUuid(inviteId);
    String accountName = TestUtils.generateRandomUUID();
    String companyName = TestUtils.generateRandomUUID();
    invite.setAccountName(accountName);
    invite.setCompanyName(companyName);
    invite.setPassword(password.toCharArray());
    return invite;
  }
}
