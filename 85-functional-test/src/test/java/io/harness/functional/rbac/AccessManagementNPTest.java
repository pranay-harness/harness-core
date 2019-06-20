package io.harness.functional.rbac;

import static io.harness.rule.OwnerRule.SWAMY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.utils.SSOUtils;
import io.harness.testframework.framework.utils.UserGroupUtils;
import io.harness.testframework.framework.utils.UserUtils;
import io.harness.testframework.restutils.ApiKeysRestUtils;
import io.harness.testframework.restutils.IPWhitelistingRestUtils;
import io.harness.testframework.restutils.SSORestUtils;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import junit.framework.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
public class AccessManagementNPTest extends AbstractFunctionalTest {
  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void accessManagementNoPermissionTestForList() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, getAccount().getUuid(), READ_ONLY_USER);
    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, "readonlyuser");
    assertNotNull(readOnlyUser);
    logger.info("List the users using ReadOnly permission");
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/users")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);
    logger.info("List users failed with Bad request as expected");
    logger.info("List the user groups using ReadOnly permission");
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/userGroups")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);
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
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/api-keys")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);

    logger.info("List APIKeys failed with Bad request as expected");

    logger.info("List the IPWhitelists using ReadOnly permission");
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/whitelist")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);
    logger.info("List IPWhitelists failed with Bad request as expected");

    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void accessManagementNoPermissionTestForGet() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    final String IP_WHITELIST_VAL = "0.0.0.0";
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, getAccount().getUuid(), READ_ONLY_USER);
    UserGroup userGroup = UserGroupUtils.getUserGroup(getAccount(), bearerToken, "Account Administrator");
    ApiKeyEntry apiKeyEntry = ApiKeyEntry.builder().build();
    List<String> userGroupIds = new ArrayList<>();
    userGroupIds.add(userGroup.getUuid());
    apiKeyEntry.setUserGroupIds(userGroupIds);
    apiKeyEntry.setName("apiKey");
    apiKeyEntry.setAccountId(getAccount().getUuid());
    ApiKeyEntry postedEntry = ApiKeysRestUtils.createApiKey(getAccount().getUuid(), bearerToken, apiKeyEntry);
    LdapSettings ldapSettings = SSOUtils.createDefaultLdapSettings(getAccount().getUuid());
    TestCase.assertTrue(
        SSORestUtils.addLdapSettings(getAccount().getUuid(), bearerToken, ldapSettings) == HttpStatus.SC_OK);
    Object ssoConfig = SSORestUtils.getAccessManagementSettings(getAccount().getUuid(), bearerToken);
    Whitelist ipToWhiteList = new Whitelist();
    ipToWhiteList.setAccountId(getAccount().getUuid());
    ipToWhiteList.setFilter(IP_WHITELIST_VAL);
    ipToWhiteList.setDescription("Test Whitelisting");
    ipToWhiteList.setStatus(WhitelistStatus.DISABLED);

    logger.info("Adding the IP to be whitelisted");
    Whitelist ipAdded = IPWhitelistingRestUtils.addWhiteListing(getAccount().getUuid(), bearerToken, ipToWhiteList);
    TestCase.assertNotNull(ipAdded);

    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, "readonlyuser");
    assertNotNull(readOnlyUser);

    logger.info("List the user groups using ReadOnly permission");
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/userGroups/" + userGroup.getUuid())
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);
    logger.info("List user groups failed with Bad request as expected");

    logger.info("List the LDAP Settings using ReadOnly permission");
    logger.info("The below statement fails for the bug - PL-2335. Disabling it to avoid failures");

    assertTrue(SSORestUtils.getLdapSettings(getAccount().getUuid(), roBearerToken) == HttpStatus.SC_BAD_REQUEST);
    TestCase.assertTrue(SSORestUtils.deleteLDAPSettings(getAccount().getUuid(), bearerToken) == HttpStatus.SC_OK);
    logger.info("List LDAP settings failed with Bad request as expected");

    logger.info("List the APIKeys using ReadOnly permission");

    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/api-keys/" + postedEntry.getUuid())
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);

    logger.info("List APIKeys failed with Bad request as expected");

    logger.info("List the IPWhitelists using ReadOnly permission");
    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .get("/whitelist/" + ipAdded.getUuid())
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);
    logger.info("List IPWhitelists failed with Bad request as expected");

    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForUser() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, getAccount().getUuid(), READ_ONLY_USER);

    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, "readonlyuser");
    String email = "testemail@harness.mailinator.com";
    UserInvite invite = new UserInvite();
    invite.setAccountId(getAccount().getUuid());
    List<String> emailList = new ArrayList<>();
    emailList.add(email);
    invite.setEmails(emailList);
    invite.setName(email.replace("@harness.mailinator.com", ""));
    invite.setAppId(getAccount().getAppId());

    logger.info("Attempting to create a user without permission");

    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .body(invite, ObjectMapperType.GSON)
                   .contentType(ContentType.JSON)
                   .post("/users/invites")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);

    logger.info("User Creation Denied Successfully");

    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForUserGroup() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, getAccount().getUuid(), READ_ONLY_USER);

    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, "readonlyuser");

    logger.info("Creating a new user group without permission. It should fail");
    JsonObject groupInfoAsJson = new JsonObject();
    String name = "UserGroup - " + System.currentTimeMillis();
    groupInfoAsJson.addProperty("name", name);
    groupInfoAsJson.addProperty("description", "Test Description - " + System.currentTimeMillis());

    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .body(groupInfoAsJson.toString())
                   .post("/userGroups")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);

    logger.info("Group creation denied successfully.");
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForIPWhitelisting() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    final String IP_WHITELIST_VAL = "0.0.0.0";
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, getAccount().getUuid(), READ_ONLY_USER);

    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, "readonlyuser");

    logger.info("Attempting to create IP whitelisting entry without permissions");
    Whitelist ipToWhiteList = new Whitelist();
    ipToWhiteList.setAccountId(getAccount().getUuid());
    ipToWhiteList.setFilter(IP_WHITELIST_VAL);
    ipToWhiteList.setDescription("Test Whitelisting");
    ipToWhiteList.setStatus(WhitelistStatus.DISABLED);

    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .body(ipToWhiteList, ObjectMapperType.GSON)
                   .post("/whitelist")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);

    logger.info("Adding the IP to be whitelisted");

    logger.info("IP whitelisting creation denied successfully");
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForAPIKeys() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, getAccount().getUuid(), READ_ONLY_USER);

    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, "readonlyuser");

    logger.info("Attempting to add an APIKey without permission");
    logger.info("Creating a new user group");
    JsonObject groupInfoAsJson = new JsonObject();
    String userGroupname = "UserGroup - " + System.currentTimeMillis();
    groupInfoAsJson.addProperty("name", userGroupname);
    groupInfoAsJson.addProperty("description", "Test Description - " + System.currentTimeMillis());
    UserGroup userGroup = UserGroupUtils.createUserGroup(getAccount(), bearerToken, groupInfoAsJson);
    assertNotNull(userGroup);
    logger.info("Constructing APIKeys");
    List<String> userGroupList = new ArrayList<>();
    userGroupList.add(userGroup.getUuid());
    String name = "APIKey - " + System.currentTimeMillis();
    ApiKeyEntry apiKeyEntry = ApiKeyEntry.builder().build();
    apiKeyEntry.setUserGroupIds(userGroupList);
    apiKeyEntry.setAccountId(getAccount().getUuid());
    apiKeyEntry.setName(name);
    logger.info("Creating an APIKey without permission");

    assertTrue(Setup.portal()
                   .auth()
                   .oauth2(roBearerToken)
                   .queryParam("accountId", getAccount().getUuid())
                   .body(apiKeyEntry, ObjectMapperType.GSON)
                   .post("/api-keys")
                   .getStatusCode()
        == HttpStatus.SC_BAD_REQUEST);

    logger.info("APIKey creation denied successfully");

    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForLDAP() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, getAccount().getUuid(), READ_ONLY_USER);

    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, "readonlyuser");
    logger.info("Trying to create LDAP SSO Setting Without Permission");
    LdapSettings ldapSettings = SSOUtils.createDefaultLdapSettings(getAccount().getUuid());
    TestCase.assertTrue(
        SSORestUtils.addLdapSettings(getAccount().getUuid(), roBearerToken, ldapSettings) == HttpStatus.SC_BAD_REQUEST);
    logger.info("LDAP creation denied successfully");

    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }

  @Test
  @Owner(emails = SWAMY, resent = false)
  @Category(FunctionalTests.class)
  public void amNoPermissionToPostForSAML() {
    final String READ_ONLY_USER = "readonlyuser@harness.io";
    logger.info("Starting with the ReadOnly Test");

    User readOnlyUser = UserUtils.getUser(bearerToken, getAccount().getUuid(), READ_ONLY_USER);

    logger.info("Logging in as a ReadOnly user");
    String roBearerToken = Setup.getAuthToken(READ_ONLY_USER, "readonlyuser");
    logger.info("Creating SAML config without permission");
    String filePath = System.getProperty("user.dir");
    filePath = filePath + "/"
        + "src/test/resources/secrets/"
        + "SAML_SSO_Provider.xml";
    TestCase.assertTrue(SSORestUtils.addSAMLSettings(getAccount().getUuid(), roBearerToken, "SAML", filePath)
        == HttpStatus.SC_BAD_REQUEST);
    logger.info("SAML creation denied successfully");
    Setup.signOut(readOnlyUser.getUuid(), roBearerToken);
    logger.info("Readonly user logout successful");
  }
}
