package io.harness.functional.users;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.beans.UserInvite;

import java.io.IOException;
import java.util.List;
import javax.mail.MessagingException;
@Slf4j
public class UserTest extends AbstractFunctionalTest {
  @Inject private SettingGenerator settingGenerator;
  @Inject private OwnerManager ownerManager;

  final int MAX_RETRIES = 20;
  final int DELAY_IN_MS = 6000;
  final Retry<Object> retry = new Retry<>(MAX_RETRIES, DELAY_IN_MS);
  final String EXPECTED_SUBJECT = "You are invited to join Harness at Harness platform";
  final String EXPECTED_RESET_PWD_SUBJECT = "Reset your HARNESS PLATFORM password";
  final Seed seed = new Seed(0);
  Owners owners;

  @Before
  public void userTestSetup() {
    owners = ownerManager.create();
    SettingAttribute emailSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, Settings.PAID_EMAIL_SMTP_CONNECTOR);
    assertThat(emailSettingAttribute).isNotNull();
    logger.info("Setup completed successfully");
  }

  @Test()
  @Owner(emails = "swamy@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void listUsers() {
    logger.info("Starting the list users test");
    Account account = this.getAccount();
    UserRestUtils urUtil = new UserRestUtils();
    List<User> userList = urUtil.getUserList(bearerToken, account.getUuid());
    assertNotNull(userList);
    assertTrue(userList.size() > 0);
  }

  @Test()
  @Owner(emails = "swamy@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void testUserInvite() throws IOException, MessagingException {
    Account account = this.getAccount();
    String domainName = "@harness.mailinator.com";
    String emailId = TestUtils.generateUniqueInboxId();
    List<UserInvite> userInvitationList = UserRestUtils.inviteUser(account, bearerToken, emailId + domainName);
    assertNotNull(userInvitationList);
    assertTrue(userInvitationList.size() == 1);
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
        HTMLUtils.retrieveInviteUrlFromEmail(messageDetails.getData().getParts().get(0).getBody(), "SIGN UP");
    assertNotNull(inviteUrl);
    assertTrue(StringUtils.isNotBlank(inviteUrl));
    logger.info("Email read and Signup URL is available for user signup");

    messageDetails = null;
    messageDetails = MailinatorRestUtils.deleteEmail(emailId, emailFetchId);
    logger.info("Email deleted for the inbox : " + emailId);
    assertNotNull(messageDetails.getAdditionalProperties());
    assertNotNull(messageDetails.getAdditionalProperties().containsKey("status"));
    assertTrue(messageDetails.getAdditionalProperties().get("status").toString().equals("ok"));

    // Complete registration using the API
    logger.info("Entering user invite validation");
    UserInvite incomplete = userInvitationList.get(0);
    UserInvite completed = UserRestUtils.completeUserRegistration(account, bearerToken, incomplete);
    assertNotNull(completed);
    assertFalse("Error : Agreement is true before signup", incomplete.isAgreement());
    assertFalse("Error : Completion is true before signup", incomplete.isCompleted());
    assertTrue("Error: Completion is false after signup", completed.isCompleted());
    // Assert.assertTrue("Error : Agreement is false after signup",completed.isAgreement());
    logger.info(incomplete.getAccountId() + ":" + incomplete.getEmail());
    assertTrue(incomplete.getEmail().equals(completed.getEmail()));
    assertTrue(incomplete.getName().equals(completed.getName()));
    assertTrue(incomplete.getAccountId().equals(completed.getAccountId()));
    // Verify if the signed-up user can login
    String bearerToken = Setup.getAuthToken(completed.getEmail(), UserConstants.DEFAULT_PASSWORD);
    assertNotNull("Bearer Token not successfully provided", bearerToken);
    int statusCode = Setup.signOut(completed.getUuid(), bearerToken);
    assertTrue(statusCode == HttpStatus.SC_OK);

    // Verify user can reset the password
    emailId = emailId + domainName;
    UserRestUtils.sendResetPasswordMail(emailId);
    logger.info("Attempting to retrieve reset password mail from inbox : " + emailId);
    message = MailinatorRestUtils.retrieveMessageFromInbox(emailId, EXPECTED_RESET_PWD_SUBJECT);
    logger.info("Reset password mail retrieved");
    logger.info("Reading the retrieved email");
    emailFetchId = message.getId();
    messageDetails = MailinatorRestUtils.readEmail(emailId, emailFetchId);
    assertNotNull(messageDetails);
    String resetUrl =
        HTMLUtils.retrieveResetUrlFromEmail(messageDetails.getData().getParts().get(0).getBody(), "RESET PASSWORD");
    assertTrue(StringUtils.isNotBlank(resetUrl));
    logger.info(""
        + " URL is available for user password reset");
    UserRestUtils.resetPasswordWith(TestUtils.getResetTokenFromUrl(resetUrl), UserConstants.RESET_PASSWORD);
    // Verify if the user can login through the reset password
    bearerToken = Setup.getAuthToken(completed.getEmail(), UserConstants.RESET_PASSWORD);
    assertNotNull("Bearer Token not successfully provided", bearerToken);
    statusCode = Setup.signOut(completed.getUuid(), bearerToken);
    assertTrue(statusCode == HttpStatus.SC_OK);
    logger.info("All validation completed");
    logger.info("All validation for reset also done");
  }
}
