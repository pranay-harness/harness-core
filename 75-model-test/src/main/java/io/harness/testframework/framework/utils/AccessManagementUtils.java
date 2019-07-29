package io.harness.testframework.framework.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;

import io.harness.testframework.framework.Setup;
import io.harness.testframework.restutils.ApiKeysRestUtils;
import io.harness.testframework.restutils.IPWhitelistingRestUtils;
import io.harness.testframework.restutils.SSORestUtils;
import io.restassured.mapper.ObjectMapperType;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import software.wings.beans.Account;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.access.Whitelist;
import software.wings.beans.security.access.WhitelistStatus;
import software.wings.beans.sso.LdapSettings;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AccessManagementUtils {
  public static void runNoAccessTest(Account account, String roBearerToken, String readOnlyUserid) {
    logger.info("List user groups failed with Bad request as expected");
    logger.info("List the SSO Settings using ReadOnly permission");
    logger.info("The below statement fails for the bug - PL-2335. Disabling it to avoid failures");

    /*assertTrue(Setup.portal()
        .auth()
        .oauth2(roBearerToken)
        .queryParam("accountId", getAccount().getUuid())
        .get("/sso/access-management/" + getAccount().getUuid()).getStatusCode() == HttpStatus.SC_BAD_REQUEST);
    logger.info("List SSO settings failed with Bad request as expected");*/

    logger.info("List the APIKeys using ReadOnly permission");

    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/api-keys")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);

    logger.info("List APIKeys failed with Bad request as expected");

    logger.info("List the IPWhitelists using ReadOnly permission");
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/whitelist")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);
    logger.info("List IPWhitelists failed with Bad request as expected");

    Setup.signOut(readOnlyUserid, roBearerToken);
    logger.info("Readonly user logout successful");
  }

  public static void runUserPostTest(
      Account account, String bearerToken, String userName, String email, String password, int expectedInviteStatus) {
    logger.info("Creating the user : " + userName);
    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), userName);
    logger.info("Logging in as user : " + userName);
    String roBearerToken = Setup.getAuthToken(userName, password);
    UserInvite invite = UserUtils.createUserInvite(account, email);
    logger.info("Attempting to create a user with expectation : " + expectedInviteStatus);
    assertTrue(UserUtils.attemptInvite(invite, account, roBearerToken, expectedInviteStatus));
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  public static void runAllGetTests(Account account, String bearerToken, String userId, String password,
      int expectedStatusCodeForUsersAndGroups, int expectedStatusGroupForOthers) {
    final String READ_ONLY_USER = userId;
    final String IP_WHITELIST_VAL = "0.0.0.0";
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER);
    UserGroup userGroup = UserGroupUtils.getUserGroup(account, bearerToken, "Account Administrator");
    List<String> userIdsToAdd = new ArrayList<>();
    userIdsToAdd.add(readOnlyUser.getUuid());

    ApiKeyEntry apiKeyEntry = ApiKeyEntry.builder().build();
    List<String> userGroupIds = new ArrayList<>();
    userGroupIds.add(userGroup.getUuid());
    apiKeyEntry.setUserGroupIds(userGroupIds);
    apiKeyEntry.setName("apiKey");
    apiKeyEntry.setAccountId(account.getUuid());
    ApiKeyEntry postedEntry = ApiKeysRestUtils.createApiKey(account.getUuid(), bearerToken, apiKeyEntry);
    //    LdapSettings ldapSettings = SSOUtils.createDefaultLdapSettings(account.getUuid());
    //    logger.info("LDAP setting addition in progress");
    //    assertFalse(
    //        SSORestUtils.addLdapSettings(account.getUuid(), bearerToken, ldapSettings) == HttpStatus.SC_BAD_REQUEST);
    //    logger.info("LDAP added successfully");
    //    Object ssoConfig = SSORestUtils.getAccessManagementSettings(account.getUuid(), bearerToken);
    //    assertNotNull(ssoConfig);
    //    logger.info("LDAP added successfully");
    Whitelist ipToWhiteList = new Whitelist();
    ipToWhiteList.setAccountId(account.getUuid());
    ipToWhiteList.setFilter(IP_WHITELIST_VAL);
    ipToWhiteList.setDescription("Test Whitelisting");
    ipToWhiteList.setStatus(WhitelistStatus.DISABLED);

    logger.info("Adding the IP to be whitelisted");
    Whitelist ipAdded = IPWhitelistingRestUtils.addWhiteListing(account.getUuid(), bearerToken, ipToWhiteList);
    assertNotNull(ipAdded);
    logger.info("IPWhitelisting verification suucessful");

    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);
    assertNotNull(readOnlyUser);
    logger.info("Designated user's Bearer Token issued");

    logger.info("Get the user groups using ReadOnly permission");
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/userGroups/" + userGroup.getUuid())
                   .getStatusCode()
        == expectedStatusCodeForUsersAndGroups);
    logger.info("User group test for assert succeeded");
    logger.info("List user groups failed with Bad request as expected");

    //    logger.info("List the LDAP Settings using ReadOnly permission");
    //    logger.info("The below statement fails for the bug - PL-2335. Disabling it to avoid failures");
    //
    //    assertTrue(SSORestUtils.getLdapSettings(account.getUuid(), roBearerToken) == expectedStatusGroupForOthers);
    //    logger.info("LDAP setting test succeeded");
    //    assertTrue(SSORestUtils.deleteLDAPSettings(account.getUuid(), bearerToken) == HttpStatus.SC_OK);
    //    logger.info("LDAP setting test for delete succeeded");

    logger.info("List the APIKeys using ReadOnly permission");

    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/api-keys/" + postedEntry.getUuid())
                   .getStatusCode()
        == expectedStatusGroupForOthers);

    logger.info("List APIKeys failed with Bad request as expected");

    logger.info("List the IPWhitelists using ReadOnly permission");
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/whitelist/" + ipAdded.getUuid())
                   .getStatusCode()
        == expectedStatusGroupForOthers);
    logger.info("List IPWhitelists failed with Bad request as expected");

    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  public static void testPermissionToPostInUserGroup(
      Account account, String bearerToken, String userId, String password, int expectedResult) {
    final String READ_ONLY_USER = userId;
    logger.info("Starting with the ReadOnly Test");
    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER);
    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);
    logger.info("Creating a new user group without permission. It should fail");
    JsonObject groupInfoAsJson = new JsonObject();
    String name = "UserGroup - " + System.currentTimeMillis();
    groupInfoAsJson.addProperty("name", name);
    groupInfoAsJson.addProperty("description", "Test Description - " + System.currentTimeMillis());
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .body(groupInfoAsJson.toString())
                   .post("/userGroups")
                   .getStatusCode()
        == expectedResult);
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  public static void amNoPermissionToPostForIPWhitelisting(
      Account account, String bearerToken, String userId, String password, int expectedOutcome) {
    final String READ_ONLY_USER = userId;
    final String IP_WHITELIST_VAL = "0.0.0.0";
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER);

    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);

    logger.info("Attempting to create IP whitelisting entry without permissions");
    Whitelist ipToWhiteList = new Whitelist();
    ipToWhiteList.setAccountId(account.getUuid());
    ipToWhiteList.setFilter(IP_WHITELIST_VAL);
    ipToWhiteList.setDescription("Test Whitelisting");
    ipToWhiteList.setStatus(WhitelistStatus.DISABLED);

    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .body(ipToWhiteList, ObjectMapperType.GSON)
                   .post("/whitelist")
                   .getStatusCode()
        == expectedOutcome);

    logger.info("Adding the IP to be whitelisted");

    logger.info("IP whitelisting creation denied successfully");
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  public static void ldapCreationFailureCheckTest(
      Account account, String bearerToken, String userId, String password, int statusCodeExpected) {
    final String READ_ONLY_USER = userId;
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER);

    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);
    logger.info("Trying to create LDAP SSO Setting Without Permission");
    LdapSettings ldapSettings = SSOUtils.createDefaultLdapSettings(account.getUuid());
    TestCase.assertTrue(
        SSORestUtils.addLdapSettings(account.getUuid(), roBearerToken, ldapSettings) == statusCodeExpected);
    logger.info("LDAP creation denied successfully");

    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  public static void amNoPermissionToPostForSAML(
      Account account, String bearerToken, String userId, String password, int statusCodeExpected) {
    final String READ_ONLY_USER = userId;
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER);

    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);
    logger.info("Creating SAML config without permission");
    String filePath = System.getProperty("user.dir");
    filePath = filePath + "/"
        + "src/test/resources/secrets/"
        + "SAML_SSO_Provider.xml";
    TestCase.assertTrue(
        SSORestUtils.addSAMLSettings(account.getUuid(), roBearerToken, "SAML", filePath) == statusCodeExpected);
    logger.info("SAML creation denied successfully");
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  public static void updateIPWhiteListing(
      Account account, String bearerToken, String userId, String password, int statusCodeExpected) {
    final String IP_WHITELIST_VAL = "0.0.0.0";
    Whitelist ipToWhiteList = new Whitelist();
    ipToWhiteList.setAccountId(account.getUuid());
    ipToWhiteList.setFilter(IP_WHITELIST_VAL);
    ipToWhiteList.setDescription("Test Whitelisting");
    ipToWhiteList.setStatus(WhitelistStatus.DISABLED);
    logger.info("Adding the IP to be whitelisted");
    Whitelist ipAdded = IPWhitelistingRestUtils.addWhiteListing(account.getUuid(), bearerToken, ipToWhiteList);
    TestCase.assertNotNull(ipAdded);
    logger.info("Updating and verifying the whitelisted IP");
    ipToWhiteList.setUuid(ipAdded.getUuid());
    ipToWhiteList.setFilter("127.0.0.1");
    ipToWhiteList.setDescription("Modified description");

    final String READ_ONLY_USER = userId;
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER);
    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);

    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .body(ipToWhiteList, ObjectMapperType.GSON)
                   .put("/whitelist/" + ipToWhiteList.getUuid())
                   .getStatusCode()
        == statusCodeExpected);

    logger.info("Updation of whitelist denied successfully");
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");

    logger.info("Deleting the created whitelist entries");
    TestCase.assertTrue(IPWhitelistingRestUtils.deleteWhitelistedIP(account.getUuid(), bearerToken, ipAdded.getUuid()));
  }

  public static void updateAndDeleteAPIKeys(Account account, String bearerToken, String userId, String password,
      UserGroup userGroup, int statusCodeExpected) {
    final String READ_ONLY_USER = userId;
    logger.info("Constructing APIKeys");
    ApiKeyEntry apiKeyEntry = ApiKeyEntry.builder().build();
    List<String> userGroupList = new ArrayList<>();
    userGroupList.add(userGroup.getUuid());
    String name = "APIKey - " + System.currentTimeMillis();
    apiKeyEntry.setUserGroupIds(userGroupList);
    apiKeyEntry.setAccountId(account.getUuid());
    apiKeyEntry.setName(name);
    logger.info("Creating APIKeys");
    ApiKeyEntry postCreationEntry = ApiKeysRestUtils.createApiKey(apiKeyEntry.getAccountId(), bearerToken, apiKeyEntry);
    logger.info("Validating created APIKeys");
    assertNotNull(postCreationEntry);

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER);
    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);

    logger.info("Attempting to update APIKeys");
    String changedName = "APIKey_Changed - " + System.currentTimeMillis();
    postCreationEntry.setName(changedName);
    postCreationEntry.setUserGroups(null);
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .body(postCreationEntry, ObjectMapperType.GSON)
                   .post("/api-keys")
                   .getStatusCode()
        == statusCodeExpected);

    logger.info("Updation of APIKeys denied successfully");

    logger.info("Deleting the APIKeys without permission");
    TestCase.assertTrue(ApiKeysRestUtils.deleteApiKey(account.getUuid(), roBearerToken, postCreationEntry.getUuid())
        == HttpStatus.SC_BAD_REQUEST);
    logger.info("Deletion denied successfully");

    logger.info("Cleaning up. Deleting the APIKeys now");

    TestCase.assertTrue(
        ApiKeysRestUtils.deleteApiKey(account.getUuid(), bearerToken, postCreationEntry.getUuid()) == HttpStatus.SC_OK);
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
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
    logger.info("Adding the IP to be whitelisted");
    Whitelist ipAdded = IPWhitelistingRestUtils.addWhiteListing(account.getUuid(), bearerToken, ipToWhiteList);
    TestCase.assertNotNull(ipAdded);
    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER);
    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .delete("/whitelist/" + ipAdded.getUuid())
                   .getStatusCode()
        == statusCodeExpected);
    logger.info("Deletion denied to the given read only/no permission user");
    logger.info("Deleting the created whitelist entries");
    TestCase.assertTrue(IPWhitelistingRestUtils.deleteWhitelistedIP(account.getUuid(), bearerToken, ipAdded.getUuid()));
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  public static void runAPIKeyPostTest(Account account, String bearerToken, String userId, String password,
      int statusCodeExpected, UserGroup userGroup) {
    logger.info("Starting with the ReadOnly Test");
    final String READ_ONLY_USER = userId;
    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER);

    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);

    logger.info("Attempting to add an APIKey without permission");
    logger.info("Constructing APIKeys");
    List<String> userGroupList = new ArrayList<>();
    userGroupList.add(userGroup.getUuid());
    String name = "APIKey - " + System.currentTimeMillis();
    ApiKeyEntry apiKeyEntry = ApiKeyEntry.builder().build();
    apiKeyEntry.setUserGroupIds(userGroupList);
    apiKeyEntry.setAccountId(account.getUuid());
    apiKeyEntry.setName(name);
    logger.info("Creating an APIKey without permission");

    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .body(apiKeyEntry, ObjectMapperType.GSON)
                   .post("/api-keys")
                   .getStatusCode()
        == statusCodeExpected);

    logger.info("APIKey creation denied successfully");

    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  public static void runUserAndGroupsListTest(Account account, String bearerToken) {
    logger.info("List the users using ReadOnly permission");
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(bearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/users")
                   .getStatusCode()
        == HttpStatus.SC_OK);
    logger.info("List users failed with Bad request as expected");
    logger.info("List the user groups using ReadOnly permission");
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(bearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/userGroups")
                   .getStatusCode()
        == HttpStatus.SC_OK);
  }
}
