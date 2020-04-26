package io.harness.e2e.dailysanity.platform.paid;

import static io.harness.rule.OwnerRule.NATARAJA;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import io.harness.category.element.E2ETests;
import io.harness.e2e.AbstractE2ETest;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.matchers.BooleanMatcher;
import io.harness.testframework.framework.matchers.NotNullMatcher;
import io.harness.testframework.framework.utils.SSOUtils;
import io.harness.testframework.framework.utils.TestUtils;
import io.harness.testframework.framework.utils.UserGroupUtils;
import io.harness.testframework.framework.utils.UserUtils;
import io.harness.testframework.restutils.SSORestUtils;
import io.harness.testframework.restutils.UserGroupRestUtils;
import io.harness.testframework.restutils.UserRestUtils;
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
import software.wings.security.authentication.OauthProviderType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class SSOTestPaidTest extends AbstractE2ETest {
  static final int MAX_RETRIES = 5;
  static final int DELAY_IN_MS = 6000;
  static final Retry<Object> retry = new Retry<>(MAX_RETRIES, DELAY_IN_MS);
  @Test()
  @Owner(developers = NATARAJA)
  @Category(E2ETests.class)
  public void createGoogleOauth() {
    logger.info("Starting the google oauth test");
    logger.info("Creating the oauth provider for google");
    Set<OauthProviderType> oauthProviderTypeSet = new HashSet<>();
    oauthProviderTypeSet.add(OauthProviderType.GOOGLE);
    OauthSettings oauthSettings =
        OauthSettings.builder().allowedProviders(oauthProviderTypeSet).displayName("Google").filter("").build();
    oauthSettings.setDisplayName("GOOGLE");
    assertThat(SSORestUtils.addOauthSettings(getAccount().getUuid(), bearerToken, oauthSettings) == HttpStatus.SC_OK)
        .isTrue();
    Object ssoConfig = SSORestUtils.getAccessManagementSettings(getAccount().getUuid(), bearerToken);
    assertThat(SSORestUtils.deleteOAUTHSettings(getAccount().getUuid(), bearerToken) == HttpStatus.SC_OK).isTrue();
    logger.info("Done");
  }

  @Test()
  @Owner(developers = NATARAJA)
  @Category(E2ETests.class)
  public void testLDAPAuthentication() {
    final String QUERY = "Manager";
    final String GROUP_NAME = "HR Managers";
    final String LDAP_LOGIN_ID = "cschmith@example.com";
    logger.info("Starting the LDAP test");
    logger.info("Creating LDAP SSO Setting");
    LdapSettings ldapSettings = SSOUtils.createDefaultLdapSettings(getAccount().getUuid());
    assertThat(SSORestUtils.addLdapSettings(getAccount().getUuid(), bearerToken, ldapSettings) == HttpStatus.SC_OK)
        .isTrue();
    Object ssoConfig = SSORestUtils.getAccessManagementSettings(getAccount().getUuid(), bearerToken);
    logger.info("Creating a userGroup");
    logger.info("Creating a new user group");
    JsonObject groupInfoAsJson = new JsonObject();
    String name = "UserGroup - " + System.currentTimeMillis();
    groupInfoAsJson.addProperty("name", name);
    groupInfoAsJson.addProperty("description", "Test Description - " + System.currentTimeMillis());
    UserGroup userGroup = UserGroupUtils.createUserGroup(getAccount(), bearerToken, groupInfoAsJson);
    assertThat(userGroup).isNotNull();
    String ldapId = SSOUtils.getLdapId(ssoConfig);
    logger.info("Doing an LDAP group search");
    Collection<LdapGroupResponse> ldapGroupResponses =
        SSORestUtils.searchLdapWithQuery(getAccount().getUuid(), bearerToken, QUERY, ldapId);
    assertThat(ldapGroupResponses.size() > 0).isTrue();
    LdapGroupResponse choosenGroup = SSOUtils.chooseLDAPGroup(ldapGroupResponses, GROUP_NAME);
    assertThat(choosenGroup).isNotNull();
    logger.info("Performing LDAP linking and syncing");
    UserGroup ldapLinkedGroup =
        UserGroupRestUtils.linkLDAPSettings(getAccount(), bearerToken, userGroup.getUuid(), ldapId, choosenGroup);
    assertThat(ldapLinkedGroup).isNotNull();
    UserGroup finalLdapLinkedGroup = ldapLinkedGroup;
    logger.info("Verifying the linking");
    boolean linkAndSyncSuccessful = (Boolean) retry.executeWithRetry(
        ()
            -> UserGroupUtils.hasUsersInUserGroup(getAccount(), bearerToken, finalLdapLinkedGroup.getName()),
        new BooleanMatcher<>(), true);
    assertThat(linkAndSyncSuccessful).isTrue();
    logger.info("Testing the LDAP login");
    String ldapLoginPassword = new ScmSecret().decryptToString(new SecretName("ldap_cschmith_password"));
    LdapResponse ldapResponse =
        SSORestUtils.testAuthenticate(getAccount().getUuid(), bearerToken, LDAP_LOGIN_ID, ldapLoginPassword);
    assertThat(ldapResponse).isNotNull();
    assertThat(ldapResponse.getStatus().name().equals("SUCCESS")).isTrue();
    logger.info("Testing the LDAP login - Succeeded");
    logger.info("Logging in using LDAP credentials");
    User user = UserUtils.getUser(bearerToken, getAccount().getUuid(), LDAP_LOGIN_ID).getUser();
    User user2 = UserUtils.getUser(bearerToken, getAccount().getUuid(), "ldaptest1@harness.io").getUser();
    assertThat(SSORestUtils.assignAuthMechanism(getAccount().getUuid(), bearerToken, "LDAP") == HttpStatus.SC_OK)
        .isTrue();
    TestUtils.sleep(30);
    String authToken = (String) retry.executeWithRetry(
        () -> Setup.getAuthToken(LDAP_LOGIN_ID, ldapLoginPassword), new NotNullMatcher(), true);
    assertThat(StringUtils.isNotBlank(authToken)).isTrue();
    logger.info("Logging out in as LDAP user");
    Setup.signOut(user.getUuid(), authToken);
    logger.info("Unlink LDAP user");
    ldapLinkedGroup = UserGroupRestUtils.unlinkLDAPSettings(getAccount(), bearerToken, userGroup.getUuid());
    UserGroup ldapUnlinkedGroup = ldapLinkedGroup;
    linkAndSyncSuccessful = (Boolean) retry.executeWithRetry(
        ()
            -> UserGroupUtils.hasUsersInUserGroup(getAccount(), bearerToken, ldapUnlinkedGroup.getName()),
        new BooleanMatcher<>(), false);
    assertThat(linkAndSyncSuccessful).isFalse();
    logger.info("Unlink successful");
    assertThat(
        SSORestUtils.assignAuthMechanism(getAccount().getUuid(), bearerToken, "USER_PASSWORD") == HttpStatus.SC_OK)
        .isTrue();
    logger.info("Disabled LDAP");
    assertThat(SSORestUtils.deleteLDAPSettings(getAccount().getUuid(), bearerToken) == HttpStatus.SC_OK).isTrue();
    logger.info("Deleted LDAP");
    assertThat(UserGroupRestUtils.deleteUserGroup(getAccount(), bearerToken, ldapUnlinkedGroup.getUuid())).isTrue();
    logger.info("Deleted Usergroup");
    logger.info("Deleting user : " + user.getEmail() + " and " + user2.getEmail());
    assertThat(UserRestUtils.deleteUser(getAccount().getUuid(), bearerToken, user.getUuid()) == HttpStatus.SC_OK)
        .isTrue();
    assertThat(UserRestUtils.deleteUser(getAccount().getUuid(), bearerToken, user2.getUuid()) == HttpStatus.SC_OK)
        .isTrue();
    logger.info("Done");
  }

  @Test()
  @Owner(developers = NATARAJA)
  @Category(E2ETests.class)
  public void createSAMLSettingsInPaid() {
    logger.info("Starting the SAML test");
    String filePath = System.getProperty("user.dir");
    filePath = filePath + "/"
        + "src/test/resources/secrets/"
        + "SAML_SSO_Provider.xml";
    assertThat(SSORestUtils.addSAMLSettings(getAccount().getUuid(), bearerToken, "SAML", filePath) == HttpStatus.SC_OK)
        .isTrue();
    Object ssoConfig = SSORestUtils.getAccessManagementSettings(getAccount().getUuid(), bearerToken);
    assertThat(SSORestUtils.deleSAMLSettings(getAccount().getUuid(), bearerToken) == HttpStatus.SC_OK).isTrue();
    logger.info("Done");
  }
}
