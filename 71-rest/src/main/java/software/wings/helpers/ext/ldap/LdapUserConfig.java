package software.wings.helpers.ext.ldap;

import software.wings.beans.sso.LdapSearchConfig;

public interface LdapUserConfig extends LdapSearchConfig {
  String getSearchFilter();

  String getEmailAttr();

  String getDisplayNameAttr();

  String getGroupMembershipAttr();

  String getUserFilter();

  String getLoadUsersFilter();

  String getGroupMembershipFilter(String groupDn);
}
