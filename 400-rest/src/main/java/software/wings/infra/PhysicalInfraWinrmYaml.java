package software.wings.infra;

import static software.wings.beans.InfrastructureType.PHYSICAL_INFRA_WINRM;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.infrastructure.Host;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(PHYSICAL_INFRA_WINRM)
public final class PhysicalInfraWinrmYaml extends CloudProviderInfrastructureYaml {
  private String cloudProviderName;
  private List<String> hostNames;
  private List<Host> hosts;
  private String loadBalancerName;
  private String winRmConnectionAttributesName;

  @Builder
  public PhysicalInfraWinrmYaml(String type, String cloudProviderName, List<String> hostNames, List<Host> hosts,
      String loadBalancerName, String winRmConnectionAttributesName) {
    super(type);
    setCloudProviderName(cloudProviderName);
    setHostNames(hostNames);
    setHosts(hosts);
    setLoadBalancerName(loadBalancerName);
    setWinRmConnectionAttributesName(winRmConnectionAttributesName);
  }

  public PhysicalInfraWinrmYaml() {
    super(PHYSICAL_INFRA_WINRM);
  }
}
