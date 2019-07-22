package software.wings.infra;

import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.infrastructure.Host;

import java.util.List;

@JsonTypeName("PHYSICAL_DATA_CENTER_SSH")
@Data
public class PhysicalInfra
    implements PhysicalDataCenterInfra, InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  @ExcludeFieldMap private String cloudProviderId;
  private List<String> hostNames;
  private List<Host> hosts;
  private String loadBalancerId;
  private String loadBalancerName;
  private String hostConnectionAttrs;

  @Override
  public InfrastructureMapping getInfraMapping() {
    return aPhysicalInfrastructureMapping()
        .withComputeProviderSettingId(cloudProviderId)
        .withHostNames(hostNames)
        .withLoadBalancerId(loadBalancerId)
        .withLoadBalancerName(loadBalancerName)
        .withHostConnectionAttrs(hostConnectionAttrs)
        .withInfraMappingType(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name())
        .build();
  }

  @Override
  public Class<PhysicalInfrastructureMapping> getMappingClass() {
    return PhysicalInfrastructureMapping.class;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.PHYSICAL_DATA_CENTER;
  }
}
