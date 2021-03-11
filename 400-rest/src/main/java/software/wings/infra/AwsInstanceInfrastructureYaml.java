package software.wings.infra;

import static software.wings.beans.InfrastructureType.AWS_INSTANCE;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.AwsInstanceFilter;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(AWS_INSTANCE)
public final class AwsInstanceInfrastructureYaml extends CloudProviderInfrastructureYaml {
  private String cloudProviderName;
  private String region;
  private String hostConnectionAttrsName;
  private String loadBalancerName;
  private boolean usePublicDns;
  private String hostConnectionType;
  private boolean useAutoScalingGroup;
  private AwsInstanceFilter awsInstanceFilter;
  private String autoScalingGroupName;
  private boolean setDesiredCapacity;
  private int desiredCapacity;
  private String hostNameConvention;
  private Map<String, String> expressions;

  @Builder
  public AwsInstanceInfrastructureYaml(String type, String cloudProviderName, String region,
      String hostConnectionAttrsName, String loadBalancerName, boolean usePublicDns, String hostConnectionType,
      boolean useAutoScalingGroup, AwsInstanceFilter awsInstanceFilter, String autoScalingGroupName,
      boolean setDesiredCapacity, int desiredCapacity, String hostNameConvention, Map<String, String> expressions) {
    super(type);
    setCloudProviderName(cloudProviderName);
    setRegion(region);
    setHostConnectionAttrsName(hostConnectionAttrsName);
    setLoadBalancerName(loadBalancerName);
    setUsePublicDns(usePublicDns);
    setHostConnectionType(hostConnectionType);
    setUseAutoScalingGroup(useAutoScalingGroup);
    setAutoScalingGroupName(autoScalingGroupName);
    setAwsInstanceFilter(awsInstanceFilter);
    setSetDesiredCapacity(setDesiredCapacity);
    setDesiredCapacity(desiredCapacity);
    setHostNameConvention(hostNameConvention);
    setExpressions(expressions);
  }

  public AwsInstanceInfrastructureYaml() {
    super(AWS_INSTANCE);
  }
}
