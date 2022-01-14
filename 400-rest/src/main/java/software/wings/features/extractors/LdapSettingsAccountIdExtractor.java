package software.wings.features.extractors;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.sso.LdapSettings;
import software.wings.features.api.AccountIdExtractor;

@OwnedBy(PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class LdapSettingsAccountIdExtractor implements AccountIdExtractor<LdapSettings> {
  @Override
  public String getAccountId(LdapSettings ldapSettings) {
    return ldapSettings.getAccountId();
  }
}
