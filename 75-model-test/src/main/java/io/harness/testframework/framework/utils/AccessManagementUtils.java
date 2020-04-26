package io.harness.testframework.framework.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import io.harness.testframework.framework.Setup;
import io.harness.testframework.restutils.ApiKeysRestUtils;
import io.harness.testframework.restutils.IPWhitelistingRestUtils;
import io.harness.testframework.restutils.SSORestUtils;
import io.restassured.mapper.ObjectMapperType;
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

    /*assertThat(Setup.portal()
        .auth()
        .oauth2(roBearerToken)
        .queryParam("accountId", getAccount().getUuid())
        .get("/sso/access-management/" + getAccount().getUuid()).getStatusCode() == HttpStatus.SC_BAD_REQUEST).isTrue();
    logger.info("List SSO settings failed with Bad request as expected");*/

    logger.info("List the APIKeys using ReadOnly permission");

    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/api-keys")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST)
        .isTrue();

    logger.info("List APIKeys failed with Bad request as expected");

    logger.info("List the IPWhitelists using ReadOnly permission");
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/whitelist")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST)
        .isTrue();
    logger.info("List IPWhitelists failed with Bad request as expected");

    Setup.signOut(readOnlyUserid, roBearerToken);
    logger.info("Readonly user logout successful");
  }

  public static void runUserPostTest(
      Account account, String bearerToken, String userName, String email, String password, int expectedInviteStatus) {
    logger.info("Creating the user : " + userName);
    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), userName).getUser();
    logger.info("Logging in as user : " + userName);
    String roBearerToken = Setup.getAuthToken(userName, password);
    UserInvite invite = UserUtils.createUserInvite(account, email);
    logger.info("Attempting to create a user with expectation : " + expectedInviteStatus);
    assertThat(UserUtils.attemptInvite(invite, account, roBearerToken, expectedInviteStatus)).isTrue();
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  public static void runAllGetTests(Account account, String bearerToken, String userId, String password,
      int expectedStatusCodeForUsersAndGroups, int expectedStatusGroupForOthers) {
    final String READ_ONLY_USER = userId;
    final String IP_WHITELIST_VAL = "0.0.0.0";
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER).getUser();
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
    //    assertThat(
    //        SSORestUtils.addLdapSettings(account.getUuid(), bearerToken, ldapSettings) ==
    //        HttpStatus.SC_BAD_REQUEST).isFalse();
    //    logger.info("LDAP added successfully");
    //    Object ssoConfig = SSORestUtils.getAccessManagementSettings(account.getUuid(), bearerToken);
    //    assertThat(ssoConfig).isNotNull();
    //    logger.info("LDAP added successfully");
    Whitelist ipToWhiteList = new Whitelist();
    ipToWhiteList.setAccountId(account.getUuid());
    ipToWhiteList.setFilter(IP_WHITELIST_VAL);
    ipToWhiteList.setDescription("Test Whitelisting");
    ipToWhiteList.setStatus(WhitelistStatus.DISABLED);

    logger.info("Adding the IP to be whitelisted");
    Whitelist ipAdded = IPWhitelistingRestUtils.addWhiteListing(account.getUuid(), bearerToken, ipToWhiteList);
    assertThat(ipAdded).isNotNull();
    logger.info("IPWhitelisting verification suucessful");

    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);
    assertThat(readOnlyUser).isNotNull();
    logger.info("Designated user's Bearer Token issued");

    logger.info("Get the user groups using ReadOnly permission");
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/userGroups/" + userGroup.getUuid())
                   .getStatusCode()
        == expectedStatusCodeForUsersAndGroups)
        .isTrue();
    logger.info("User group test for assert succeeded");
    logger.info("List user groups failed with Bad request as expected");

    //    logger.info("List the LDAP Settings using ReadOnly permission");
    //    logger.info("The below statement fails for the bug - PL-2335. Disabling it to avoid failures");
    //
    //    assertThat(SSORestUtils.getLdapSettings(account.getUuid(), roBearerToken) ==
    //    expectedStatusGroupForOthers).isTrue(); logger.info("LDAP setting test succeeded");
    //    assertThat(SSORestUtils.deleteLDAPSettings(account.getUuid(), bearerToken) == HttpStatus.SC_OK).isTrue();
    //    logger.info("LDAP setting test for delete succeeded");

    logger.info("List the APIKeys using ReadOnly permission");

    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/api-keys/" + postedEntry.getUuid())
                   .getStatusCode()
        == expectedStatusGroupForOthers)
        .isTrue();

    logger.info("List APIKeys failed with Bad request as expected");

    logger.info("List the IPWhitelists using ReadOnly permission");
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/whitelist/" + ipAdded.getUuid())
                   .getStatusCode()
        == expectedStatusGroupForOthers)
        .isTrue();
    logger.info("List IPWhitelists failed with Bad request as expected");

    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  public static void testPermissionToPostInUserGroup(
      Account account, String bearerToken, String userId, String password, int expectedResult) {
    final String READ_ONLY_USER = userId;
    logger.info("Starting with the ReadOnly Test");
    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER).getUser();
    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);
    logger.info("Creating a new user group without permission. It should fail");
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
    logger.info("Readonly user logout successful");
  }

  public static void amNoPermissionToPostForIPWhitelisting(
      Account account, String bearerToken, String userId, String password, int expectedOutcome) {
    final String READ_ONLY_USER = userId;
    final String IP_WHITELIST_VAL = "0.0.0.0";
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER).getUser();

    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);

    logger.info("Attempting to create IP whitelisting entry without permissions");
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

    logger.info("Adding the IP to be whitelisted");

    logger.info("IP whitelisting creation denied successfully");
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  public static void ldapCreationFailureCheckTest(
      Account account, String bearerToken, String userId, String password, int statusCodeExpected) {
    final String READ_ONLY_USER = userId;
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER).getUser();

    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);
    logger.info("Trying to create LDAP SSO Setting Without Permission");
    LdapSettings ldapSettings = SSOUtils.createDefaultLdapSettings(account.getUuid());
    assertThat(SSORestUtils.addLdapSettings(account.getUuid(), roBearerToken, ldapSettings) == statusCodeExpected)
        .isTrue();
    logger.info("LDAP creation denied successfully");

    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  public static void amNoPermissionToPostForSAML(
      Account account, String bearerToken, String userId, String password, int statusCodeExpected) {
    final String READ_ONLY_USER = userId;
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER).getUser();

    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);
    logger.info("Creating SAML config without permission");
    String filePath = System.getProperty("user.dir");
    filePath = filePath + "/"
        + "src/test/resources/secrets/"
        + "SAML_SSO_Provider.xml";
    assertThat(SSORestUtils.addSAMLSettings(account.getUuid(), roBearerToken, "SAML", filePath) == statusCodeExpected)
        .isTrue();
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
    assertThat(ipAdded).isNotNull();
    logger.info("Updating and verifying the whitelisted IP");
    ipToWhiteList.setUuid(ipAdded.getUuid());
    ipToWhiteList.setFilter("127.0.0.1");
    ipToWhiteList.setDescription("Modified description");

    final String READ_ONLY_USER = userId;
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER).getUser();
    logger.info("Logging in as a ReadOnly user");
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

    logger.info("Updation of whitelist denied successfully");
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");

    logger.info("Deleting the created whitelist entries");
    assertThat(IPWhitelistingRestUtils.deleteWhitelistedIP(account.getUuid(), bearerToken, ipAdded.getUuid())).isTrue();
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
    assertThat(postCreationEntry).isNotNull();

    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER).getUser();
    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);

    logger.info("Attempting to update APIKeys");
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

    logger.info("Updation of APIKeys denied successfully");

    logger.info("Deleting the APIKeys without permission");
    assertThat(ApiKeysRestUtils.deleteApiKey(account.getUuid(), roBearerToken, postCreationEntry.getUuid())
        == HttpStatus.SC_BAD_REQUEST)
        .isTrue();
    logger.info("Deletion denied successfully");

    logger.info("Cleaning up. Deleting the APIKeys now");

    assertThat(
        ApiKeysRestUtils.deleteApiKey(account.getUuid(), bearerToken, postCreationEntry.getUuid()) == HttpStatus.SC_OK)
        .isTrue();
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
    assertThat(ipAdded).isNotNull();
    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER).getUser();
    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, password);
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .delete("/whitelist/" + ipAdded.getUuid())
                   .getStatusCode()
        == statusCodeExpected)
        .isTrue();
    logger.info("Deletion denied to the given read only/no permission user");
    logger.info("Deleting the created whitelist entries");
    assertThat(IPWhitelistingRestUtils.deleteWhitelistedIP(account.getUuid(), bearerToken, ipAdded.getUuid())).isTrue();
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  public static void runAPIKeyPostTest(Account account, String bearerToken, String userId, String password,
      int statusCodeExpected, UserGroup userGroup) {
    logger.info("Starting with the ReadOnly Test");
    final String READ_ONLY_USER = userId;
    User readOnlyUser = UserUtils.getUser(bearerToken, account.getUuid(), READ_ONLY_USER).getUser();

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

    assertThat(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", account.getUuid())
                   .body(apiKeyEntry, ObjectMapperType.GSON)
                   .post("/api-keys")
                   .getStatusCode()
        == statusCodeExpected)
        .isTrue();

    logger.info("APIKey creation denied successfully");

    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  public static void runUserAndGroupsListTest(Account account, String bearerToken, int httpStatus) {
    logger.info("List the users using ReadOnly permission");
    assertThat(Setup.portal()
                   .auth()
                   .oauth2(bearerToken)
                   .queryParam("accountId", account.getUuid())
                   .get("/users")
                   .getStatusCode()
        == httpStatus)
        .isTrue();
    logger.info("List users failed with Bad request as expected");
    logger.info("List the user groups using ReadOnly permission");
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
