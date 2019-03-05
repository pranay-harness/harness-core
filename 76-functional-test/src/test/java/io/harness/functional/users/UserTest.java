package io.harness.functional.users;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import io.harness.RestUtils.GuerillaMailUtil;
import io.harness.RestUtils.HTMLUtils;
import io.harness.RestUtils.MailinatorRestUtils;
import io.harness.RestUtils.UserRestUtil;
import io.harness.Utils.TestUtils;
import io.harness.category.element.FunctionalTests;
import io.harness.framework.Retry;
import io.harness.framework.Setup;
import io.harness.framework.constants.UserConstants;
import io.harness.framework.email.EmailMetaData;
import io.harness.framework.email.GuerillaEmailDetails;
import io.harness.framework.email.GuerillaEmailInfo;
import io.harness.framework.email.GuerillaIndividualEmail;
import io.harness.framework.email.mailinator.MailinatorMessageDetails;
import io.harness.framework.email.mailinator.MailinatorMetaMessage;
import io.harness.framework.matchers.EmailMatcher;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.UserInvite;

import java.io.IOException;
import java.util.List;
import javax.mail.MessagingException;

public class UserTest extends AbstractFunctionalTest {
  private static final Logger logger = LoggerFactory.getLogger(UserTest.class);
  final int MAX_RETRIES = 20;
  final int DELAY_IN_MS = 6000;
  final Retry<Object> retry = new Retry<>(MAX_RETRIES, DELAY_IN_MS);
  final String EXPECTED_SUBJECT = "You are invited to join Harness at Harness platform";
  UserRestUtil urUtil = new UserRestUtil();
  GuerillaMailUtil gmailUtil = new GuerillaMailUtil();
  MailinatorRestUtils mailinatorRestUtils = new MailinatorRestUtils();
  HTMLUtils htmlUtils = new HTMLUtils();
  TestUtils testUtils = new TestUtils();

  @Test()
  @Category(FunctionalTests.class)
  @Owner(emails = "swamy@harness.io")
  public void listUsers() {
    logger.info("Starting the list users test");
    Account account = this.getAccount();
    UserRestUtil urUtil = new UserRestUtil();
    List<User> userList = urUtil.getUserList(account);
    assertNotNull(userList);
    assertTrue(userList.size() > 0);
  }

  @Test()
  @Category(FunctionalTests.class)
  @Owner(emails = "swamy@harness.io")
  @Ignore("Ignoring User Invite test until Rishi confirms whether we should use guerilla email or not")
  public void verifyUserInvite() throws IOException, MessagingException {
    Account account = this.getAccount();
    GuerillaEmailInfo emailInfo = gmailUtil.refreshAndGetNewEmail();
    logger.info(emailInfo.getEmailAddr());
    logger.info(emailInfo.getSidToken());
    List<UserInvite> userInvitationList = urUtil.inviteUser(account, emailInfo.getEmailAddr());
    assertNotNull(userInvitationList);
    assertTrue(userInvitationList.size() == 1);
    // Verify if email is sent, received and has signup link
    // Email check will run every 6 seconds upto 2 mins to see if email is delivered.
    GuerillaEmailDetails gmailDetails = (GuerillaEmailDetails) retry.executeWithRetry(
        () -> gmailUtil.getAllEmailList(emailInfo.getSidToken()), new EmailMatcher<>(), EXPECTED_SUBJECT);
    final EmailMetaData[] emailToBeFetched = {null};
    gmailDetails.getList().forEach(email -> {
      if (email.getMailSubject().equals(EXPECTED_SUBJECT)) {
        emailToBeFetched[0] = email;
      }
    });
    GuerillaIndividualEmail email =
        gmailUtil.fetchEmail(emailInfo.getSidToken(), String.valueOf(emailToBeFetched[0].getMailId()));
    String inviteUrl = htmlUtils.retrieveInviteUrlFromEmail(email.getMailBody());
    assertTrue(StringUtils.isNotBlank(inviteUrl));
    logger.info("Successfully completed signup email delivery test");
    assertTrue(gmailUtil.forgetEmailId(emailInfo.getSidToken()));
    // Complete registration using the API
    UserInvite incomplete = userInvitationList.get(0);
    UserInvite completed = urUtil.completeUserRegistration(account, incomplete);
    assertNotNull(completed);
    assertFalse("Error : Agreement is true before signup", incomplete.isAgreement());
    assertFalse("Error : Completion is true before signup", incomplete.isCompleted());
    assertTrue("Error: Completion is false after signup", completed.isCompleted());
    // Assert.assertTrue("Error : Agreement is false after signup",completed.isAgreement());
    assertTrue(incomplete.getEmail().equals(completed.getEmail()));
    assertTrue(incomplete.getName().equals(completed.getName()));
    assertTrue(incomplete.getAccountId().equals(completed.getAccountId()));
    // Verify if the signed-up user can login
    String bearerToken = Setup.getAuthToken(completed.getEmail(), UserConstants.DEFAULT_PASSWORD);
    assertNotNull("Bearer Token not successfully provided", bearerToken);
    int statusCode = Setup.signOut(completed.getUuid(), bearerToken);
    assertTrue(statusCode == HttpStatus.SC_OK);
    logger.info("Successfully completed user registration email");
  }

  @Test()
  @Category(FunctionalTests.class)
  @Owner(emails = "swamy@harness.io")
  public void testUserInvite() throws IOException, MessagingException {
    Account account = this.getAccount();
    String domainName = "@swamy-harness.mailinator.com";
    String emailId = testUtils.generateUniqueInboxId();
    List<UserInvite> userInvitationList = urUtil.inviteUser(account, emailId + domainName);
    assertNotNull(userInvitationList);
    assertTrue(userInvitationList.size() == 1);
    // Verify if email is sent, received and has signup link
    // Email check will run every 6 seconds upto 2 mins to see if email is delivered.
    logger.info("Attempting to retrieve signup mail from inbox");
    MailinatorMetaMessage message = mailinatorRestUtils.retrieveMessageFromInbox(emailId, EXPECTED_SUBJECT);
    logger.info("Signup mail retrieved");
    logger.info("Reading the retrieved email");
    String emailFetchId = message.getId();
    MailinatorMessageDetails messageDetails = mailinatorRestUtils.readEmail(emailId, emailFetchId);
    assertNotNull(messageDetails);
    String inviteUrl = htmlUtils.retrieveInviteUrlFromEmail(messageDetails.getData().getParts().get(0).getBody());
    assertNotNull(inviteUrl);
    assertTrue(StringUtils.isNotBlank(inviteUrl));
    logger.info("Email read and Signup URL is available for user signup");

    messageDetails = null;
    messageDetails = mailinatorRestUtils.deleteEmail(emailId, emailFetchId);
    logger.info("Email deleted for the inbox : " + emailId);
    assertNotNull(messageDetails.getAdditionalProperties());
    assertNotNull(messageDetails.getAdditionalProperties().containsKey("status"));
    assertTrue(messageDetails.getAdditionalProperties().get("status").toString().equals("ok"));

    // Complete registration using the API
    logger.info("Entering user invite validation");
    UserInvite incomplete = userInvitationList.get(0);
    UserInvite completed = urUtil.completeUserRegistration(account, incomplete);
    assertNotNull(completed);
    assertFalse("Error : Agreement is true before signup", incomplete.isAgreement());
    assertFalse("Error : Completion is true before signup", incomplete.isCompleted());
    assertTrue("Error: Completion is false after signup", completed.isCompleted());
    // Assert.assertTrue("Error : Agreement is false after signup",completed.isAgreement());
    assertTrue(incomplete.getEmail().equals(completed.getEmail()));
    assertTrue(incomplete.getName().equals(completed.getName()));
    assertTrue(incomplete.getAccountId().equals(completed.getAccountId()));
    // Verify if the signed-up user can login
    String bearerToken = Setup.getAuthToken(completed.getEmail(), UserConstants.DEFAULT_PASSWORD);
    assertNotNull("Bearer Token not successfully provided", bearerToken);
    int statusCode = Setup.signOut(completed.getUuid(), bearerToken);
    assertTrue(statusCode == HttpStatus.SC_OK);
    logger.info("All validation completed");
  }
}
