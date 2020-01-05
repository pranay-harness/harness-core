package software.wings.infra;

import static software.wings.beans.InfrastructureType.PCF_INFRASTRUCTURE;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.expression.Expression;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.annotation.IncludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import java.util.List;

@JsonTypeName("PCF_PCF")
@Data
@Builder
public class PcfInfraStructure implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  private String cloudProviderId;
  @Expression @IncludeFieldMap private String organization;
  @Expression @IncludeFieldMap private String space;
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

  @Override
  public String getInfrastructureType() {
    return PCF_INFRASTRUCTURE;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(PCF_INFRASTRUCTURE)
  public static final class Yaml extends CloudProviderInfrastructureYaml {
    private String cloudProviderName;
    private String organization;
    private String space;
    private List<String> tempRouteMap;
    private List<String> routeMaps;

    @Builder
    public Yaml(String type, String cloudProviderName, String organization, String space, List<String> tempRouteMap,
        List<String> routeMaps) {
      super(type);
      setCloudProviderName(cloudProviderName);
      setOrganization(organization);
      setSpace(space);
      setTempRouteMap(tempRouteMap);
      setRouteMaps(routeMaps);
    }

    public Yaml() {
      super(PCF_INFRASTRUCTURE);
    }
  }
}
