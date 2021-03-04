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
public final class SumoConfigYaml extends VerificationProviderYaml {
  private String sumoUrl;
  private String accessId;
  private String accessKey;

  @Builder
  public SumoConfigYaml(String type, String harnessApiVersion, String sumoUrl, String accessId, String accessKey,
      UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.sumoUrl = sumoUrl;
    this.accessId = accessId;
    this.accessKey = accessKey;
  }
}
