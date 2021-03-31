package io.harness.ng.authenticationsettings.dtos.mechanisms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.sso.LdapConnectionSettings;
import software.wings.beans.sso.LdapGroupSettings;
import software.wings.beans.sso.LdapUserSettings;
import software.wings.security.authentication.AuthenticationMechanism;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("LDAP")
@OwnedBy(HarnessTeam.PL)
public class LDAPSettings extends NGAuthSettings {
  @NotNull @Valid LdapConnectionSettings connectionSettings;

  @Valid List<LdapUserSettings> userSettingsList;

  @Valid List<LdapGroupSettings> groupSettingsList;

  @Override
  public AuthenticationMechanism getSettingsType() {
    return AuthenticationMechanism.LDAP;
  }
}
