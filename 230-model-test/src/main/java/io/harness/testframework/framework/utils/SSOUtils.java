/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework.utils;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;

import software.wings.beans.sso.LdapConnectionSettings;
import software.wings.beans.sso.LdapGroupResponse;
import software.wings.beans.sso.LdapGroupSettings;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.LdapUserSettings;

import com.google.gson.internal.LinkedTreeMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class SSOUtils {
  public static LdapSettings createDefaultLdapSettings(String accountId) {
    LdapConnectionSettings ldapConnectionSettings = new LdapConnectionSettings();
    LdapGroupSettings groupSettings = new LdapGroupSettings();
    LdapUserSettings userSettings = new LdapUserSettings();
    List<LdapUserSettings> ldapUserSettingsList = new ArrayList<>();
    List<LdapGroupSettings> ldapGroupSettingsList = new ArrayList<>();
    String decryptedBindPassword = new ScmSecret().decryptToString(new SecretName("ldap_bind_password"));
    String ldapHostName = new ScmSecret().decryptToString(new SecretName("ldap_host_name"));

    ldapConnectionSettings.setHost(ldapHostName);
    ldapConnectionSettings.setPort(1389);
    ldapConnectionSettings.setSslEnabled(false);
    ldapConnectionSettings.setBindDN("uid=scarter,ou=People,dc=example,dc=com");
    ldapConnectionSettings.setMaxReferralHops(5);
    ldapConnectionSettings.setBindPassword(decryptedBindPassword);
    ldapConnectionSettings.setConnectTimeout(5000);
    ldapConnectionSettings.setResponseTimeout(5000);
    ldapConnectionSettings.setReferralsEnabled(true);

    userSettings.setBaseDN("dc=example,dc=com");
    userSettings.setDisplayNameAttr("cn");
    userSettings.setEmailAttr("mail");
    userSettings.setGroupMembershipAttr("isMemberOf");
    userSettings.setSearchFilter("(objectClass=person)");
    ldapUserSettingsList.add(userSettings);

    groupSettings.setBaseDN("dc=example,dc=com");
    groupSettings.setDescriptionAttr("description");
    groupSettings.setNameAttr("cn");
    groupSettings.setSearchFilter("(objectClass=groupOfUniqueNames)");
    ldapGroupSettingsList.add(groupSettings);

    return LdapSettings.builder()
        .connectionSettings(ldapConnectionSettings)
        .displayName("LDAP")
        .groupSettingsList(ldapGroupSettingsList)
        .userSettingsList(ldapUserSettingsList)
        .accountId(accountId)
        .build();
  }

  public static String getLdapId(Object SSOConfig) {
    assertThat(SSOConfig).isNotNull();
    LinkedTreeMap<String, Object> maps = (LinkedTreeMap) SSOConfig;
    LinkedTreeMap<String, Object> ssoSettings = (LinkedTreeMap) ((ArrayList) maps.get("ssoSettings")).get(0);
    String ldapId = ssoSettings.get("uuid").toString();
    assertThat(StringUtils.isNotBlank(ldapId)).isTrue();
    return ldapId;
  }

  public static LdapGroupResponse chooseLDAPGroup(Collection<LdapGroupResponse> groupList, String groupName) {
    Iterator<LdapGroupResponse> iterator = groupList.iterator();
    while (iterator.hasNext()) {
      LdapGroupResponse ldapGroupResponse = iterator.next();
      if (ldapGroupResponse.getName().equals(groupName)) {
        return ldapGroupResponse;
      }
    }
    return null;
  }
}
