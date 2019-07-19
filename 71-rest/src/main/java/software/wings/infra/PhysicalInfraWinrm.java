package software.wings.infra;

import static software.wings.beans.PhysicalInfrastructureMappingWinRm.Builder.aPhysicalInfrastructureMappingWinRm;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import software.wings.annotation.IncludeInFieldMap;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhysicalInfrastructureMappingBase.PhysicalInfrastructureMappingBaseKeys;
import software.wings.beans.PhysicalInfrastructureMappingWinRm;
import software.wings.beans.PhysicalInfrastructureMappingWinRm.PhysicalInfrastructureMappingWinRmKeys;
import software.wings.beans.infrastructure.Host;

import java.util.List;

@JsonTypeName("PHYSICAL_DATA_CENTER_WINRM")
@Data
public class PhysicalInfraWinrm implements PhysicalDataCenterInfra, InfraMappingInfrastructureProvider {
  private String cloudProviderId;
  @IncludeInFieldMap(key = PhysicalInfrastructureMappingBaseKeys.hostNames) private List<String> hostNames;
  @IncludeInFieldMap(key = PhysicalInfrastructureMappingBaseKeys.hosts) private List<Host> hosts;
  @IncludeInFieldMap(key = PhysicalInfrastructureMappingBaseKeys.loadBalancerId) private String loadBalancerId;
  @IncludeInFieldMap(key = PhysicalInfrastructureMappingBaseKeys.loadBalancerName) private String loadBalancerName;
  @IncludeInFieldMap(key = PhysicalInfrastructureMappingWinRmKeys.winRmConnectionAttributes)
  private String winRmConnectionAttributes;

  @Override
  public InfrastructureMapping getInfraMapping() {
    return aPhysicalInfrastructureMappingWinRm()
        .withComputeProviderSettingId(cloudProviderId)
        .withHostNames(hostNames)
        .withLoadBalancerId(loadBalancerId)
        .withLoadBalancerName(loadBalancerName)
        .withWinRmConnectionAttributes(winRmConnectionAttributes)
        .withInfraMappingType(InfrastructureMappingType.PHYSICAL_DATA_CENTER_WINRM.name())
        .build();
  }

  @Override
  public Class<PhysicalInfrastructureMappingWinRm> getMappingClass() {
    return PhysicalInfrastructureMappingWinRm.class;
  }
}
