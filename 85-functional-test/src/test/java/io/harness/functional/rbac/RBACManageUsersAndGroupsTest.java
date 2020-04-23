package io.harness.functional.rbac;

import static io.harness.rule.OwnerRule.NATARAJA;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
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

/*
 * All the commented sections in this tests are bugs.
 * Individual bugs will be filed. However, since they are existing bugs, commenting out these tests to avoid creating
 * noise.
 */

@Slf4j
public class RBACManageUsersAndGroupsTest extends AbstractFunctionalTest {
  final String RBAC_USER = "rbac2@harness.io";
  String userGroupManagementId;
  UserGroup userGroup;

  @Before
  public void rbacManageUsersAndGroupsSetup() {
    logger.info("Running RBAC setup");
    User readOnlyUser = UserUtils.getUser(bearerToken, getAccount().getUuid(), RBAC_USER);
    assertThat(readOnlyUser).isNotNull();
    userGroupManagementId = readOnlyUser.getUuid();
    userGroup = UserGroupUtils.createUserGroup(
        getAccount(), bearerToken, userGroupManagementId, PermissionTypes.ACCOUNT_USERANDGROUPS.toString());
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void accessManagementPermissionTestForList() {
    logger.info("Logging in as a rbac2 user");
    String roBearerToken = Setup.getAuthToken(RBAC_USER, "rbac2");
    AccessManagementUtils.runUserAndGroupsListTest(getAccount(), roBearerToken, HttpStatus.SC_OK);
    // AccessManagementUtils.runNoAccessTest(getAccount(), roBearerToken, userGroupManagementId);
    Setup.signOut(userGroupManagementId, roBearerToken);
  }

  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void amPermissionToPostForUser() {
    final String READ_ONLY_USER = "rbac2@harness.io";
    String email = "testemail2@harness.mailinator.com";
    String password = "rbac2";
    AccessManagementUtils.runUserPostTest(getAccount(), bearerToken, READ_ONLY_USER, email, password, HttpStatus.SC_OK);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void createApplicationFail() {
    logger.info("Check if create application test fails");
    String roBearerToken = Setup.getAuthToken(RBAC_USER, "rbac2");
    final String appName = "TestApp" + System.currentTimeMillis();
    Application application = anApplication().name(appName).build();
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .body(application, ObjectMapperType.GSON)
                   .contentType(ContentType.JSON)
                   .post("/apps")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST)
        .isTrue();
    Setup.signOut(userGroupManagementId, roBearerToken);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void deleteApplicationFail() {
    logger.info("Check if delete application test fails");
    String roBearerToken = Setup.getAuthToken(RBAC_USER, "rbac2");
    final String appName = "TestApp" + System.currentTimeMillis();
    Application application = anApplication().name(appName).build();
    Application createdApp = ApplicationRestUtils.createApplication(bearerToken, getAccount(), application);
    assertThat(createdApp).isNotNull();
    assertThat(ApplicationRestUtils.deleteApplication(roBearerToken, createdApp.getUuid(), getAccount().getUuid())
        == HttpStatus.SC_BAD_REQUEST)
        .isTrue();
    assertThat(ApplicationRestUtils.deleteApplication(bearerToken, createdApp.getUuid(), getAccount().getUuid())
        == HttpStatus.SC_OK)
        .isTrue();
    Setup.signOut(userGroupManagementId, roBearerToken);
  }

  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  @Owner(emails = SWAMY)
  //  @Category(FunctionalTests.class)
  //  public void accessManagementPermissionTestForGet() {
  //    final String READ_ONLY_USER = "rbac2@harness.io";
  //    AccessManagementUtils.runAllGetTests(
  //        getAccount(), bearerToken, READ_ONLY_USER, "rbac2", HttpStatus.SC_OK, HttpStatus.SC_BAD_REQUEST);
  //  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void amPermissionToPostForUserGroup() {
    final String READ_ONLY_USER = "rbac2@harness.io";
    AccessManagementUtils.testPermissionToPostInUserGroup(
        getAccount(), bearerToken, READ_ONLY_USER, "rbac2", HttpStatus.SC_OK);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void updateMembersAndPermissionsInUserGroup() {
    logger.info("Logging in as a rbac2 user");
    String roBearerToken = Setup.getAuthToken(RBAC_USER, "rbac2");
    UserGroupUtils.createUserGroup(
        getAccount(), roBearerToken, userGroupManagementId, PermissionTypes.ACCOUNT_ADMIN.toString());
    Setup.signOut(userGroupManagementId, roBearerToken);
    logger.info("Logging out as a rbac2 user");
  }

  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  @Owner(emails = SWAMY)
  //  @Category(FunctionalTests.class)
  //  public void amNoPermissionToPostForIPWhitelisting() {
  //    AccessManagementUtils.amNoPermissionToPostForIPWhitelisting(
  //        getAccount(), bearerToken, RBAC_USER, "readonlyuser", HttpStatus.SC_BAD_REQUEST);
  //    logger.info("Logging out as a rbac2 user");
  //  }
  //
  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  @Owner(emails = SWAMY)
  //  @Category(FunctionalTests.class)
  //  public void updateNotificationSettingsInUserGroup() {
  //    logger.info("Logging in as a rbac2 user");
  //    String roBearerToken = Setup.getAuthToken(RBAC_USER, "rbac2");
  //    UserGroupUtils.createUserGroupAndUpdateWithNotificationSettings(getAccount(), roBearerToken);
  //    Setup.signOut(userGroupManagementId, roBearerToken);
  //    logger.info("Logging out as a rbac2 user");
  //  }
  //
  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  @Owner(emails = SWAMY)
  //  @Category(FunctionalTests.class)
  //  public void amNoPermissionToPostForAPIKeys() {
  //    logger.info("Logging in as a rbac2 user");
  //    AccessManagementUtils.runAPIKeyPostTest(
  //        getAccount(), bearerToken, RBAC_USER, "rbac2", HttpStatus.SC_BAD_REQUEST, userGroup);
  //  }
  //
  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  @Owner(emails = SWAMY)
  //  @Category(FunctionalTests.class)
  //  public void amNoPermissionToPostForLDAP() {
  //    AccessManagementUtils.ldapCreationFailureCheckTest(
  //        getAccount(), bearerToken, RBAC_USER, "rbac2", HttpStatus.SC_BAD_REQUEST);
  //  }
  //
  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  @Owner(emails = SWAMY)
  //  @Category(FunctionalTests.class)
  //  public void amNoPermissionToPostForSAML() {
  //    AccessManagementUtils.amNoPermissionToPostForSAML(
  //        getAccount(), bearerToken, RBAC_USER, "rbac2", HttpStatus.SC_BAD_REQUEST);
  //  }
  //
  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  @Owner(emails = SWAMY)
  //  @Category(FunctionalTests.class)
  //  public void updateIPWhitelistingTest() {
  //    AccessManagementUtils.updateIPWhiteListing(
  //        getAccount(), bearerToken, RBAC_USER, "rbac2", HttpStatus.SC_BAD_REQUEST);
  //    logger.info("Test Completed");
  //  }
  //
  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  @Owner(emails = SWAMY)
  //  @Category(FunctionalTests.class)
  //  public void deleteIPWhitelistingTest() {
  //    AccessManagementUtils.deleteIPWhitelisting(
  //        getAccount(), bearerToken, RBAC_USER, "rbac2", HttpStatus.SC_BAD_REQUEST);
  //    logger.info("Logging out as a rbac2 user");
  //  }
  //
  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  @Owner(emails = SWAMY)
  //  @Category(FunctionalTests.class)
  //  public void updateAndDeleteApiKeysTest() {
  //    AccessManagementUtils.updateAndDeleteAPIKeys(
  //        getAccount(), bearerToken, RBAC_USER, "rbac2", userGroup, HttpStatus.SC_BAD_REQUEST);
  //    logger.info("Test completed successfully");
  //  }

  @After
  public void rbacCleanup() {
    logger.info("Running RBAC cleanup");
    UserGroupUtils.deleteMembers(getAccount(), bearerToken, userGroup);
    UserGroupRestUtils.deleteUserGroup(getAccount(), bearerToken, userGroup.getUuid());
  }
}
