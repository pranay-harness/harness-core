package io.harness.e2e.dailysanity.platform.paid;

import static io.harness.rule.OwnerRule.NATARAJA;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.E2ETests;
import io.harness.e2e.AbstractE2ETest;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.email.mailinator.MailinatorMessageDetails;
import io.harness.testframework.framework.email.mailinator.MailinatorMetaMessage;
import io.harness.testframework.framework.utils.TestUtils;
import io.harness.testframework.framework.utils.UserUtils;
import io.harness.testframework.restutils.HTMLUtils;
import io.harness.testframework.restutils.MailinatorRestUtils;
import io.harness.testframework.restutils.UserRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.User;
import software.wings.beans.UserInvite;

import java.io.IOException;
import javax.mail.MessagingException;

@Slf4j
public class NewTrialAccountSignup extends AbstractE2ETest {
  @Test()
  @Owner(developers = NATARAJA, intermittent = false)
  @Category(E2ETests.class)
  public void testTrialSignUp() throws IOException, MessagingException {
    String testPassword = new ScmSecret().decryptToString(new SecretName("user_readonly_password"));
    String domainName = "@harness.mailinator.com";
    String emailId = TestUtils.generateUniqueInboxId();
    String fullEmailId = emailId + domainName;
    logger.info("Generating the email id for trial user : " + fullEmailId);
    UserInvite userInvite = UserUtils.getTrialSignUpInvite(fullEmailId, testPassword);
    Boolean isTrialInviteDone = UserRestUtils.createNewTrialInvite(userInvite);
    assertThat(isTrialInviteDone).isTrue();
    MailinatorMetaMessage message =
        MailinatorRestUtils.retrieveMessageFromInbox(emailId, "Verify Your Email for Harness Trial");
    logger.info("Verify email mail retrieved");
    logger.info("Reading the retrieved email");
    String emailFetchId = message.getId();
    MailinatorMessageDetails messageDetails = MailinatorRestUtils.readEmail(emailId, emailFetchId);
    assertThat(messageDetails).isNotNull();
    String inviteUrl =
        HTMLUtils.retrieveInviteUrlFromEmail(messageDetails.getData().getParts().get(0).getBody(), "VERIFY EMAIL");
    assertThat(inviteUrl).isNotNull();
    assertThat(StringUtils.isNotBlank(inviteUrl)).isTrue();
    logger.info("Email read and Signup URL is available for user signup: " + inviteUrl);

    messageDetails = null;
    messageDetails = MailinatorRestUtils.deleteEmail(emailId, emailFetchId);
    logger.info("Email deleted for the inbox : " + emailId);
    assertThat(messageDetails.getAdditionalProperties()).isNotNull();
    assertThat(messageDetails.getAdditionalProperties().containsKey("status")).isNotNull();
    assertThat(messageDetails.getAdditionalProperties().get("status").toString().equals("ok")).isTrue();

    String inviteIdFromUrl = TestUtils.validateAndGetTrialUserInviteFromUrl(inviteUrl);
    User user = UserRestUtils.completeNewTrialUserSignup(bearerToken, inviteIdFromUrl);
    assertThat(user).isNotNull();
    assertThat(userInvite.getEmail()).isEqualTo("User login should be successfull after SignUp", user.getEmail());
    Setup.signOut(user.getUuid(), user.getToken());
    User newLogedUser = Setup.loginUser(fullEmailId, testPassword);
    assertThat(newLogedUser).isNotNull();
    assertThat(user.getToken()).isNotNull();
    assertThat(fullEmailId)
        .isEqualTo("Login should work as expected after new account signup", newLogedUser.getEmail());
  }
}
