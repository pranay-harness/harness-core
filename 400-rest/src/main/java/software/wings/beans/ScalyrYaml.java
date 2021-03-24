package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictionsYaml;
import software.wings.yaml.setting.VerificationProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class ScalyrYaml extends VerificationProviderYaml {
  private String scalyrUrl;
  private String apiToken;

  @Builder
  public ScalyrYaml(String type, String harnessApiVersion, String scalyrUrl, String apiToken,
      UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.scalyrUrl = scalyrUrl;
    this.apiToken = apiToken;
  }
}
