package software.wings.features.extractors;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.sso.SamlSettings;
import software.wings.features.api.AccountIdExtractor;

@OwnedBy(PL)
public class SamlSettingsAccountIdExtractor implements AccountIdExtractor<SamlSettings> {
  @Override
  public String getAccountId(SamlSettings samlSettings) {
    return samlSettings.getAccountId();
  }
}
