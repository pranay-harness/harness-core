package software.wings.beans.settings.helm;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictionsYaml;
import software.wings.yaml.setting.HelmRepoYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class GCSHelmRepoConfigYaml extends HelmRepoYaml {
  private String cloudProvider;
  private String bucket;

  @Builder
  public GCSHelmRepoConfigYaml(String type, String harnessApiVersion, String cloudProvider, String bucket,
      UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.cloudProvider = cloudProvider;
    this.bucket = bucket;
  }
}
