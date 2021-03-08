package software.wings.beans;

import software.wings.beans.infrastructure.Host;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PhysicalInfrastructureMappingBaseYaml extends YamlWithComputeProvider {
  private List<String> hostNames;
  private List<Host> hosts;
  private String loadBalancer;

  public PhysicalInfrastructureMappingBaseYaml(String type, String harnessApiVersion, String computeProviderType,
      String serviceName, String infraMappingType, String deploymentType, String computeProviderName, String name,
      List<String> hostNames, String loadBalancer, List<Host> hosts, Map<String, Object> blueprints) {
    super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
        computeProviderName, blueprints);
    this.hostNames = hostNames;
    this.loadBalancer = loadBalancer;
    this.hosts = hosts;
  }
}
