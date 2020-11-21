package software.wings.beans;

import static io.harness.validation.Validator.ensureType;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;

import software.wings.api.DeploymentType;

import com.github.reinert.jjschema.Attributes;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "AzureWebAppsInfrastructureMappingKeys")
public class AzureWebAppInfrastructureMapping extends AzureAppServiceInfrastructureMapping {
  @Trimmed @Attributes(title = "Webb App") protected String webApp;

  @Override
  protected void setDeploymentSpecificVariables(Map<String, Object> map) {
    Object webAppName = map.get("webApp");
    String errorMsg = "Web App name should be of String type";
    notNullCheck(errorMsg, webAppName);
    ensureType(String.class, webAppName, errorMsg);
    setWebApp((String) webAppName);
  }

  @Override
  protected String getAppServiceType() {
    return DeploymentType.AZURE_WEBAPP.name();
  }

  @Builder
  public AzureWebAppInfrastructureMapping(String entityYamlPath, String appId, String accountId, String type,
      String uuid, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt,
      String computeProviderSettingId, String envId, String serviceTemplateId, String serviceId,
      String computeProviderType, String infraMappingType, String deploymentType, String computeProviderName,
      String name, boolean autoPopulateName, Map<String, Object> blueprints, String provisionerId, boolean sample,
      String subscriptionId, String resourceGroup, String deploymentSlot, String webApp) {
    super(entityYamlPath, appId, accountId, type, uuid, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt,
        computeProviderSettingId, envId, serviceTemplateId, serviceId, computeProviderType, infraMappingType,
        deploymentType, computeProviderName, name, autoPopulateName, blueprints, provisionerId, sample, subscriptionId,
        resourceGroup, deploymentSlot);
    this.webApp = webApp;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends AppServiceYaml {
    private String webApp;

    public Yaml(String type, String harnessApiVersion, String serviceName, String infraMappingType,
        String deploymentType, String computeProviderType, String computeProviderName, Map<String, Object> blueprints,
        String subscriptionId, String resourceGroup, String deploymentSlot, String webApp) {
      super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, computeProviderType,
          computeProviderName, blueprints, subscriptionId, resourceGroup, deploymentSlot);
      this.webApp = webApp;
    }
  }
}
