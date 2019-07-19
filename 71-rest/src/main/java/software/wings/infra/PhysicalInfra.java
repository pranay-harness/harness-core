package software.wings.infra;

import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import software.wings.annotation.IncludeInFieldMap;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMapping.PhysicalInfrastructureMappingKeys;
import software.wings.beans.PhysicalInfrastructureMappingBase.PhysicalInfrastructureMappingBaseKeys;
import software.wings.beans.infrastructure.Host;

import java.util.List;

@JsonTypeName("PHYSICAL_DATA_CENTER_SSH")
@Data
public class PhysicalInfra implements PhysicalDataCenterInfra, InfraMappingInfrastructureProvider {
  private String cloudProviderId;
  @IncludeInFieldMap(key = PhysicalInfrastructureMappingBaseKeys.hostNames) private List<String> hostNames;
  @IncludeInFieldMap(key = PhysicalInfrastructureMappingBaseKeys.hosts) private List<Host> hosts;
  @IncludeInFieldMap(key = PhysicalInfrastructureMappingBaseKeys.loadBalancerId) private String loadBalancerId;
  @IncludeInFieldMap(key = PhysicalInfrastructureMappingBaseKeys.loadBalancerName) private String loadBalancerName;
  @IncludeInFieldMap(key = PhysicalInfrastructureMappingKeys.hostConnectionAttrs) private String hostConnectionAttrs;

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
}
