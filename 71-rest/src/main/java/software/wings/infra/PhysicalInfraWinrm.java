package software.wings.infra;

import static software.wings.beans.PhysicalInfrastructureMappingWinRm.Builder.aPhysicalInfrastructureMappingWinRm;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhysicalInfrastructureMappingWinRm;
import software.wings.beans.infrastructure.Host;

import java.util.List;

@JsonTypeName("PHYSICAL_DATA_CENTER_WINRM")
@Data
public class PhysicalInfraWinrm
    implements PhysicalDataCenterInfra, InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  @ExcludeFieldMap private String cloudProviderId;
  private List<String> hostNames;
  private List<Host> hosts;
  private String loadBalancerId;
  private String loadBalancerName;
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

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.PHYSICAL_DATA_CENTER;
  }
}
