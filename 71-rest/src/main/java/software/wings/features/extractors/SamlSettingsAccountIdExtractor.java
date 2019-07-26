package software.wings.features.extractors;

import software.wings.beans.sso.SamlSettings;
import software.wings.features.api.AccountIdExtractor;

public class SamlSettingsAccountIdExtractor implements AccountIdExtractor<SamlSettings> {
  @Override
  public String getAccountId(SamlSettings samlSettings) {
    return samlSettings.getAccountId();
  }
}
