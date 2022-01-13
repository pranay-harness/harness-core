/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.rbac;

import static io.harness.rule.OwnerRule.NATARAJA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.utils.AccessManagementUtils;
import io.harness.testframework.framework.utils.UserGroupUtils;
import io.harness.testframework.framework.utils.UserUtils;
import io.harness.testframework.restutils.UserGroupRestUtils;

import software.wings.beans.User;
import software.wings.beans.security.UserGroup;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class AccessManagementNPTest extends AbstractFunctionalTest {
  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void accessManagementNoPermissionTestForList() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    log.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, getAccount().getUuid(), READ_ONLY_USER).getUser();
    log.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, "readonlyuser");
    assertThat(readOnlyUser).isNotNull();
    log.info("List the users using ReadOnly permission");
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/users")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST)
        .isTrue();
    log.info("List users failed with Bad request as expected");
    log.info("List the user groups using ReadOnly permission");
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/userGroups")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST)
        .isTrue();
    AccessManagementUtils.runNoAccessTest(getAccount(), roBearerToken, readOnlyUser.getUuid());
    log.info("Tests completed");
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void accessManagementNoPermissionTestForGet() {
    log.info("No permission test for GET");
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    AccessManagementUtils.runAllGetTests(getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser",
        HttpStatus.SC_BAD_REQUEST, HttpStatus.SC_BAD_REQUEST);
    log.info("No permission test for GET ends");
  }

  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
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
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void amNoPermissionToPostForUserGroup() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    AccessManagementUtils.testPermissionToPostInUserGroup(
        getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void amNoPermissionToPostForIPWhitelisting() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    AccessManagementUtils.amNoPermissionToPostForIPWhitelisting(
        getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void amNoPermissionToPostForAPIKeys() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    UserGroup userGroup = createUserGroup();
    AccessManagementUtils.runAPIKeyPostTest(
        getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser", HttpStatus.SC_BAD_REQUEST, userGroup);
    assertThat(UserGroupRestUtils.deleteUserGroup(getAccount(), bearerToken, userGroup.getUuid())).isTrue();
    log.info("Test completed successfully");
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void amNoPermissionToPostForLDAP() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    AccessManagementUtils.ldapCreationFailureCheckTest(
        getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void amNoPermissionToPostForSAML() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    AccessManagementUtils.amNoPermissionToPostForSAML(
        getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void updateIPWhitelistingTest() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    AccessManagementUtils.updateIPWhiteListing(
        getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void deleteIPWhitelistingTest() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    AccessManagementUtils.deleteIPWhitelisting(
        getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser", HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void updateAndDeleteApiKeysTest() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    UserGroup userGroup = createUserGroup();
    AccessManagementUtils.updateAndDeleteAPIKeys(
        getAccount(), bearerToken, READ_ONLY_USER, "readonlyuser", userGroup, HttpStatus.SC_BAD_REQUEST);
    assertThat(UserGroupRestUtils.deleteUserGroup(getAccount(), bearerToken, userGroup.getUuid())).isTrue();
    log.info("Test completed successfully");
  }

  private UserGroup createUserGroup() {
    log.info("Creating a userGroup");
    log.info("Creating a new user group");
    JsonObject groupInfoAsJson = new JsonObject();
    String userGroupname = "UserGroup - " + System.currentTimeMillis();
    groupInfoAsJson.addProperty("name", userGroupname);
    groupInfoAsJson.addProperty("description", "Test Description - " + System.currentTimeMillis());
    UserGroup userGroup = UserGroupUtils.createUserGroup(getAccount(), bearerToken, groupInfoAsJson);
    assertThat(userGroup).isNotNull();
    return userGroup;
  }
}
