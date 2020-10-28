package software.wings.infra;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import software.wings.api.CloudProviderType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

@Data
@FieldNameConstants(innerTypeName = "AzureAppServiceInfraKeys")
public abstract class AzureAppServiceInfra implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider {
  protected String cloudProviderId;
  protected String subscriptionId;
  protected String resourceGroup;
  protected String deploymentSlot;

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AZURE;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public abstract static class AzureAppServiceInfraYaml extends CloudProviderInfrastructureYaml {
    private String cloudProviderName;
    private String subscriptionId;
    private String resourceGroup;
    private String deploymentSlot;

    public AzureAppServiceInfraYaml(
        String type, String cloudProviderName, String subscriptionId, String resourceGroup, String deploymentSlot) {
      super(type);
      this.cloudProviderName = cloudProviderName;
      this.subscriptionId = subscriptionId;
      this.resourceGroup = resourceGroup;
      this.deploymentSlot = deploymentSlot;
    }

    public AzureAppServiceInfraYaml(String type) {
      super(type);
    }
  }
}
