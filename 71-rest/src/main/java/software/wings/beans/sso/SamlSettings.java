package software.wings.beans.sso;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = false)
public class SamlSettings extends SSOSettings {
  @NotNull private String metaDataFile;
  @NotNull private String accountId;
  @NotNull private String origin;

  @Builder
  public SamlSettings(
      SSOType ssoType, String displayName, String url, String metaDataFile, String accountId, String origin) {
    super(SSOType.SAML, displayName, url);
    this.metaDataFile = metaDataFile;
    this.accountId = accountId;
    this.origin = origin;
  }

  @Override
  public SSOSettings getPublicSSOSettings() {
    return SamlSettings.builder().displayName(displayName).ssoType(type).url(url).accountId(accountId).build();
  }
}
