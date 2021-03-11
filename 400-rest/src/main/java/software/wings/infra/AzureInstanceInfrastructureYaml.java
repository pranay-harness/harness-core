package software.wings.infra;

import static software.wings.beans.InfrastructureType.AZURE_SSH;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.AzureTag;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(AZURE_SSH)
public final class AzureInstanceInfrastructureYaml extends CloudProviderInfrastructureYaml {
  private String cloudProviderName;
  private String resourceGroup;
  private String subscriptionId;
  private List<AzureTag> tags;
  private String hostConnectionAttrsName;
  private String winRmConnectionAttributesName;

  @Builder
  public AzureInstanceInfrastructureYaml(String type, String cloudProviderName, String resourceGroup,
      String subscriptionId, List<AzureTag> tags, String hostConnectionAttrsName,
      String winRmConnectionAttributesName) {
    super(type);
    setCloudProviderName(cloudProviderName);
    setResourceGroup(resourceGroup);
    setSubscriptionId(subscriptionId);
    setTags(tags);
    setHostConnectionAttrsName(hostConnectionAttrsName);
    setWinRmConnectionAttributesName(winRmConnectionAttributesName);
  }

  public AzureInstanceInfrastructureYaml() {
    super(AZURE_SSH);
  }
}
