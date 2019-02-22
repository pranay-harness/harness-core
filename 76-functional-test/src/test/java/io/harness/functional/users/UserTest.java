package io.harness.functional.users;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

import io.harness.RestUtils.GuerillaMailUtil;
import io.harness.RestUtils.UserRestUtil;
import io.harness.category.element.FunctionalTests;
import io.harness.framework.GuerillaEmailInfo;
import io.harness.functional.AbstractFunctionalTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.User;

import java.util.List;

public class UserTest extends AbstractFunctionalTest {
  private static final Logger logger = LoggerFactory.getLogger(UserTest.class);

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

  // TODO: Test implementation under progress. Requires email server verification tasks.
  @Test()
  @Ignore
  @Category(FunctionalTests.class)
  public void sendInvite() {
    Account account = this.getAccount();
    UserRestUtil urUtil = new UserRestUtil();
    GuerillaEmailInfo emailInfo = new GuerillaMailUtil().getNewEmailId();
    logger.info(emailInfo.getEmailAddr());
  }
}
