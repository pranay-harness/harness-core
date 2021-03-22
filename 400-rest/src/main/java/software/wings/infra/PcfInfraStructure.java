package software.wings.infra;

import static io.harness.expression.Expression.DISALLOW_SECRETS;

import static software.wings.beans.InfrastructureType.PCF_INFRASTRUCTURE;

import io.harness.expression.Expression;

import software.wings.annotation.IncludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PcfInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@JsonTypeName("PCF_PCF")
@Data
@Builder
public class PcfInfraStructure implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  private String cloudProviderId;
  @Expression(DISALLOW_SECRETS) @IncludeFieldMap private String organization;
  @Expression(DISALLOW_SECRETS) @IncludeFieldMap private String space;
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
}
