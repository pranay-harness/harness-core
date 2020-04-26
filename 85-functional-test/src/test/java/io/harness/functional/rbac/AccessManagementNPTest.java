package io.harness.functional.rbac;

import static io.harness.rule.OwnerRule.NATARAJA;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.utils.AccessManagementUtils;
import io.harness.testframework.framework.utils.UserGroupUtils;
import io.harness.testframework.framework.utils.UserUtils;
import io.harness.testframework.restutils.UserGroupRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;

@Slf4j
public class AccessManagementNPTest extends AbstractFunctionalTest {
  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void accessManagementNoPermissionTestForList() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, getAccount().getUuid(), READ_ONLY_USER).getUser();
    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, "readonlyuser");
    assertThat(readOnlyUser).isNotNull();
    logger.info("List the users using ReadOnly permission");
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/users")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST)
        .isTrue();
    logger.info("List users failed with Bad request as expected");
    logger.info("List the user groups using ReadOnly permission");
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/userGroups")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST)
        .isTrue();
    AccessManagementUtils.runNoAccessTest(getAccount(), roBearerToken, readOnlyUser.getUuid());
    logger.info("Tests completed");
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void accessManagementNoPermissionTestForGet() {
    logger.info("No permission test for GET");
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    AccessManagementUtils.runAllGetTests(getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser",
        HttpStatus.SC_BAD_REQUEST, HttpStatus.SC_BAD_REQUEST);
    logger.info("No permission test for GET ends");
  }

  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForUser() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    String email = "testemail@harness.mailinator.com";
    String password = "readonlyuser";
    AccessManagementUtils.runUserPostTest(
        getAccount(), bearerToken, READ_ONLY_USER, email, password, HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForUserGroup() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    AccessManagementUtils.testPermissionToPostInUserGroup(
        getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser", HttpStatus.SC_BAD_REQUEST);
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
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    UserGroup userGroup = createUserGroup();
    AccessManagementUtils.runAPIKeyPostTest(
        getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser", HttpStatus.SC_BAD_REQUEST, userGroup);
    assertThat(UserGroupRestUtils.deleteUserGroup(getAccount(), bearerToken, userGroup.getUuid())).isTrue();
    logger.info("Test completed successfully");
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForLDAP() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    AccessManagementUtils.ldapCreationFailureCheckTest(
        getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForSAML() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    AccessManagementUtils.amNoPermissionToPostForSAML(
        getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void updateIPWhitelistingTest() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    AccessManagementUtils.updateIPWhiteListing(
        getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void deleteIPWhitelistingTest() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    AccessManagementUtils.deleteIPWhitelisting(
        getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void updateAndDeleteApiKeysTest() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    UserGroup userGroup = createUserGroup();
    AccessManagementUtils.updateAndDeleteAPIKeys(
        getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser", userGroup, HttpStatus.SC_BAD_REQUEST);
    assertThat(UserGroupRestUtils.deleteUserGroup(getAccount(), bearerToken, userGroup.getUuid())).isTrue();
    logger.info("Test completed successfully");
  }

  private UserGroup createUserGroup() {
    logger.info("Creating a userGroup");
    logger.info("Creating a new user group");
    JsonObject groupInfoAsJson = new JsonObject();
    String userGroupname = "UserGroup - " + System.currentTimeMillis();
    groupInfoAsJson.addProperty("name", userGroupname);
    groupInfoAsJson.addProperty("description", "Test Description - " + System.currentTimeMillis());
    UserGroup userGroup = UserGroupUtils.createUserGroup(getAccount(), bearerToken, groupInfoAsJson);
    assertThat(userGroup).isNotNull();
    return userGroup;
  }
}