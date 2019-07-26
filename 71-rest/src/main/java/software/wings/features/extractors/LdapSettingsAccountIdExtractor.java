package software.wings.features.extractors;

import software.wings.beans.sso.LdapSettings;
import software.wings.features.api.AccountIdExtractor;

public class LdapSettingsAccountIdExtractor implements AccountIdExtractor<LdapSettings> {
  @Override
  public String getAccountId(LdapSettings ldapSettings) {
    return ldapSettings.getAccountId();
  }
}
