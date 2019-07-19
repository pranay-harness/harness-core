package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import software.wings.annotation.IncludeInFieldMap;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.PcfInfrastructureMapping.PcfInfrastructureMappingKeys;

import java.util.List;

@JsonTypeName("PCF_PCF")
@Data
public class PcfInfraStructure implements InfraMappingInfrastructureProvider {
  private String cloudProviderId;
  @IncludeInFieldMap(key = PcfInfrastructureMappingKeys.organization) private String organization;
  @IncludeInFieldMap(key = PcfInfrastructureMappingKeys.space) private String space;
  @IncludeInFieldMap(key = PcfInfrastructureMappingKeys.tempRouteMap) private List<String> tempRouteMap;
  @IncludeInFieldMap(key = PcfInfrastructureMappingKeys.routeMaps) private List<String> routeMaps;

  @Override
  public InfrastructureMapping getInfraMapping() {
    return PcfInfrastructureMapping.builder()
        .computeProviderSettingId(cloudProviderId)
        .organization(organization)
        .space(space)
        .tempRouteMap(tempRouteMap)
        .routeMaps(routeMaps)
        .infraMappingType(InfrastructureMappingType.PCF_PCF.name())
        .build();
  }

  @Override
  public Class<PcfInfrastructureMapping> getMappingClass() {
    return PcfInfrastructureMapping.class;
  }
}
