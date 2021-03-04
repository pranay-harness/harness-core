package software.wings.beans.settings.azureartifacts;

import software.wings.security.UsageRestrictionYaml;
import software.wings.yaml.setting.AzureArtifactsYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AzureArtifactsPATConfigYaml extends AzureArtifactsYaml {
  private String azureDevopsUrl;
  private String pat;

  @Builder
  public AzureArtifactsPATConfigYaml(String type, String harnessApiVersion, String azureDevopsUrl, String pat,
      UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.azureDevopsUrl = azureDevopsUrl;
    this.pat = pat;
  }
}
