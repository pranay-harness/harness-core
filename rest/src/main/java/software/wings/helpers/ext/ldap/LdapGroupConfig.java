package software.wings.helpers.ext.ldap;

import software.wings.beans.sso.LdapSearchConfig;

public interface LdapGroupConfig extends LdapSearchConfig {
  String getSearchFilter();

  String getNameAttr();

  String getDescriptionAttr();

  String getUserMembershipAttr();

  String getReferencedUserAttr();

  String getFilter(String identifier);
}
