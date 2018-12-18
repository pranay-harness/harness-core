package software.wings.service.impl.ldap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import software.wings.helpers.ext.ldap.LdapSearch;
import software.wings.helpers.ext.ldap.LdapUserConfig;

import javax.validation.constraints.NotNull;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapUserExistsRequest extends AbstractLdapRequest {
  LdapSearch ldapSearch;
  String identifier;

  public LdapUserExistsRequest(@NotNull final LdapUserConfig ldapUserConfig, @NotNull final LdapSearch ldapSearch,
      @NotNull String identifier, int responseTimeoutInSeconds) {
    super(ldapUserConfig, responseTimeoutInSeconds);
    this.ldapSearch = ldapSearch;
    this.identifier = identifier;
  }
}
