package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictionsYaml;
import software.wings.yaml.setting.VerificationProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class SumoConfigYaml extends VerificationProviderYaml {
  private String sumoUrl;
  private String accessId;
  private String accessKey;

  @Builder
  public SumoConfigYaml(String type, String harnessApiVersion, String sumoUrl, String accessId, String accessKey,
      UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.sumoUrl = sumoUrl;
    this.accessId = accessId;
    this.accessKey = accessKey;
  }
}
