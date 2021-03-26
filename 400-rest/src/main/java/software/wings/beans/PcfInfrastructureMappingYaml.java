package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class PcfInfrastructureMappingYaml extends InfrastructureMappingYamlWithComputeProvider {
  private String organization;
  private String space;
  private List<String> tempRouteMap;
  private List<String> routeMaps;

  @lombok.Builder
  public PcfInfrastructureMappingYaml(String type, String harnessApiVersion, String computeProviderType,
      String serviceName, String infraMappingType, String deploymentType, String computeProviderName,
      String organization, String space, List<String> tempRouteMap, List<String> routeMaps,
      Map<String, Object> blueprints) {
    super(type, harnessApiVersion, computeProviderType, serviceName, infraMappingType, deploymentType,
        computeProviderName, blueprints);
    this.organization = organization;
    this.space = space;
    this.tempRouteMap = tempRouteMap;
    this.routeMaps = routeMaps;
  }
}
