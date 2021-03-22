package software.wings.infra;

import static software.wings.beans.InfrastructureType.PHYSICAL_INFRA_WINRM;
import static software.wings.beans.PhysicalInfrastructureMappingWinRm.Builder.aPhysicalInfrastructureMappingWinRm;

import software.wings.api.CloudProviderType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhysicalInfrastructureMappingWinRm;
import software.wings.beans.infrastructure.Host;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Transient;

@JsonTypeName("PHYSICAL_DATA_CENTER_WINRM")
@Data
@Builder
public class PhysicalInfraWinrm implements PhysicalDataCenterInfra, InfraMappingInfrastructureProvider,
                                           FieldKeyValMapProvider, WinRmBasedInfrastructure {
  private String cloudProviderId;
  private List<String> hostNames;
  private List<Host> hosts;
  private String loadBalancerId;
  @Transient private String loadBalancerName;
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
  public String getInfrastructureType() {
    return PHYSICAL_INFRA_WINRM;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.PHYSICAL_DATA_CENTER;
  }

  public String getWinRmConnectionAttributes() {
    return winRmConnectionAttributes;
  }
}
