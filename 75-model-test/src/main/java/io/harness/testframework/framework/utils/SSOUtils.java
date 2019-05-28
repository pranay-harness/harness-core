package io.harness.testframework.framework.utils;

import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import software.wings.beans.sso.LdapConnectionSettings;
import software.wings.beans.sso.LdapGroupSettings;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.LdapUserSettings;

import java.util.ArrayList;
import java.util.List;

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

    LdapSettings ldapSettings = LdapSettings.builder()
                                    .connectionSettings(ldapConnectionSettings)
                                    .displayName("LDAP")
                                    .groupSettingsList(ldapGroupSettingsList)
                                    .userSettingsList(ldapUserSettingsList)
                                    .accountId(accountId)
                                    .build();
    return ldapSettings;
  }
}
