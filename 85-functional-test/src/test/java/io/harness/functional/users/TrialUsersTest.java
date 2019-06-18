package io.harness.functional.users;

import static io.harness.rule.OwnerRule.SWAMY;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.constants.UserConstants;
import io.harness.testframework.framework.email.mailinator.MailinatorMessageDetails;
import io.harness.testframework.framework.email.mailinator.MailinatorMetaMessage;
import io.harness.testframework.framework.utils.TestUtils;
import io.harness.testframework.restutils.HTMLUtils;
import io.harness.testframework.restutils.MailinatorRestUtils;
import io.harness.testframework.restutils.UserRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.SettingAttribute;
import software.wings.beans.UserInvite;

import java.io.IOException;
import javax.mail.MessagingException;
@Slf4j
public class TrialUsersTest extends AbstractFunctionalTest {
  @Inject private SettingGenerator settingGenerator;
  @Inject private OwnerManager ownerManager;

  Owners owners;
  final Seed seed = new Seed(0);

  final int MAX_RETRIES = 20;
  final int DELAY_IN_MS = 6000;
  final Retry<Object> retry = new Retry<>(MAX_RETRIES, DELAY_IN_MS);
  final String EXPECTED_SUBJECT = "Verify Your Email for Harness Trial";
  final String EXPECTED_RESET_PWD_SUBJECT = "Reset your HARNESS PLATFORM password";

  @Before
  public void trialUserTestSetup() {
    owners = ownerManager.create();
    SettingAttribute emailSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.PAID_EMAIL_SMTP_CONNECTOR);
    assertThat(emailSettingAttribute).isNotNull();
    logger.info("Setup completed successfully");
  }

  @Test()
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  @Ignore("We have migrated to new API's. Ignoring as per discussion with Swamy and Nataraja")
  public void verifyTrialUserSignup() throws IOException, MessagingException {
    String domainName = "@harness.mailinator.com";
    String emailId = TestUtils.generateUniqueInboxId();
    String fullEmailId = emailId + domainName;
    logger.info("Generating the email id for trial user : " + fullEmailId);
    Boolean isTrialInviteDone = UserRestUtils.createTrialInvite(fullEmailId);
    assertTrue(isTrialInviteDone);
    // Verify if email is sent, received and has signup link
    // Email check will run every 6 seconds upto 2 mins to see if email is delivered.
    logger.info("Attempting to retrieve signup mail from inbox : " + emailId);
    MailinatorMetaMessage message = MailinatorRestUtils.retrieveMessageFromInbox(emailId, EXPECTED_SUBJECT);
    logger.info("Signup mail retrieved");
    logger.info("Reading the retrieved email");
    String emailFetchId = message.getId();
    MailinatorMessageDetails messageDetails = MailinatorRestUtils.readEmail(emailId, emailFetchId);
    assertNotNull(messageDetails);

    String inviteUrl =
        HTMLUtils.retrieveInviteUrlFromEmail(messageDetails.getData().getParts().get(0).getBody(), "VERIFY EMAIL");
    assertNotNull(inviteUrl);
    assertTrue(StringUtils.isNotBlank(inviteUrl));
    String actualUrl = TestUtils.getInviteIdFromUrl(inviteUrl);
    assertNotNull(actualUrl);
    assertTrue(actualUrl.contains("inviteId="));
    String inviteId = actualUrl.split("inviteId=")[1];
    logger.info("Verified the presence of trial user signup URL");
    messageDetails = MailinatorRestUtils.deleteEmail(emailId, emailFetchId);
    logger.info("Email deleted for the inbox : " + emailId);
    assertNotNull(messageDetails.getAdditionalProperties());
    assertNotNull(messageDetails.getAdditionalProperties().containsKey("status"));
    assertTrue(messageDetails.getAdditionalProperties().get("status").toString().equals("ok"));

    // Complete signup using the API
    logger.info("Entering trial user invite validation");
    UserInvite invite = new UserInvite();
    invite.setEmail(fullEmailId);
    invite.setName(emailId.replace("@harness.mailinator.com", ""));
    invite.setUuid(inviteId);
    String accountName = TestUtils.generateRandomUUID();
    String companyName = TestUtils.generateRandomUUID();

    UserInvite completed = UserRestUtils.completeTrialUserSignup(bearerToken, accountName, companyName, invite);
    logger.info(accountName + ":" + companyName + ":" + invite.getEmail());
    assertNotNull(completed);
    assertTrue("Error: Completion is false after signup", completed.isCompleted());

    String bearerToken = Setup.getAuthToken(completed.getEmail(), UserConstants.DEFAULT_PASSWORD);
    assertNotNull("Bearer Token not successfully provided", bearerToken);
    int statusCode = Setup.signOut(completed.getUuid(), bearerToken);
    assertTrue(statusCode == HttpStatus.SC_OK);
    logger.info("Logged out of trial user");
    logger.info("Looking for the delegate not available Alert");

    // Verify the reset password.
    UserRestUtils.sendResetPasswordMail(fullEmailId);
    logger.info("Attempting to retrieve reset password mail from inbox : " + fullEmailId);
    message = MailinatorRestUtils.retrieveMessageFromInbox(emailId, EXPECTED_RESET_PWD_SUBJECT);
    logger.info("Reset password mail retrieved");
    logger.info("Reading the retrieved email");
    emailFetchId = message.getId();
    messageDetails = MailinatorRestUtils.readEmail(fullEmailId, emailFetchId);
    assertNotNull(messageDetails);
    String resetUrl =
        HTMLUtils.retrieveResetUrlFromEmail(messageDetails.getData().getParts().get(0).getBody(), "RESET PASSWORD");
    assertTrue(StringUtils.isNotBlank(resetUrl));
    logger.info("Email read and Reset password URL is available for user password reset");
    UserRestUtils.resetPasswordWith(TestUtils.getResetTokenFromUrl(resetUrl), UserConstants.RESET_PASSWORD);
    // Verify if the user can login through the reset password
    bearerToken = Setup.getAuthToken(completed.getEmail(), UserConstants.RESET_PASSWORD);
    assertNotNull("Bearer Token not successfully provided", bearerToken);
    statusCode = Setup.signOut(completed.getUuid(), bearerToken);
    assertTrue(statusCode == org.apache.http.HttpStatus.SC_OK);
    // Delete Email
    messageDetails = null;
    messageDetails = MailinatorRestUtils.deleteEmail(fullEmailId, emailFetchId);
    logger.info("Email deleted for the inbox : " + fullEmailId);
    assertNotNull(messageDetails.getAdditionalProperties());
    assertNotNull(messageDetails.getAdditionalProperties().containsKey("status"));
    assertTrue(messageDetails.getAdditionalProperties().get("status").toString().equals("ok"));
    logger.info("All validation completed");
    logger.info("All validation for reset also done");
  }
}
