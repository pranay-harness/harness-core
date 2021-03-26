package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.infrastructure.Host;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class PhysicalInfrastructureMappingWinRmYaml extends PhysicalInfrastructureMappingBaseYaml {
  private String winRmProfile;

  @lombok.Builder
  public PhysicalInfrastructureMappingWinRmYaml(String type, String harnessApiVersion, String computeProviderType,
      String serviceName, String infraMappingType, String deploymentType, String computeProviderName, String name,
      List<String> hostNames, String loadBalancer, String winRmProfile, List<Host> hosts,
      Map<String, Object> blueprints) {
    super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
        computeProviderName, name, hostNames, loadBalancer, hosts, blueprints);
    this.winRmProfile = winRmProfile;
  }
}
