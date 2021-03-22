package software.wings.infra;

import static software.wings.beans.InfrastructureType.PHYSICAL_INFRA;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.infrastructure.Host;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(PHYSICAL_INFRA)
public final class PhysicalInfraYaml extends CloudProviderInfrastructureYaml {
  private String cloudProviderName;
  private List<String> hostNames;
  private List<Host> hosts;
  private String loadBalancerName;
  private String hostConnectionAttrsName;
  private Map<String, String> expressions;

  @Builder
  public PhysicalInfraYaml(String type, String cloudProviderName, List<String> hostNames, List<Host> hosts,
      String loadBalancerName, String hostConnectionAttrsName, Map<String, String> expressions) {
    super(type);
    setCloudProviderName(cloudProviderName);
    setHostNames(hostNames);
    setHosts(hosts);
    setLoadBalancerName(loadBalancerName);
    setHostConnectionAttrsName(hostConnectionAttrsName);
    setExpressions(expressions);
  }

  public PhysicalInfraYaml() {
    super(PHYSICAL_INFRA);
  }
}
