package software.wings.service.impl.ldap;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.helpers.ext.ldap.LdapUserConfig;

import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapUserExistsResponse extends AbstractLdapResponse {
  String Dn;

  public LdapUserExistsResponse(@NotNull LdapUserConfig ldapUserConfig, @NotNull LdapResponse ldapResponse, String Dn) {
    super(ldapUserConfig, ldapResponse);
    this.Dn = Dn;
  }
}
