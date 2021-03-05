package software.wings.infra;

import static software.wings.beans.InfrastructureType.PCF_INFRASTRUCTURE;

import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(PCF_INFRASTRUCTURE)
public final class PcfInfraStructureYaml extends CloudProviderInfrastructureYaml {
  private String cloudProviderName;
  private String organization;
  private String space;
  private List<String> tempRouteMap;
  private List<String> routeMaps;

  @Builder
  public PcfInfraStructureYaml(String type, String cloudProviderName, String organization, String space,
      List<String> tempRouteMap, List<String> routeMaps) {
    super(type);
    setCloudProviderName(cloudProviderName);
    setOrganization(organization);
    setSpace(space);
    setTempRouteMap(tempRouteMap);
    setRouteMaps(routeMaps);
  }

  public PcfInfraStructureYaml() {
    super(PCF_INFRASTRUCTURE);
  }
}
