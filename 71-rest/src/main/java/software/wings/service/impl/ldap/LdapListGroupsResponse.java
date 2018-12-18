package software.wings.service.impl.ldap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.ldaptive.SearchResult;
import software.wings.helpers.ext.ldap.LdapGroupConfig;
import software.wings.helpers.ext.ldap.LdapResponse;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapListGroupsResponse {
  SearchResult searchResult;
  LdapResponse ldapResponse;
  LdapGroupConfig ldapGroupConfig;

  public LdapListGroupsResponse(SearchResult searchResult, LdapResponse ldapResponse, LdapGroupConfig ldapGroupConfig) {
    this.searchResult = searchResult;
    this.ldapResponse = ldapResponse;
    this.ldapGroupConfig = ldapGroupConfig;
  }
}
