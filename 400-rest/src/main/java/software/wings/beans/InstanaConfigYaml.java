package software.wings.beans;

import software.wings.security.UsageRestrictionYaml;
import software.wings.yaml.setting.VerificationProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class InstanaConfigYaml extends VerificationProviderYaml {
  private String instanaUrl;
  private String apiToken;
  @Builder
  public InstanaConfigYaml(String type, String harnessApiVersion, String instanaUrl, String apiToken,
      UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.instanaUrl = instanaUrl;
    this.apiToken = apiToken;
  }
}
