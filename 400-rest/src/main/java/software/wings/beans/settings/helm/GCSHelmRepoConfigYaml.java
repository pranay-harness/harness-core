package software.wings.beans.settings.helm;

import software.wings.security.UsageRestrictionYaml;
import software.wings.yaml.setting.HelmRepoYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class GCSHelmRepoConfigYaml extends HelmRepoYaml {
  private String cloudProvider;
  private String bucket;

  @Builder
  public GCSHelmRepoConfigYaml(String type, String harnessApiVersion, String cloudProvider, String bucket,
      UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.cloudProvider = cloudProvider;
    this.bucket = bucket;
  }
}
