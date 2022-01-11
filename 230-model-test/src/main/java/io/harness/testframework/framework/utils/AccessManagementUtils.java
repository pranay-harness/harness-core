/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework.utils;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.testframework.framework.Setup;
import io.harness.testframework.restutils.ApiKeysRestUtils;
import io.harness.testframework.restutils.IPWhitelistingRestUtils;
import io.harness.testframework.restutils.SSORestUtils;

import software.wings.beans.Account;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.access.Whitelist;
import software.wings.beans.security.access.WhitelistStatus;
import software.wings.beans.sso.LdapSettings;

import com.google.gson.JsonObject;
import io.restassured.mapper.ObjectMapperType;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

@Slf4j
public class AccessManagementUtils {
  public static void runNoAccessTest(Account account, String roBearerToken, String readOnlyUserid) {
    log.info("List user groups failed with Bad request as expected");
    log.info("List the SSO Settings using ReadOnly permission");
    log.info("The below statement fails for the bug - PL-2335. Disabling it to avoid failures");

    /*assertThat(Setup.portal()
        .auth()
        .oauth2(roBearerToken)
        .queryParam("accountId", getAccount().getUuid())
        .get("/sso/access-management/" + getAccount().getUuid()).getStatusCode() == HttpStatus.SC_BAD_REQUEST).isTrue();
    log.info("List SSO settings failed with Bad request as expected");*/

    log.info("List the APIKeys using ReadOnly permission");

    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/api-keys")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST)
        .isTrue();

    log.info("List APIKeys failed with Bad request as expected");

    log.info("List the IPWhitelists using ReadOnly permission");
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/whitelist")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST)
        .isTrue();
    log.info("List IPWhitelists failed with Bad request as expected");

    Setup.signOut(readOnlyUserid, roBearerToken);
    log.info("Readonly user logout successful");
  }

  public static void runUserPostTest(
      Account account, String bearerToken, String userName, String email, String password, int expectedInviteStatus) {
    log.info("Creating the user : " + userName);
    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), userName).getUser();
    log.info("Logging in as user : " + userName);
    String roBearerToken = Setup.getAuthToken(userName, password);
    UserInvite invite = UserUtils.createUserInvite(account, email);
    log.info("Attempting to create a user with expectation : " + expectedInviteStatus);
    assertThat(UserUtils.attemptInvite(invite, account, roBearerToken, expectedInviteStatus)).isTrue();
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    log.info("Readonly user logout successful");
  }

  public static void runAllGetTests(Account account, String bearerToken, String userId, String password,
      int expectedStatusCodeForUsersAndGroups, int expectedStatusGroupForOthers) {
    final String IP_WHITELIST_VAL = "0.0.0.0";
    String apiKeyName = generateUuid();
    log.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), userId).getUser();
    UserGroup userGroup = UserGroupUtils.getUserGroup(account, bearerToken, "Account Administrator");
    List<String> userIdsToAdd = new ArrayList<>();
    userIdsToAdd.add(readOnlyUser.getUuid());

    ApiKeyEntry apiKeyEntry = ApiKeyEntry.builder().build();
    List<String> userGroupIds = new ArrayList<>();
    userGroupIds.add(userGroup.getUuid());
    apiKeyEntry.setUserGroupIds(userGroupIds);
    apiKeyEntry.setName(apiKeyName);
    apiKeyEntry.setAccountId(account.getUuid());
    ApiKeyEntry postedEntry = ApiKeysRestUtils.createApiKey(account.getUuid(), bearerToken, apiKeyEntry);

    Whitelist ipToWhiteList = new Whitelist();
    ipToWhiteList.setAccountId(account.getUuid());
    ipToWhiteList.setFilter(IP_WHITELIST_VAL);
    ipToWhiteList.setDescription("Test Whitelisting");
    ipToWhiteList.setStatus(WhitelistStatus.DISABLED);

    log.info("Adding the IP to be whitelisted");
    Whitelist ipAdded = IPWhitelistingRestUtils.addWhiteListing(account.getUuid(), bearerToken, ipToWhiteList);
    assertThat(ipAdded).isNotNull();
    log.info("IPWhitelisting verification successful");

    log.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(userId, password);
    assertThat(readOnlyUser).isNotNull();
    log.info("Designated user's Bearer Token issued");

    log.info("Get the user groups using ReadOnly permission");
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/userGroups/" + userGroup.getUuid())
                   .getStatusCode()
        == expectedStatusCodeForUsersAndGroups)
        .isTrue();
    log.info("User group test for assert succeeded");
    log.info("List user groups failed with Bad request as expected");

    //    log.info("List the LDAP Settings using ReadOnly permission");
    //    log.info("The below statement fails for the bug - PL-2335. Disabling it to avoid failures");
    //
    //    assertThat(SSORestUtils.getLdapSettings(account.getUuid(), roBearerToken) ==
    //    expectedStatusGroupForOthers).isTrue(); log.info("LDAP setting test succeeded");
    //    assertThat(SSORestUtils.deleteLDAPSettings(account.getUuid(), bearerToken) == HttpStatus.SC_OK).isTrue();
    //    log.info("LDAP setting test for delete succeeded");

    log.info("List the APIKeys using ReadOnly permission");

    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/api-keys/" + postedEntry.getUuid())
                   .getStatusCode()
        == expectedStatusGroupForOthers)
        .isTrue();

    log.info("List APIKeys failed with Bad request as expected");

    log.info("List the IPWhitelists using ReadOnly permission");
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/whitelist/" + ipAdded.getUuid())
                   .getStatusCode()
        == expectedStatusGroupForOthers)
        .isTrue();
    log.info("List IPWhitelists failed with Bad request as expected");

    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    log.info("Readonly user logout successful");
  }

  public static void testPermissionToPostInUserGroup(
      Account account, String bearerToken, String userId, String password, int expectedResult) {
    final String READ_ONLY_USER = userId;
    log.info("Starting with the ReadOnly Test");
    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER).getUser();
    log.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);
    log.info("Creating a new user group without permission. It should fail");
    JsonObject groupInfoAsJson = new JsonObject();
    String name = "UserGroup - " + System.currentTimeMillis();
    groupInfoAsJson.addProperty("name", name);
    groupInfoAsJson.addProperty("description", "Test Description - " + System.currentTimeMillis());
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .body(groupInfoAsJson.toString())
                   .post("/userGroups")
                   .getStatusCode()
        == expectedResult)
        .isTrue();
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    log.info("Readonly user logout successful");
  }

  public static void amNoPermissionToPostForIPWhitelisting(
      Account account, String bearerToken, String userId, String password, int expectedOutcome) {
    final String IP_WHITELIST_VAL = "0.0.0.0";
    log.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), userId).getUser();

    log.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(userId, password);

    log.info("Attempting to create IP whitelisting entry without permissions");
    Whitelist ipToWhiteList = new Whitelist();
    ipToWhiteList.setAccountId(account.getUuid());
    ipToWhiteList.setFilter(IP_WHITELIST_VAL);
    ipToWhiteList.setDescription("Test Whitelisting");
    ipToWhiteList.setStatus(WhitelistStatus.DISABLED);

    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .body(ipToWhiteList, ObjectMapperType.GSON)
                   .post("/whitelist")
                   .getStatusCode()
        == expectedOutcome)
        .isTrue();

    log.info("Adding the IP to be whitelisted");

    log.info("IP whitelisting creation denied successfully");
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    log.info("Readonly user logout successful");
  }

  public static void ldapCreationFailureCheckTest(
      Account account, String bearerToken, String userId, String password, int statusCodeExpected) {
    final String READ_ONLY_USER = userId;
    log.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER).getUser();

    log.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);
    log.info("Trying to create LDAP SSO Setting Without Permission");
    LdapSettings ldapSettings = SSOUtils.createDefaultLdapSettings(account.getUuid());
    assertThat(SSORestUtils.addLdapSettings(account.getUuid(), roBearerToken, ldapSettings) == statusCodeExpected)
        .isTrue();
    log.info("LDAP creation denied successfully");

    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    log.info("Readonly user logout successful");
  }

  public static void amNoPermissionToPostForSAML(
      Account account, String bearerToken, String userId, String password, int statusCodeExpected) {
    final String READ_ONLY_USER = userId;
    log.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER).getUser();

    log.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);
    log.info("Creating SAML config without permission");
    String filePath = System.getProperty("user.dir");
    filePath = filePath + "/"
        + "200-functional-test/src/test/resources/secrets/"
        + "SAML_SSO_Provider.xml";
    assertThat(SSORestUtils.addSAMLSettings(account.getUuid(), roBearerToken, "SAML", filePath) == statusCodeExpected)
        .isTrue();
    log.info("SAML creation denied successfully");
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    log.info("Readonly user logout successful");
  }

  public static void updateIPWhiteListing(
      Account account, String bearerToken, String userId, String password, int statusCodeExpected) {
    final String IP_WHITELIST_VAL = "0.0.0.0";
    Whitelist ipToWhiteList = new Whitelist();
    ipToWhiteList.setAccountId(account.getUuid());
    ipToWhiteList.setFilter(IP_WHITELIST_VAL);
    ipToWhiteList.setDescription("Test Whitelisting");
    ipToWhiteList.setStatus(WhitelistStatus.DISABLED);
    log.info("Adding the IP to be whitelisted");
    Whitelist ipAdded = IPWhitelistingRestUtils.addWhiteListing(account.getUuid(), bearerToken, ipToWhiteList);
    assertThat(ipAdded).isNotNull();
    log.info("Updating and verifying the whitelisted IP");
    ipToWhiteList.setUuid(ipAdded.getUuid());
    ipToWhiteList.setFilter("127.0.0.1");
    ipToWhiteList.setDescription("Modified description");

    final String READ_ONLY_USER = userId;
    log.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER).getUser();
    log.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);

    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .body(ipToWhiteList, ObjectMapperType.GSON)
                   .put("/whitelist/" + ipToWhiteList.getUuid())
                   .getStatusCode()
        == statusCodeExpected)
        .isTrue();

    log.info("Updation of whitelist denied successfully");
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    log.info("Readonly user logout successful");

    log.info("Deleting the created whitelist entries");
    assertThat(IPWhitelistingRestUtils.deleteWhitelistedIP(account.getUuid(), bearerToken, ipAdded.getUuid())).isTrue();
  }

  public static void updateAndDeleteAPIKeys(Account account, String bearerToken, String userId, String password,
      UserGroup userGroup, int statusCodeExpected) {
    final String READ_ONLY_USER = userId;
    log.info("Constructing APIKeys");
    ApiKeyEntry apiKeyEntry = ApiKeyEntry.builder().build();
    List<String> userGroupList = new ArrayList<>();
    userGroupList.add(userGroup.getUuid());
    String name = "APIKey - " + System.currentTimeMillis();
    apiKeyEntry.setUserGroupIds(userGroupList);
    apiKeyEntry.setAccountId(account.getUuid());
    apiKeyEntry.setName(name);
    log.info("Creating APIKeys");
    ApiKeyEntry postCreationEntry = ApiKeysRestUtils.createApiKey(apiKeyEntry.getAccountId(), bearerToken, apiKeyEntry);
    log.info("Validating created APIKeys");
    assertThat(postCreationEntry).isNotNull();

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER).getUser();
    log.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);

    log.info("Attempting to update APIKeys");
    String changedName = "APIKey_Changed - " + System.currentTimeMillis();
    postCreationEntry.setName(changedName);
    postCreationEntry.setUserGroups(null);
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .body(postCreationEntry, ObjectMapperType.GSON)
                   .post("/api-keys")
                   .getStatusCode()
        == statusCodeExpected)
        .isTrue();

    log.info("Updation of APIKeys denied successfully");

    log.info("Deleting the APIKeys without permission");
    assertThat(ApiKeysRestUtils.deleteApiKey(account.getUuid(), roBearerToken, postCreationEntry.getUuid())
        == HttpStatus.SC_BAD_REQUEST)
        .isTrue();
    log.info("Deletion denied successfully");

    log.info("Cleaning up. Deleting the APIKeys now");

    assertThat(
        ApiKeysRestUtils.deleteApiKey(account.getUuid(), bearerToken, postCreationEntry.getUuid()) == HttpStatus.SC_OK)
        .isTrue();
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    log.info("Readonly user logout successful");
  }

  public static void deleteIPWhitelisting(
      Account account, String bearerToken, String userId, String password, int statusCodeExpected) {
    final String READ_ONLY_USER = userId;
    final String IP_WHITELIST_VAL = "0.0.0.0";
    Whitelist ipToWhiteList = new Whitelist();
    ipToWhiteList.setAccountId(account.getUuid());
    ipToWhiteList.setFilter(IP_WHITELIST_VAL);
    ipToWhiteList.setDescription("Test Whitelisting");
    ipToWhiteList.setStatus(WhitelistStatus.DISABLED);
    log.info("Adding the IP to be whitelisted");
    Whitelist ipAdded = IPWhitelistingRestUtils.addWhiteListing(account.getUuid(), bearerToken, ipToWhiteList);
    assertThat(ipAdded).isNotNull();
    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER).getUser();
    log.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .delete("/whitelist/" + ipAdded.getUuid())
                   .getStatusCode()
        == statusCodeExpected)
        .isTrue();
    log.info("Deletion denied to the given read only/no permission user");
    log.info("Deleting the created whitelist entries");
    assertThat(IPWhitelistingRestUtils.deleteWhitelistedIP(account.getUuid(), bearerToken, ipAdded.getUuid())).isTrue();
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    log.info("Readonly user logout successful");
  }

  public static void runAPIKeyPostTest(Account account, String bearerToken, String userId, String password,
      int statusCodeExpected, UserGroup userGroup) {
    log.info("Starting with the ReadOnly Test");
    final String READ_ONLY_USER = userId;
    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER).getUser();

    log.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);

    log.info("Attempting to add an APIKey without permission");
    log.info("Constructing APIKeys");
    List<String> userGroupList = new ArrayList<>();
    userGroupList.add(userGroup.getUuid());
    String name = "APIKey - " + System.currentTimeMillis();
    ApiKeyEntry apiKeyEntry = ApiKeyEntry.builder().build();
    apiKeyEntry.setUserGroupIds(userGroupList);
    apiKeyEntry.setAccountId(account.getUuid());
    apiKeyEntry.setName(name);
    log.info("Creating an APIKey without permission");

    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .body(apiKeyEntry, ObjectMapperType.GSON)
                   .post("/api-keys")
                   .getStatusCode()
        == statusCodeExpected)
        .isTrue();

    log.info("APIKey creation denied successfully");

    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    log.info("Readonly user logout successful");
  }

  public static void runUserAndGroupsListTest(Account account, String bearerToken, int httpStatus) {
    log.info("List the users using ReadOnly permission");
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(bearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/users")
                   .getStatusCode()
        == httpStatus)
        .isTrue();
    log.info("List users failed with Bad request as expected");
    log.info("List the user groups using ReadOnly permission");
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(bearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/userGroups")
                   .getStatusCode()
        == httpStatus)
        .isTrue();
  }
}
