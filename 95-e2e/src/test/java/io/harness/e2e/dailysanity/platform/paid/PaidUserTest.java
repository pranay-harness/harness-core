package io.harness.e2e.dailysanity.platform.paid;

import static io.harness.rule.OwnerRule.NATARAJA;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.E2ETests;
import io.harness.e2e.AbstractE2ETest;
import io.harness.rule.Owner;
import io.harness.testframework.framework.utils.TestUtils;
import io.harness.testframework.framework.utils.UserUtils;
import io.harness.testframework.restutils.UserRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.PublicUser;
import software.wings.beans.UserInvite;

import java.io.IOException;
import java.util.List;
import javax.mail.MessagingException;

@Slf4j
public class PaidUserTest extends AbstractE2ETest {
  final String EXPECTED_SUBJECT = "You have been invited to join the Automation One account at Harness";

  @Test()
  @Owner(developers = NATARAJA)
  @Category(E2ETests.class)
  public void listUsers() {
    logger.info("Starting the list users test");
    UserRestUtils urUtil = new UserRestUtils();
    List<PublicUser> userList = urUtil.getUserList(bearerToken, getAccount().getUuid());
    assertThat(userList).isNotNull();
    assertThat(userList.size() > 0).isTrue();
  }

  @Owner(developers = NATARAJA)
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
