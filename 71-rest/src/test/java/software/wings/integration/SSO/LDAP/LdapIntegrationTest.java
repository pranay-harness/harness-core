package software.wings.integration.SSO.LDAP;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static software.wings.integration.SSO.LDAP.LdapIntegrationTestConstants.ACCOUNT_ID;
import static software.wings.integration.SSO.LDAP.LdapIntegrationTestConstants.ADMIN_HARNESS_ID;
import static software.wings.integration.SSO.LDAP.LdapIntegrationTestConstants.INVALID_TOKEN;
import static software.wings.integration.SSO.LDAP.LdapIntegrationTestConstants.LDAP_GROUP_DN_TO_LINK_TO_HARNESS_GROUP;
import static software.wings.integration.SSO.LDAP.LdapIntegrationTestConstants.adminAccountLdapPass;
import static software.wings.integration.SSO.LDAP.LdapTestHelper.buildLdapSettings;
import static software.wings.integration.SSO.LDAP.LdapUrlHelper.createEnableLdapAsDefaultLoginMechanismUrl;
import static software.wings.integration.SSO.LDAP.LdapUrlHelper.createEnableUserPassAsDefaultLoginMechanismUrl;
import static software.wings.integration.SSO.LDAP.LdapUrlHelper.createLinkGroupByUrl;
import static software.wings.integration.SSO.LDAP.LdapUrlHelper.createSearchGroupByNameUrl;
import static software.wings.integration.SSO.LDAP.LdapUrlHelper.createTestLdapConnSettingsURL;
import static software.wings.integration.SSO.LDAP.LdapUrlHelper.createTestLdapGroupSettingsURL;
import static software.wings.integration.SSO.LDAP.LdapUrlHelper.createTestLdapUserSettingsURL;
import static software.wings.integration.SSO.LDAP.LdapUrlHelper.createUnlinkGroupByUrl;
import static software.wings.integration.SSO.LDAP.LdapUrlHelper.createUploadLdapSettingsURL;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.rest.RestResponse;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.security.UserGroup;
import software.wings.beans.sso.LdapGroupResponse;
import software.wings.beans.sso.LdapLinkGroupRequest;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.LdapTestResponse;
import software.wings.beans.sso.LdapTestResponse.Status;
import software.wings.integration.BaseIntegrationTest;
import software.wings.resources.SSOResource.LDAPTestAuthenticationRequest;
import software.wings.security.authentication.AuthenticationUtils;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.utils.WingsIntegrationTestConstants;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

/**
 * Integration test for Ldap.
 */
@Slf4j
public class LdapIntegrationTest extends BaseIntegrationTest implements WingsIntegrationTestConstants {
  @Inject private SSOService ssoService;
  @Inject private UserGroupService userGroupService;
  @Inject private UserService userService;
  @Inject private AuthenticationUtils authenticationUtil;

  private Client client;
  private LdapSettings ldapSettings;

  @Before
  public void setupLdap() throws KeyManagementException, NoSuchAlgorithmException {
    client = LdapTestHelper.getClient();
    loginAdminUser();
    deleteExistingLdapSettings();
    ldapSettings = buildLdapSettings();
    LdapIntegrationTestConstants.ACCOUNT_ID =
        authenticationUtil.getPrimaryAccount(authenticationUtil.getUser(ADMIN_HARNESS_ID)).getUuid();
    LdapIntegrationTestConstants.USER_GROUP_ID =
        userGroupService.getUserGroupsByAccountId(ACCOUNT_ID, userService.getUserByEmail(ADMIN_HARNESS_ID))
            .get(0)
            .getUuid();
    userToken = INVALID_TOKEN;
  }

  @Test
  @Category(IntegrationTests.class)
  public void testLdapConnectionSettings() {
    loginAdminUser();
    assertNotEquals(userToken, INVALID_TOKEN);
    createAndTestLdapSettings();

    // tests happy cases for ldap test api's
    ldapConnectionSettingsTest();
    ldapUserSettingsTest();
    ldapGroupSettingsTest();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testLdapLogin() {
    loginAdminUser();
    assertNotEquals(userToken, INVALID_TOKEN);
    createAndTestLdapSettings();

    // Authentication testing for Ldap
    enableLdapAsDefaultLoginMechanism();
    userToken = INVALID_TOKEN;
    loginUsingLdap();
    assertNotEquals(userToken, INVALID_TOKEN);
    revertDefaultLoginToUserPassword();
  }

  @Test
  @Category(IntegrationTests.class)
  public void testLdapUserGroupLinking() {
    loginAdminUser();
    assertNotEquals(userToken, INVALID_TOKEN);
    createAndTestLdapSettings();

    // GroupLinking testing
    searchGroupByNameTest();
    linkLdapGroupToHarnessUserGroupTest();
    unlinkLdapGroupToHarnessUserGroupTest();
    revertDefaultLoginToUserPassword();
  }

  private void revertDefaultLoginToUserPassword() {
    String url = createEnableUserPassAsDefaultLoginMechanismUrl();
    logger.info("Calling url: " + url);
    WebTarget target = client.target(url);
    Response response = getRequestBuilderWithAuthHeader(target).put(Entity.json(JsonUtils.asJson("")));
    assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
  }

  public void createAndTestLdapSettings() {
    String url = createUploadLdapSettingsURL();
    logger.info("Calling url: " + url);
    WebTarget target = client.target(url);
    RestResponse<LdapSettings> ldapSettingsRestResponse = getRequestBuilderWithAuthHeader(target).post(
        Entity.json(JsonUtils.asJson(ldapSettings)), new GenericType<RestResponse<LdapSettings>>() {});
    LdapSettings resource = ldapSettingsRestResponse.getResource();
    assertEquals(resource.getAccountId(), ACCOUNT_ID);
    assertNotNull(resource.getConnectionSettings());
    assertEquals(resource.getUserSettingsList().size(), 1);
    assertEquals(resource.getGroupSettingsList().size(), 2);
  }

  public void ldapConnectionSettingsTest() {
    String url = createTestLdapConnSettingsURL();
    logger.info("Calling url: " + url);
    WebTarget target = client.target(url);
    RestResponse<LdapTestResponse> ldapSettingsRestResponse = getRequestBuilderWithAuthHeader(target).post(
        Entity.json(JsonUtils.asJson(ldapSettings)), new GenericType<RestResponse<LdapTestResponse>>() {});
    LdapTestResponse resource = ldapSettingsRestResponse.getResource();
    assertEquals(Status.SUCCESS, resource.getStatus());
  }

  public void ldapUserSettingsTest() {
    String url = createTestLdapUserSettingsURL();
    logger.info("Calling url: " + url);
    WebTarget target = client.target(url);
    RestResponse<LdapTestResponse> ldapSettingsRestResponse = getRequestBuilderWithAuthHeader(target).post(
        Entity.json(JsonUtils.asJson(ldapSettings)), new GenericType<RestResponse<LdapTestResponse>>() {});
    LdapTestResponse resource = ldapSettingsRestResponse.getResource();
    assertEquals(Status.SUCCESS, resource.getStatus());
  }

  public void ldapGroupSettingsTest() {
    String url = createTestLdapGroupSettingsURL();
    logger.info("Calling url: " + url);
    WebTarget target = client.target(url);
    RestResponse<LdapTestResponse> ldapSettingsRestResponse = getRequestBuilderWithAuthHeader(target).post(
        Entity.json(JsonUtils.asJson(ldapSettings)), new GenericType<RestResponse<LdapTestResponse>>() {});
    LdapTestResponse resource = ldapSettingsRestResponse.getResource();
    assertEquals(Status.SUCCESS, resource.getStatus());
  }

  public void enableLdapAsDefaultLoginMechanism() {
    String url = createEnableLdapAsDefaultLoginMechanismUrl();
    logger.info("Calling url: " + url);
    WebTarget target = client.target(url);
    LDAPTestAuthenticationRequest ldapTestAuthenticationRequest = LdapTestHelper.getAuthenticationRequestObject();
    Response response =
        getRequestBuilderWithAuthHeader(target).put(Entity.json(JsonUtils.asJson(ldapTestAuthenticationRequest)));
    assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
  }

  public void searchGroupByNameTest() {
    LdapSettings ldapSettings = ssoService.getLdapSettings(ACCOUNT_ID);
    String url = createSearchGroupByNameUrl(ldapSettings.getUuid());
    logger.info("Calling url: " + url);
    WebTarget target = client.target(url);
    RestResponse<Collection<LdapGroupResponse>> collectionRestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<Collection<LdapGroupResponse>>>() {});
    Collection<LdapGroupResponse> resource = collectionRestResponse.getResource();

    resource.forEach(groupResponse -> { assertTrue(groupResponse.getName().toLowerCase().contains("admin")); });
    assertTrue(resource.size() > 0);
  }

  public void linkLdapGroupToHarnessUserGroupTest() {
    enableLdapAsDefaultLoginMechanism();
    loginUsingLdap();
    LdapSettings ldapSettings = ssoService.getLdapSettings(ACCOUNT_ID);
    String url = createLinkGroupByUrl(ldapSettings.getUuid());
    logger.info("Calling url: " + url);
    WebTarget target = client.target(url);
    LdapLinkGroupRequest ldapLinkGroupRequest = LdapTestHelper.getLdapLinkGroupRequest();

    RestResponse<UserGroup> userGroupRestResponse = getRequestBuilderWithAuthHeader(target).put(
        Entity.json(JsonUtils.asJson(ldapLinkGroupRequest)), new GenericType<RestResponse<UserGroup>>() {});
    UserGroup userGroup = userGroupRestResponse.getResource();
    assertTrue(userGroup.isSsoLinked());
    assertEquals(userGroup.getSsoGroupId(), LDAP_GROUP_DN_TO_LINK_TO_HARNESS_GROUP);
  }

  public void unlinkLdapGroupToHarnessUserGroupTest() {
    String url = createUnlinkGroupByUrl();
    logger.info("Calling url: " + url);
    WebTarget target = client.target(url);

    RestResponse<UserGroup> userGroupResponse =
        getRequestBuilderWithAuthHeader(target).put(Entity.json(""), new GenericType<RestResponse<UserGroup>>() {});
    UserGroup userGroup = userGroupResponse.getResource();
    assertFalse(userGroup.isSsoLinked());
  }

  public void loginUsingLdap() {
    String loginUserToken = loginUser(ADMIN_HARNESS_ID, adminAccountLdapPass);
    assertNotEquals(loginUserToken, INVALID_TOKEN);
  }

  private void deleteExistingLdapSettings() {
    if (ssoService.getLdapSettings(ACCOUNT_ID) != null) {
      ssoService.deleteLdapSettings(ACCOUNT_ID);
    }
  }
}
