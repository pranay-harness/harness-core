package io.harness.functional.rbac;

import static io.harness.rule.OwnerRule.SWAMY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.utils.AccessManagementUtils;
import io.harness.testframework.framework.utils.UserGroupUtils;
import io.harness.testframework.framework.utils.UserUtils;
import io.harness.testframework.restutils.UserGroupRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.User;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.UserGroup;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AccessManagementROTest extends AbstractFunctionalTest {
  final String RBAC_USER = "rbac1@harness.io";
  String readOnlyUserid;
  UserGroup userGroup;

  @Before
  public void rbacSetup() {
    logger.info("Running RBAC setup");
    createUserGroup();
  }

  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void accessManagementNoPermissionTestForList() {
    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(RBAC_USER, "rbac1");
    logger.info("List the users using ReadOnly permission");
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/users")
                   .getStatusCode()
        == HttpStatus.SC_OK);
    logger.info("List users failed with Bad request as expected");
    logger.info("List the user groups using ReadOnly permission");
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/userGroups")
                   .getStatusCode()
        == HttpStatus.SC_OK);
    AccessManagementUtils.runNoAccessTest(getAccount(), roBearerToken, readOnlyUserid);
  }

  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForUser() {
    final String READ_ONLY_USER = "rbac1@harness.io";
    String email = "testemail@harness.mailinator.com";
    String password = "rbac1";
    AccessManagementUtils.runUserPostFailTest(getAccount(), bearerToken, READ_ONLY_USER, email, password);
  }

  private void createUserGroup() {
    logger.info("Starting with the ReadOnly Test");
    User readOnlyUser = UserUtils.getUser(bearerToken, getAccount().getUuid(), RBAC_USER);
    assertNotNull(readOnlyUser);
    readOnlyUserid = readOnlyUser.getUuid();
    List<String> userIds = new ArrayList<>();
    userIds.add(readOnlyUser.getUuid());
    AccountPermissions accountPermissions = UserGroupUtils.buildReadOnlyPermission();
    userGroup =
        UserGroupUtils.createUserGroupWithPermissionAndMembers(getAccount(), bearerToken, userIds, accountPermissions);
  }

  private void deleteMembers() {
    List<String> emptyList = new ArrayList<>();
    userGroup.setMemberIds(emptyList);
    assertTrue(UserGroupRestUtils.updateMembers(getAccount(), bearerToken, userGroup) == HttpStatus.SC_OK);
    userGroup = UserGroupUtils.getUserGroup(getAccount(), bearerToken, userGroup.getName());
    assertTrue(userGroup.getMemberIds() == null || userGroup.getMemberIds().size() == 0);
  }

  @After
  public void rbacCleanup() {
    logger.info("Running RBAC cleanup");
    deleteMembers();
    UserGroupRestUtils.deleteUserGroup(getAccount(), bearerToken, userGroup.getUuid());
  }
}
