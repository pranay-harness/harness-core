package software.wings.infra;

import static software.wings.beans.InfrastructureType.PHYSICAL_INFRA;
import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.infrastructure.Host;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import java.util.List;

@JsonTypeName("PHYSICAL_DATA_CENTER_SSH")
@Data
@Builder
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

  public String getCloudProviderInfrastructureType() {
    return PHYSICAL_INFRA;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.PHYSICAL_DATA_CENTER;
  }
  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(PHYSICAL_INFRA)
  public static final class Yaml extends CloudProviderInfrastructureYaml {
    private String cloudProviderName;
    private List<String> hostNames;
    private List<Host> hosts;
    private String loadBalancerName;
    private String hostConnectionAttrs;

    @Builder
    public Yaml(String type, String cloudProviderName, List<String> hostNames, List<Host> hosts,
        String loadBalancerName, String hostConnectionAttrs) {
      super(type);
      setCloudProviderName(cloudProviderName);
      setHostNames(hostNames);
      setHosts(hosts);
      setLoadBalancerName(loadBalancerName);
      setHostConnectionAttrs(hostConnectionAttrs);
    }

    public Yaml() {
      super(PHYSICAL_INFRA);
    }
  }
}
