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
public final class PhysicalInfrastructureMappingYaml extends PhysicalInfrastructureMappingBaseYaml {
  // maps to hostConnectionAttrs
  // This would either be a username/password / ssh key id
  private String connection;

  @lombok.Builder
  public PhysicalInfrastructureMappingYaml(String type, String harnessApiVersion, String computeProviderType,
      String serviceName, String infraMappingType, String deploymentType, String computeProviderName, String name,
      List<String> hostNames, String loadBalancer, String connection, List<Host> hosts,
      Map<String, Object> blueprints) {
    super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
        computeProviderName, name, hostNames, loadBalancer, hosts, blueprints);
    this.connection = connection;
  }
}
