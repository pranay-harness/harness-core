package io.harness.functional.rbac;

import static io.harness.rule.OwnerRule.SWAMY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.Application.Builder.anApplication;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.constants.AccountManagementConstants.PermissionTypes;
import io.harness.testframework.framework.utils.AccessManagementUtils;
import io.harness.testframework.framework.utils.UserGroupUtils;
import io.harness.testframework.framework.utils.UserUtils;
import io.harness.testframework.restutils.ApplicationRestUtils;
import io.harness.testframework.restutils.UserGroupRestUtils;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;

@Slf4j
public class RBACOtherAccountsTest extends AbstractFunctionalTest {
  final String RBAC_USER = "default@harness.io";
  String userGroupManagementId;
  UserGroup userGroup;

  @Before
  public void rbacManageUsersAndGroupsSetup() {
    logger.info("Running RBAC setup");
    User readOnlyUser = UserUtils.getUser(bearerToken, getAccount().getUuid(), RBAC_USER);
    assertNotNull(readOnlyUser);
    userGroupManagementId = readOnlyUser.getUuid();
    userGroup = UserGroupUtils.createUserGroup(
        getAccount(), bearerToken, userGroupManagementId, PermissionTypes.ACCOUNT_MANAGEMENT.toString());
  }

  @Test
  @Owner(emails = SWAMY)
  @Category(FunctionalTests.class)
  public void accessManagementPermissionTestForList() {
    logger.info("Logging in as a default user");
    String roBearerToken = Setup.getAuthToken(RBAC_USER, "default");
    AccessManagementUtils.runUserAndGroupsListTest(getAccount(), roBearerToken, HttpStatus.SC_BAD_REQUEST);
    Setup.signOut(userGroupManagementId, roBearerToken);
  }

  @Test
  @Owner(emails = SWAMY)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForUser() {
    String email = "testemail2@harness.mailinator.com";
    String password = "default";
    AccessManagementUtils.runUserPostTest(
        getAccount(), bearerToken, RBAC_USER, email, password, HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(emails = SWAMY)
  @Category(FunctionalTests.class)
  public void accessManagementNoPermissionTestForGet() {
    logger.info("Readonly test for GET");
    AccessManagementUtils.runAllGetTests(
        getAccount(), bearerToken, RBAC_USER, "default", HttpStatus.SC_BAD_REQUEST, HttpStatus.SC_OK);
    logger.info("Readonly test for GET ends");
  }

  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForUserGroup() {
    AccessManagementUtils.testPermissionToPostInUserGroup(
        getAccount(), bearerToken, RBAC_USER, "default", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForIPWhitelisting() {
    AccessManagementUtils.amNoPermissionToPostForIPWhitelisting(
        getAccount(), bearerToken, RBAC_USER, "default", HttpStatus.SC_OK);
  }

  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForAPIKeys() {
    AccessManagementUtils.runAPIKeyPostTest(
        getAccount(), bearerToken, RBAC_USER, "default", HttpStatus.SC_OK, userGroup);
  }

  @Test
  @Owner(emails = SWAMY)
  @Category(FunctionalTests.class)
  public void createApplicationFail() {
    logger.info("Check if create application test fails");
    String roBearerToken = Setup.getAuthToken(RBAC_USER, "default");
    final String appName = "TestApp" + System.currentTimeMillis();
    Application application = anApplication().name(appName).build();
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .body(application, ObjectMapperType.GSON)
                   .contentType(ContentType.JSON)
                   .post("/apps")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);
    Setup.signOut(userGroupManagementId, roBearerToken);
  }

  @Test
  @Owner(emails = SWAMY)
  @Category(FunctionalTests.class)
  public void deleteApplicationFail() {
    logger.info("Check if delete application test fails");
    String roBearerToken = Setup.getAuthToken(RBAC_USER, "default");
    final String appName = "TestApp" + System.currentTimeMillis();
    Application application = anApplication().name(appName).build();
    Application createdApp = ApplicationRestUtils.createApplication(bearerToken, getAccount(), application);
    assertNotNull(createdApp);
    assertTrue(ApplicationRestUtils.deleteApplication(roBearerToken, createdApp.getUuid(), getAccount().getUuid())
        == HttpStatus.SC_BAD_REQUEST);
    assertTrue(ApplicationRestUtils.deleteApplication(bearerToken, createdApp.getUuid(), getAccount().getUuid())
        == HttpStatus.SC_OK);
    Setup.signOut(userGroupManagementId, roBearerToken);
  }

  @After
  public void rbacCleanup() {
    logger.info("Running RBAC cleanup");
    UserGroupUtils.deleteMembers(getAccount(), bearerToken, userGroup);
    UserGroupRestUtils.deleteUserGroup(getAccount(), bearerToken, userGroup.getUuid());
  }
}
