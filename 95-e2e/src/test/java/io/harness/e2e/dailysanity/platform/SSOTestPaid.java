package io.harness.e2e.dailysanity.platform;

import static io.harness.rule.OwnerRule.SWAMY;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.google.gson.JsonObject;

import io.harness.category.element.E2ETests;
import io.harness.e2e.AbstractE2ETest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.matchers.BooleanMatcher;
import io.harness.testframework.framework.utils.SSOUtils;
import io.harness.testframework.framework.utils.UserGroupUtils;
import io.harness.testframework.framework.utils.UserUtils;
import io.harness.testframework.restutils.SSORestUtils;
import io.harness.testframework.restutils.UserGroupRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.beans.sso.LdapGroupResponse;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.OauthSettings;
import software.wings.helpers.ext.ldap.LdapResponse;

import java.util.Collection;

@Slf4j
public class SSOTestPaid extends AbstractE2ETest {
  static final int MAX_RETRIES = 5;
  static final int DELAY_IN_MS = 6000;
  static final Retry<Object> retry = new Retry<>(MAX_RETRIES, DELAY_IN_MS);
  @Test()
  @Owner(emails = SWAMY, resent = false)
  @Category(E2ETests.class)
  public void createGoogleOauth() {
    logger.info("Starting the google oauth test");
    logger.info("Creating the oauth provider for google");
    OauthSettings oauthSettings = OauthSettings.builder().build();
    oauthSettings.setFilter("");
    oauthSettings.setDisplayName("google");
    assertTrue(SSORestUtils.addOauthSettings(getAccount().getUuid(), bearerToken, oauthSettings) == HttpStatus.SC_OK);
    Object ssoConfig = SSORestUtils.getAccessManagementSettings(getAccount().getUuid(), bearerToken);
    assertTrue(SSORestUtils.deleteOAUTHSettings(getAccount().getUuid(), bearerToken) == HttpStatus.SC_OK);
    logger.info("Done");
  }

  @Test()
  @Owner(emails = SWAMY, resent = false)
  @Category(E2ETests.class)
  public void testLDAPAuthentication() {
    final String QUERY = "Manager";
    final String GROUP_NAME = "HR Managers";
    final String LDAP_LOGIN_ID = "cschmith@example.com";
    logger.info("Starting the LDAP test");
    logger.info("Creating LDAP SSO Setting");
    LdapSettings ldapSettings = SSOUtils.createDefaultLdapSettings(getAccount().getUuid());
    assertTrue(SSORestUtils.addLdapSettings(getAccount().getUuid(), bearerToken, ldapSettings) == HttpStatus.SC_OK);
    Object ssoConfig = SSORestUtils.getAccessManagementSettings(getAccount().getUuid(), bearerToken);
    logger.info("Creating a userGroup");
    logger.info("Creating a new user group");
    JsonObject groupInfoAsJson = new JsonObject();
    String name = "UserGroup - " + System.currentTimeMillis();
    groupInfoAsJson.addProperty("name", name);
    groupInfoAsJson.addProperty("description", "Test Description - " + System.currentTimeMillis());
    UserGroup userGroup = UserGroupUtils.createUserGroup(getAccount(), bearerToken, groupInfoAsJson);
    assertNotNull(userGroup);
    String ldapId = SSOUtils.getLdapId(ssoConfig);
    logger.info("Doing an LDAP group search");
    Collection<LdapGroupResponse> ldapGroupResponses =
        SSORestUtils.searchLdapWithQuery(getAccount().getUuid(), bearerToken, QUERY, ldapId);
    assertTrue(ldapGroupResponses.size() > 0);
    LdapGroupResponse choosenGroup = SSOUtils.chooseLDAPGroup(ldapGroupResponses, GROUP_NAME);
    assertNotNull(choosenGroup);
    logger.info("Performing LDAP linking and syncing");
    UserGroup ldapLinkedGroup =
        UserGroupRestUtils.linkLDAPSettings(getAccount(), bearerToken, userGroup.getUuid(), ldapId, choosenGroup);
    assertNotNull(ldapLinkedGroup);
    UserGroup finalLdapLinkedGroup = ldapLinkedGroup;
    logger.info("Verifying the linking");
    boolean linkAndSyncSuccessful = (Boolean) retry.executeWithRetry(
        ()
            -> UserGroupUtils.hasUsersInUserGroup(getAccount(), bearerToken, finalLdapLinkedGroup.getName()),
        new BooleanMatcher<>(), true);
    assertTrue(linkAndSyncSuccessful);
    logger.info("Logging in as LDAP user");
    String ldapLoginPassword = new ScmSecret().decryptToString(new SecretName("ldap_cschmith_password"));
    LdapResponse ldapResponse =
        SSORestUtils.testAuthenticate(getAccount().getUuid(), bearerToken, LDAP_LOGIN_ID, ldapLoginPassword);
    assertNotNull(ldapResponse);
    User user = UserUtils.getUser(bearerToken, getAccount().getUuid(), LDAP_LOGIN_ID);
    assertTrue(SSORestUtils.assignAuthMechanism(getAccount().getUuid(), bearerToken, "LDAP") == HttpStatus.SC_OK);
    String authToken = Setup.getAuthToken(LDAP_LOGIN_ID, ldapLoginPassword);
    assertTrue(StringUtils.isNotBlank(authToken));
    logger.info("Logging out in as LDAP user");
    Setup.signOut(user.getUuid(), authToken);
    logger.info("Unlink LDAP user");
    ldapLinkedGroup = UserGroupRestUtils.unlinkLDAPSettings(getAccount(), bearerToken, userGroup.getUuid());
    UserGroup ldapUnlinkedGroup = ldapLinkedGroup;
    linkAndSyncSuccessful = (Boolean) retry.executeWithRetry(
        ()
            -> UserGroupUtils.hasUsersInUserGroup(getAccount(), bearerToken, ldapUnlinkedGroup.getName()),
        new BooleanMatcher<>(), false);
    assertFalse(linkAndSyncSuccessful);
    logger.info("Unlink successful");
    assertTrue(
        SSORestUtils.assignAuthMechanism(getAccount().getUuid(), bearerToken, "USER_PASSWORD") == HttpStatus.SC_OK);
    logger.info("Disabled LDAP");
    assertTrue(SSORestUtils.deleteLDAPSettings(getAccount().getUuid(), bearerToken) == HttpStatus.SC_OK);
    logger.info("Deleted LDAP");
    assertTrue(UserGroupRestUtils.deleteUserGroup(getAccount(), bearerToken, ldapUnlinkedGroup.getUuid()));
    logger.info("Deleted Usergroup");
    logger.info("Done");
  }

  @Test()
  @Owner(emails = SWAMY, resent = false)
  @Category(E2ETests.class)
  public void createSAMLSettingsInPaid() {
    logger.info("Starting the LDAP test");
    String filePath = System.getProperty("user.dir");
    filePath = filePath + "/"
        + "src/test/resources/secrets/"
        + "SAML_SSO_Provider.xml";
    assertTrue(SSORestUtils.addSAMLSettings(getAccount().getUuid(), bearerToken, "SAML", filePath) == HttpStatus.SC_OK);
    Object ssoConfig = SSORestUtils.getAccessManagementSettings(getAccount().getUuid(), bearerToken);
    assertTrue(SSORestUtils.deleSAMLSettings(getAccount().getUuid(), bearerToken) == HttpStatus.SC_OK);
    logger.info("Done");
  }
}
