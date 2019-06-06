package io.harness.e2e.dailysanity.platform;

import static io.harness.rule.OwnerRule.NATARAJA;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

import io.harness.category.element.E2ETests;
import io.harness.e2e.AbstractE2ETest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.utils.TestUtils;
import io.harness.testframework.framework.utils.UserUtils;
import io.harness.testframework.restutils.UserRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.UserInvite;

import java.io.IOException;
import java.util.List;
import javax.mail.MessagingException;

@Slf4j
public class PaidUserTest extends AbstractE2ETest {
  final String EXPECTED_SUBJECT = "You have been invited to join the Automation One account at Harness";

  @Test()
  @Owner(emails = NATARAJA, resent = false)
  @Category(E2ETests.class)
  public void listUsers() {
    logger.info("Starting the list users test");
    UserRestUtils urUtil = new UserRestUtils();
    List<User> userList = urUtil.getUserList(bearerToken, getAccount().getUuid());
    assertNotNull(userList);
    assertTrue(userList.size() > 0);
  }

  @Test()
  @Owner(emails = NATARAJA, resent = false)
  @Category(E2ETests.class)
  public void testUserInvite() throws IOException, MessagingException {
    Account account = getAccount();
    String domainName = "@harness.mailinator.com";
    String emailId = TestUtils.generateUniqueInboxId();
    List<UserInvite> userInvitationList =
        UserUtils.inviteUserAndValidateInviteMail(account, bearerToken, emailId, domainName, EXPECTED_SUBJECT);
    UserInvite completed = UserUtils.completeSignupAndValidateLogin(account, bearerToken, userInvitationList);
    UserUtils.resetPasswordAndValidateLogin(completed, emailId, domainName);
  }
}
