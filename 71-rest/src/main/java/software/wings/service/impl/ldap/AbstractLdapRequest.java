package software.wings.service.impl.ldap;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import software.wings.helpers.ext.ldap.LdapUserConfig;

@OwnedBy(PL)
@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class AbstractLdapRequest {
  LdapUserConfig ldapUserConfig;
  int responseTimeoutInSeconds;
}
