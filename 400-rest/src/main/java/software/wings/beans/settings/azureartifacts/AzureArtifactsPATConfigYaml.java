package software.wings.beans.settings.azureartifacts;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictions;
import software.wings.yaml.setting.AzureArtifactsYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AzureArtifactsPATConfigYaml extends AzureArtifactsYaml {
  private String azureDevopsUrl;
  private String pat;

  @Builder
  public AzureArtifactsPATConfigYaml(String type, String harnessApiVersion, String azureDevopsUrl, String pat,
      UsageRestrictions.Yaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.azureDevopsUrl = azureDevopsUrl;
    this.pat = pat;
  }
}
