package software.wings.beans;

import software.wings.security.UsageRestrictionsYaml;
import software.wings.yaml.setting.VerificationProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class NewRelicConfigYaml extends VerificationProviderYaml {
  private String apiKey;
  private String newRelicAccountId;

  @Builder
  public NewRelicConfigYaml(String type, String harnessApiVersion, String apiKey, String newRelicAccountId,
      UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.apiKey = apiKey;
    this.newRelicAccountId = newRelicAccountId;
  }
}
