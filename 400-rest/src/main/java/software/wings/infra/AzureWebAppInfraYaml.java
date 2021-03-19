package software.wings.infra;

import static software.wings.beans.InfrastructureType.AZURE_WEBAPP;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(AZURE_WEBAPP)
public final class AzureWebAppInfraYaml extends CloudProviderInfrastructureYaml {
  private String cloudProviderName;
  private String subscriptionId;
  private String resourceGroup;

  @Builder
  public AzureWebAppInfraYaml(String type, String cloudProviderName, String subscriptionId, String resourceGroup) {
    super(type);
    this.cloudProviderName = cloudProviderName;
    this.subscriptionId = subscriptionId;
    this.resourceGroup = resourceGroup;
  }

  public AzureWebAppInfraYaml() {
    super(AZURE_WEBAPP);
  }
}
