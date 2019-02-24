package io.harness.functional.users;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import io.harness.RestUtils.GuerillaMailUtil;
import io.harness.RestUtils.HTMLUtils;
import io.harness.RestUtils.UserRestUtil;
import io.harness.category.element.FunctionalTests;
import io.harness.framework.Retry;
import io.harness.framework.Setup;
import io.harness.framework.constants.UserConstants;
import io.harness.framework.email.EmailMetaData;
import io.harness.framework.email.GuerillaEmailDetails;
import io.harness.framework.email.GuerillaEmailInfo;
import io.harness.framework.email.GuerillaIndividualEmail;
import io.harness.framework.matchers.EmailMatcher;
import io.harness.functional.AbstractFunctionalTest;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
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
  final int MAX_RETRIES = 5;
  final int DELAY_IN_MS = 3000;
  final Retry<Object> retry = new Retry<>(MAX_RETRIES, DELAY_IN_MS);
  final String EXPECTED_SUBJECT = "You are invited to join Harness at Harness platform";
  UserRestUtil urUtil = new UserRestUtil();
  GuerillaMailUtil gmailUtil = new GuerillaMailUtil();
  HTMLUtils htmlUtils = new HTMLUtils();

  @Test()
  @Category(FunctionalTests.class)
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
  public void verifySignupEmailDelivery() throws IOException, MessagingException {
    Account account = this.getAccount();
    GuerillaEmailInfo emailInfo = gmailUtil.refreshAndGetNewEmail();
    logger.info(emailInfo.getEmailAddr());
    List<UserInvite> userInvitationList = urUtil.inviteUser(account, emailInfo.getEmailAddr());
    assertNotNull(userInvitationList);
    assertTrue(userInvitationList.size() == 1);
    UserInvite userInvite = userInvitationList.get(0);
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
  }

  @Test()
  @Category(FunctionalTests.class)
  public void userRegistrationCompletionTest() {
    Account account = this.getAccount();
    GuerillaEmailInfo emailInfo = gmailUtil.refreshAndGetNewEmail();
    logger.info(emailInfo.getEmailAddr());
    List<UserInvite> userInvitationList = urUtil.inviteUser(account, emailInfo.getEmailAddr());
    assertNotNull(userInvitationList);
    assertTrue(userInvitationList.size() == 1);
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
    String bearerToken = Setup.getAuthToken(completed.getEmail(), UserConstants.DEFAULT_PASSWORD);
    assertNotNull("Bearer Token not successfully provided", bearerToken);
    int statusCode = Setup.signOut(completed.getUuid(), bearerToken);
    assertTrue(statusCode == HttpStatus.SC_OK);
    logger.info("Successfully completed user registration email");
    assertTrue(gmailUtil.forgetEmailId(emailInfo.getSidToken()));
  }
}
