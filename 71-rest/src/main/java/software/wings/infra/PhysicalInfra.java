package software.wings.infra;

import static software.wings.beans.InfrastructureType.PHYSICAL_INFRA;
import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.ExcludeFieldMap;
import software.wings.api.CloudProviderType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.infrastructure.Host;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonTypeName("PHYSICAL_DATA_CENTER_SSH")
@Data
@Builder
public class PhysicalInfra implements PhysicalDataCenterInfra, InfraMappingInfrastructureProvider,
                                      FieldKeyValMapProvider, SshBasedInfrastructure, ProvisionerAware {
  public static final String hostArrayPath = "hostArrayPath";
  public static final String hostname = "hostname";

  @ExcludeFieldMap private String cloudProviderId;
  private List<String> hostNames;
  private List<Host> hosts;
  private String loadBalancerId;
  @Transient @ExcludeFieldMap private String loadBalancerName;
  private String hostConnectionAttrs;
  @ExcludeFieldMap private Map<String, String> expressions;

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

  public String getInfrastructureType() {
    return PHYSICAL_INFRA;
  }

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.PHYSICAL_DATA_CENTER;
  }

  @Override
  public Set<String> getSupportedExpressions() {
    // Can contain custom fields
    return null;
  }

  @Override
  public void applyExpressions(Map<String, Object> resolvedExpressions) {}

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(PHYSICAL_INFRA)
  public static final class Yaml extends CloudProviderInfrastructureYaml {
    private String cloudProviderName;
    private List<String> hostNames;
    private List<Host> hosts;
    private String loadBalancerName;
    private String hostConnectionAttrsName;
    private Map<String, String> expressions;

    @Builder
    public Yaml(String type, String cloudProviderName, List<String> hostNames, List<Host> hosts,
        String loadBalancerName, String hostConnectionAttrsName, Map<String, String> expressions) {
      super(type);
      setCloudProviderName(cloudProviderName);
      setHostNames(hostNames);
      setHosts(hosts);
      setLoadBalancerName(loadBalancerName);
      setHostConnectionAttrsName(hostConnectionAttrsName);
      setExpressions(expressions);
    }

    public Yaml() {
      super(PHYSICAL_INFRA);
    }
  }
}
