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
public final class AwsConfigYaml extends CloudProviderYaml {
  private String accessKey;
  private String accessKeySecretId;
  private String secretKey;
  private boolean useEc2IamCredentials;
  private String tag;
  private boolean assumeCrossAccountRole;
  private AwsCrossAccountAttributes crossAccountAttributes;
  private String defaultRegion;

  @Builder
  public AwsConfigYaml(String type, String harnessApiVersion, String accessKey, String accessKeySecretId,
      String secretKey, UsageRestrictionYaml usageRestrictions, boolean useEc2IamCredentials, String tag,
      boolean assumeCrossAccountRole, AwsCrossAccountAttributes crossAccountAttributes, String defaultRegion) {
    super(type, harnessApiVersion, usageRestrictions);
    this.accessKey = accessKey;
    this.accessKeySecretId = accessKeySecretId;
    this.secretKey = secretKey;
    this.useEc2IamCredentials = useEc2IamCredentials;
    this.tag = tag;
    this.assumeCrossAccountRole = assumeCrossAccountRole;
    this.crossAccountAttributes = crossAccountAttributes;
    this.defaultRegion = defaultRegion;
  }
}
