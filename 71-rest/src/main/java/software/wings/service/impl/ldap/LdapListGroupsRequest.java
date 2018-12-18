package software.wings.service.impl.ldap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import software.wings.helpers.ext.ldap.LdapGroupConfig;
import software.wings.helpers.ext.ldap.LdapSearch;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapListGroupsRequest {
  LdapSearch ldapSearch;
  String[] returnArguments;
  LdapGroupConfig ldapGroupConfig;
  int responseTimeoutInSeconds;

  public LdapListGroupsRequest(
      LdapSearch ldapSearch, String[] returnArguments, LdapGroupConfig ldapGroupConfig, int responseTimeoutInSeconds) {
    this.ldapSearch = ldapSearch;
    this.returnArguments = returnArguments != null ? returnArguments.clone() : null;
    this.ldapGroupConfig = ldapGroupConfig;
    this.responseTimeoutInSeconds = responseTimeoutInSeconds;
  }
}
