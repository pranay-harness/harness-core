package software.wings.service.impl.ldap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.ldaptive.SearchResult;
import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.helpers.ext.ldap.LdapUserConfig;

import javax.validation.constraints.NotNull;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapGetUsersResponse extends AbstractLdapResponse {
  SearchResult searchResult;

  LdapGetUsersResponse(
      @NotNull LdapUserConfig ldapUserConfig, @NotNull LdapResponse ldapResponse, @NotNull SearchResult searchResult) {
    super(ldapUserConfig, ldapResponse);
    this.searchResult = searchResult;
  }
}
