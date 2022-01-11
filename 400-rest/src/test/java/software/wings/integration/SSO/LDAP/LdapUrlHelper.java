/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration.SSO.LDAP;

import static software.wings.integration.SSO.LDAP.LdapIntegrationTestConstants.ACCOUNT_ID;
import static software.wings.integration.SSO.LDAP.LdapIntegrationTestConstants.ACCOUNT_ID_PARAM;
import static software.wings.integration.SSO.LDAP.LdapIntegrationTestConstants.USER_GROUP_ID;

import software.wings.utils.WingsIntegrationTestConstants;

import java.net.URISyntaxException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.http.client.utils.URIBuilder;

@FieldDefaults(level = AccessLevel.PUBLIC)
public class LdapUrlHelper implements WingsIntegrationTestConstants {
  static String createLinkGroupByUrl(String ldapSettingsId) {
    return addAccountIdToURI(API_BASE + "/userGroups/" + USER_GROUP_ID + "/link/ldap/" + ldapSettingsId);
  }

  static String createUnlinkGroupByUrl() {
    return addAccountIdAndQueryParamsToURI(
        API_BASE + "/userGroups/" + USER_GROUP_ID + "/unlink", "retainMembers", "true");
  }

  static String createSearchGroupByNameUrl(String ldapSettingsId) {
    return addAccountIdAndQueryParamsToURI(API_BASE + "/sso/ldap/" + ldapSettingsId + "/search/group", "q", "Admin");
  }

  static String createEnableLdapAsDefaultLoginMechanismUrl() {
    return addAccountIdToURI(API_BASE + "/sso/auth-mechanism/LDAP");
  }

  static String createEnableUserPassAsDefaultLoginMechanismUrl() {
    return addAccountIdToURI(API_BASE + "/sso/auth-mechanism/USER_PASSWORD");
  }

  static String createTestLdapGroupSettingsURL() {
    return addAccountIdToURI(API_BASE + "/sso/ldap/settings/test/group");
  }

  static String createTestLdapUserSettingsURL() {
    return addAccountIdToURI(API_BASE + "/sso/ldap/settings/test/user");
  }

  static String createTestLdapConnSettingsURL() {
    return addAccountIdToURI(API_BASE + "/sso/ldap/settings/test/connection");
  }

  static String createUploadLdapSettingsURL() {
    return addAccountIdToURI(API_BASE + "/sso/ldap/settings");
  }

  private static String addAccountIdToURI(String url) {
    try {
      return new URIBuilder(url).addParameter(ACCOUNT_ID_PARAM, ACCOUNT_ID).toString();
    } catch (URISyntaxException e) {
      throw new RuntimeException("Ldap Integration test failed.", e);
    }
  }

  private static String addAccountIdAndQueryParamsToURI(String url, String param, String value) {
    try {
      return new URIBuilder(url).addParameter(ACCOUNT_ID_PARAM, ACCOUNT_ID).addParameter(param, value).toString();
    } catch (URISyntaxException e) {
      throw new RuntimeException("Ldap Integration test failed.", e);
    }
  }
}
