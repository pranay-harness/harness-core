package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * The type Yaml.
 */
@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@JsonPropertyOrder({"type", "harnessApiVersion", "connectionType"})
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AwsInfrastructureMappingYaml extends InfrastructureMapping.YamlWithComputeProvider {
  // maps to restrictionType
  private String restrictions;
  // maps to restrictionExpression
  private String expression;
  private String region;
  // maps to hostConnectionAttrs
  private String provisionerName;
  private String connectionType;
  private String loadBalancer;
  private boolean usePublicDns;
  private String hostConnectionType;
  private boolean provisionInstances;
  private String autoScalingGroup;
  private int desiredCapacity;
  private String hostNameConvention;

  // These four fields map to AwsInstanceFilter
  private List<String> vpcs = new ArrayList<>();
  private List<NameValuePair.Yaml> awsTags = new ArrayList<>();

  @lombok.Builder
  public AwsInfrastructureMappingYaml(String type, String harnessApiVersion, String computeProviderType,
      String serviceName, String infraMappingType, String deploymentType, String computeProviderName, String name,
      String restrictions, String expression, String region, String provisionerName, String connectionType,
      String loadBalancer, boolean usePublicDns, String hostConnectionType, boolean provisionInstances,
      String autoScalingGroup, int desiredCapacity, List<String> vpcs, List<NameValuePair.Yaml> awsTags,
      String hostNameConvention, Map<String, Object> blueprints) {
    super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
        computeProviderName, blueprints);
    this.restrictions = restrictions;
    this.expression = expression;
    this.region = region;
    this.provisionerName = provisionerName;
    this.connectionType = connectionType;
    this.loadBalancer = loadBalancer;
    this.usePublicDns = usePublicDns;
    this.hostConnectionType = hostConnectionType;
    this.provisionInstances = provisionInstances;
    this.autoScalingGroup = autoScalingGroup;
    this.desiredCapacity = desiredCapacity;
    this.vpcs = vpcs;
    this.awsTags = awsTags;
    this.hostNameConvention = hostNameConvention;
  }
}
