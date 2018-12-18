package software.wings.service.impl.ldap;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import software.wings.helpers.ext.ldap.LdapUserConfig;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class AbstractLdapRequest {
  LdapUserConfig ldapUserConfig;
  int responseTimeoutInSeconds;
}
