package io.harness.e2e.users;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

import io.harness.category.element.E2ETests;
import io.harness.e2e.AbstractE2ETest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.restutils.UserRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.User;

import java.util.List;

@Slf4j
public class UserTests extends AbstractE2ETest {
  @Test()
  @Owner(emails = "swamy@harness.io", resent = false)
  @Category(E2ETests.class)
  public void listUsers() {
    logger.info("Starting the list users test");
    Account account = this.getAccount();
    UserRestUtils urUtil = new UserRestUtils();
    List<User> userList = urUtil.getUserList(bearerToken, account.getUuid());
    assertNotNull(userList);
    assertTrue(userList.size() > 0);
  }
}
