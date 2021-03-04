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
public final class ScalyrYaml extends VerificationProviderYaml {
  private String scalyrUrl;
  private String apiToken;

  @Builder
  public ScalyrYaml(String type, String harnessApiVersion, String scalyrUrl, String apiToken,
      UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.scalyrUrl = scalyrUrl;
    this.apiToken = apiToken;
  }
}
