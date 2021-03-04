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
public final class AmazonS3HelmRepoConfigYaml extends HelmRepoYaml {
  private String cloudProvider;
  private String bucket;
  private String region;

  @Builder
  public AmazonS3HelmRepoConfigYaml(String type, String harnessApiVersion, String cloudProvider, String bucket,
      String region, UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.cloudProvider = cloudProvider;
    this.bucket = bucket;
    this.region = region;
  }
}
