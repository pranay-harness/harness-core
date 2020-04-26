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

@Slf4j
public class AccessManagementROTest extends AbstractFunctionalTest {
  final String RBAC_USER = "rbac1@harness.io";
  String readOnlyUserid;
  UserGroup userGroup;

  @Before
  public void rbacSetup() {
    logger.info("Running RBAC setup");
    User readOnlyUser = UserUtils.getUser(bearerToken, getAccount().getUuid(), RBAC_USER).getUser();
    assertThat(readOnlyUser).isNotNull();
    readOnlyUserid = readOnlyUser.getUuid();
    userGroup = UserGroupUtils.createUserGroup(
        getAccount(), bearerToken, readOnlyUserid, PermissionTypes.ACCOUNT_READONLY.toString());
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void accessManagementPermissionTestForList() {
    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(RBAC_USER, "rbac1");
    AccessManagementUtils.runUserAndGroupsListTest(getAccount(), roBearerToken, HttpStatus.SC_OK);
    AccessManagementUtils.runNoAccessTest(getAccount(), roBearerToken, readOnlyUserid);
  }

  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForUser() {
    final String READ_ONLY_USER = "rbac1@harness.io";
    String email = "testemail@harness.mailinator.com";
    String password = "rbac1";
    AccessManagementUtils.runUserPostTest(
        getAccount(), bearerToken, READ_ONLY_USER, email, password, HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void accessManagementNoPermissionTestForGet() {
    logger.info("Readonly test for GET");
    final String READ_ONLY_USER = "rbac1@harness.io";
    AccessManagementUtils.runAllGetTests(
        getAccount(), bearerToken, READ_ONLY_USER, "rbac1", HttpStatus.SC_OK, HttpStatus.SC_BAD_REQUEST);
    logger.info("Readonly test for GET ends");
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForUserGroup() {
    final String READ_ONLY_USER = "rbac1@harness.io";
    AccessManagementUtils.testPermissionToPostInUserGroup(
        getAccount(), bearerToken, READ_ONLY_USER, "rbac1", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForIPWhitelisting() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    AccessManagementUtils.amNoPermissionToPostForIPWhitelisting(
        getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForAPIKeys() {
    final String READ_ONLY_USER = "rbac1@harness.io";
    AccessManagementUtils.runAPIKeyPostTest(
        getAccount(), bearerToken, READ_ONLY_USER, "rbac1", HttpStatus.SC_BAD_REQUEST, userGroup);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForLDAP() {
    final String READ_ONLY_USER = "rbac1@harness.io";
    AccessManagementUtils.ldapCreationFailureCheckTest(
        getAccount(), bearerToken, READ_ONLY_USER, "rbac1", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForSAML() {
    final String READ_ONLY_USER = "rbac1@harness.io";
    AccessManagementUtils.amNoPermissionToPostForSAML(
        getAccount(), bearerToken, READ_ONLY_USER, "rbac1", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void updateIPWhitelistingTest() {
    final String READ_ONLY_USER = "rbac1@harness.io";
    AccessManagementUtils.updateIPWhiteListing(
        getAccount(), bearerToken, READ_ONLY_USER, "rbac1", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void deleteIPWhitelistingTest() {
    final String READ_ONLY_USER = "rbac1@harness.io";
    AccessManagementUtils.deleteIPWhitelisting(
        getAccount(), bearerToken, READ_ONLY_USER, "rbac1", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void updateAndDeleteApiKeysTest() {
    final String READ_ONLY_USER = "rbac1@harness.io";
    AccessManagementUtils.updateAndDeleteAPIKeys(
        getAccount(), bearerToken, READ_ONLY_USER, "rbac1", userGroup, HttpStatus.SC_BAD_REQUEST);
    logger.info("Test completed successfully");
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void createApplicationFail() {
    logger.info("Check if create application test fails");
    String roBearerToken = Setup.getAuthToken(RBAC_USER, "rbac1");
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
    Setup.signOut(readOnlyUserid, roBearerToken);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void deleteApplicationFail() {
    logger.info("Check if delete application test fails");
    String roBearerToken = Setup.getAuthToken(RBAC_USER, "rbac1");
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
    Setup.signOut(readOnlyUserid, roBearerToken);
  }

  @After
  public void rbacCleanup() {
    logger.info("Running RBAC cleanup");
    UserGroupUtils.deleteMembers(getAccount(), bearerToken, userGroup);
    UserGroupRestUtils.deleteUserGroup(getAccount(), bearerToken, userGroup.getUuid());
  }
}
