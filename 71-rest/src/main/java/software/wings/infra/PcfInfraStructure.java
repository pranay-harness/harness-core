package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PcfInfrastructureMapping;

import java.util.List;

@JsonTypeName("PCF_PCF")
@Data
public class PcfInfraStructure implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  @ExcludeFieldMap private String cloudProviderId;
  private String organization;
  private String space;
  private List<String> tempRouteMap;
  private List<String> routeMaps;

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

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.PCF;
  }
}
