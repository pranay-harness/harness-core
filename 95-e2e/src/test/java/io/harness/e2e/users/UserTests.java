package io.harness.e2e.users;

import static io.harness.rule.OwnerRule.NATARAJA;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.E2ETests;
import io.harness.e2e.AbstractE2ETest;
import io.harness.rule.Owner;
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
  @Owner(developers = NATARAJA)
  @Category(E2ETests.class)
  public void listUsers() {
    logger.info("Starting the list users test");
    Account account = this.getAccount();
    UserRestUtils urUtil = new UserRestUtils();
    List<User> userList = urUtil.getUserList(bearerToken, account.getUuid());
    assertThat(userList).isNotNull();
    assertThat(userList.size() > 0).isTrue();
  }
}
