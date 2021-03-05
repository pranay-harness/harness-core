package software.wings.beans;

import software.wings.security.UsageRestrictionYaml;
import software.wings.yaml.setting.CloudProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class SpotInstConfigYaml extends CloudProviderYaml {
  private String spotInstToken;
  private String spotInstAccountId;

  @Builder
  public SpotInstConfigYaml(String type, String harnessApiVersion, UsageRestrictionYaml usageRestrictions,
      String spotInstToken, String spotInstAccountId) {
    super(type, harnessApiVersion, usageRestrictions);
    this.spotInstToken = spotInstToken;
    this.spotInstAccountId = spotInstAccountId;
  }
}
