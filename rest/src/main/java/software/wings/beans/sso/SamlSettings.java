package software.wings.beans.sso;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class SamlSettings extends SSOSettings {
  @NotNull private String metaDataFile;
  @NotNull private String accountId;

  @Builder
  public SamlSettings(SSOType ssoType, String displayName, String url, String metaDataFile, String accountId) {
    super(SSOType.SAML, displayName, url);
    this.metaDataFile = metaDataFile;
    this.accountId = accountId;
  }

  @Override
  public SSOSettings getPublicSSOSettings() {
    return SamlSettings.builder().displayName(displayName).ssoType(type).url(url).accountId(accountId).build();
  }
}
