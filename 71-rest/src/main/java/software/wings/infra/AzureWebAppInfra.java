package software.wings.infra;

import static software.wings.beans.InfrastructureType.AZURE_WEBAPP;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import software.wings.beans.AzureWebAppInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;

@JsonTypeName(AZURE_WEBAPP)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "AzureWebAppInfraKeys")
public class AzureWebAppInfra extends AzureAppServiceInfra {
  private String webApp;

  @Override
  public InfrastructureMapping getInfraMapping() {
    AzureWebAppInfrastructureMapping infrastructureMapping = AzureWebAppInfrastructureMapping.builder()
                                                                 .subscriptionId(subscriptionId)
                                                                 .resourceGroup(resourceGroup)
                                                                 .deploymentSlot(deploymentSlot)
                                                                 .webApp(webApp)
                                                                 .build();
    infrastructureMapping.setComputeProviderSettingId(cloudProviderId);
    return infrastructureMapping;
  }

  @Override
  public Class<? extends InfrastructureMapping> getMappingClass() {
    return AzureWebAppInfrastructureMapping.class;
  }

  @Override
  public String getInfrastructureType() {
    return AZURE_WEBAPP;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(AZURE_WEBAPP)
  public static final class Yaml extends AzureAppServiceInfraYaml {
    private String webApp;

    @Builder
    public Yaml(String type, String cloudProviderName, String subscriptionId, String resourceGroup,
        String deploymentSlot, String webApp) {
      super(type, cloudProviderName, subscriptionId, resourceGroup, deploymentSlot);
      this.webApp = webApp;
    }

    public Yaml() {
      super(AZURE_WEBAPP);
    }
  }
}
