package software.wings.beans;

import software.wings.security.UsageRestrictionYaml;
import software.wings.yaml.setting.LoadBalancerProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class ElasticLoadBalancerConfigYaml extends LoadBalancerProviderYaml {
  private String region;
  private String loadBalancerName;
  private String accessKey;
  private String secretKey;
  private boolean useEc2IamCredentials;

  @Builder
  public ElasticLoadBalancerConfigYaml(String type, String harnessApiVersion, String region, String loadBalancerName,
      String accessKey, String secretKey, UsageRestrictionYaml usageRestrictions, boolean useEc2IamCredentials) {
    super(type, harnessApiVersion, usageRestrictions);
    this.region = region;
    this.loadBalancerName = loadBalancerName;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.useEc2IamCredentials = useEc2IamCredentials;
  }
}
