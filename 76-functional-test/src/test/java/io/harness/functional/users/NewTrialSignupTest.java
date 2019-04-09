package io.harness.functional.users;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.inject.Inject;

import io.harness.RestUtils.UserRestUtil;
import io.harness.Utils.TestUtils;
import io.harness.category.element.FunctionalTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.beans.UserInvite;

/**
 * @author rktummala on 04/04/19
 */
public class NewTrialSignupTest extends AbstractFunctionalTest {
  @Inject private SettingGenerator settingGenerator;
  @Inject private OwnerManager ownerManager;

  Owners owners;
  final Seed seed = new Seed(0);

  private static final Logger logger = LoggerFactory.getLogger(NewTrialSignupTest.class);
  UserRestUtil urUtil = new UserRestUtil();
  TestUtils testUtils = new TestUtils();

  @Before
  public void trialUserTestSetup() {
    owners = ownerManager.create();
    SettingAttribute emailSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.PAID_EMAIL_SMTP_CONNECTOR);
    assertThat(emailSettingAttribute).isNotNull();
    logger.info("Setup completed successfully");
  }

  @Test()
  @Owner(emails = "rama@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void verifyTrialUserSignup() {
    String domainName = "@harness.mailinator.com";
    String emailId = testUtils.generateUniqueInboxId();
    String fullEmailId = emailId + domainName;
    logger.info("Generating the email id for trial user : " + fullEmailId);
    String inviteId = UUIDGenerator.generateUuid();
    String password = "password";
    UserInvite invite = new UserInvite();
    invite.setEmail(fullEmailId);
    invite.setName(emailId.replace("@harness.mailinator.com", ""));
    invite.setUuid(inviteId);
    String accountName = TestUtils.generateRandomUUID();
    String companyName = TestUtils.generateRandomUUID();
    invite.setAccountName(accountName);
    invite.setCompanyName(companyName);
    invite.setPassword(password.toCharArray());

    Boolean isTrialInviteDone = urUtil.createNewTrialInvite(invite);
    assertTrue(isTrialInviteDone);

    User user = urUtil.completeNewTrialUserSignup(invite.getUuid());
    assertNotNull(user);
    assertEquals(user.getEmail(), fullEmailId);
  }
}
