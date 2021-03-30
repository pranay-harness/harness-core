package io.harness.ng.authenticationsettings.dtos.mechanisms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.sso.LdapConnectionSettings;
import software.wings.beans.sso.LdapGroupSettings;
import software.wings.beans.sso.LdapUserSettings;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.PL)
public class LDAPSettings extends NGAuthSettings {
  @NotNull @Valid LdapConnectionSettings connectionSettings;

  @Valid List<LdapUserSettings> userSettingsList;

  @Valid List<LdapGroupSettings> groupSettingsList;
}
