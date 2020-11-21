package software.wings.beans;

import static io.harness.validation.Validator.ensureType;
import static io.harness.validation.Validator.notNullCheck;

import static java.lang.String.format;

import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;
import io.harness.exception.InvalidRequestException;

import software.wings.utils.Utils;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "AzureAppServiceInfrastructureMappingKeys")
public abstract class AzureAppServiceInfrastructureMapping extends InfrastructureMapping {
  @Trimmed @Attributes(title = "SubscriptionId") @Getter @Setter private String subscriptionId;
  @Trimmed @Attributes(title = "Resource Group") @Getter @Setter private String resourceGroup;
  @Trimmed @Attributes(title = "Deployment Slot") @Getter @Setter private String deploymentSlot;

  public AzureAppServiceInfrastructureMapping(String entityYamlPath, String appId, String accountId, String type,
      String uuid, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt,
      String computeProviderSettingId, String envId, String serviceTemplateId, String serviceId,
      String computeProviderType, String infraMappingType, String deploymentType, String computeProviderName,
      String name, boolean autoPopulateName, Map<String, Object> blueprints, String provisionerId, boolean sample,
      String subscriptionId, String resourceGroup, String deploymentSlot) {
    super(entityYamlPath, appId, accountId, type, uuid, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt,
        computeProviderSettingId, envId, serviceTemplateId, serviceId, computeProviderType, infraMappingType,
        deploymentType, computeProviderName, name, autoPopulateName, blueprints, provisionerId, sample);
    this.subscriptionId = subscriptionId;
    this.resourceGroup = resourceGroup;
    this.deploymentSlot = deploymentSlot;
  }

  @Override
  public void applyProvisionerVariables(Map<String, Object> map,
      InfrastructureMappingBlueprint.NodeFilteringType nodeFilteringType, boolean featureFlagEnabled) {
    setAppServiceGenericVariables(map);
    setDeploymentSpecificVariables(map);
  }

  private void setAppServiceGenericVariables(Map<String, Object> map) {
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      switch (entry.getKey()) {
        case "subscriptionId":
          String errorMsg = "Subscription Id should be of String type";
          notNullCheck(errorMsg, entry.getValue());
          ensureType(String.class, entry.getValue(), errorMsg);
          setSubscriptionId((String) entry.getValue());
          break;

        case "resourceGroup":
          errorMsg = "Resource Group should be of String type";
          notNullCheck(errorMsg, entry.getValue());
          ensureType(String.class, entry.getValue(), errorMsg);
          setResourceGroup((String) entry.getValue());
          break;

        case "deploymentSlot":
          errorMsg = "Deployment Slot should be of String type";
          notNullCheck(errorMsg, entry.getValue());
          ensureType(String.class, entry.getValue(), errorMsg);
          setDeploymentSlot((String) entry.getValue());
          break;

        default:
          break;
      }
    }
    validateFieldDefined(subscriptionId, "Subscription Id");
    validateFieldDefined(resourceGroup, "Resource Group");
    validateFieldDefined(deploymentSlot, "Deployment Slot");
  }

  protected void validateFieldDefined(String field, String fieldName) {
    if (StringUtils.isEmpty(field)) {
      String message = fieldName + " is required";
      throw new InvalidRequestException(message);
    }
  }

  protected abstract void setDeploymentSpecificVariables(Map<String, Object> map);

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Utils.normalize(format("(%s)_%s", getAppServiceType(),
        Optional.ofNullable(getComputeProviderName()).orElse(getComputeProviderType().toLowerCase())));
  }

  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return null;
  }

  protected abstract String getAppServiceType();

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public abstract static class AppServiceYaml extends InfrastructureMapping.YamlWithComputeProvider {
    private String subscriptionId;
    private String resourceGroup;
    private String deploymentSlot;

    public AppServiceYaml(String type, String harnessApiVersion, String serviceName, String infraMappingType,
        String deploymentType, String computeProviderType, String computeProviderName, Map<String, Object> blueprints,
        String subscriptionId, String resourceGroup, String deploymentSlot) {
      super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, computeProviderType,
          computeProviderName, blueprints);
      this.subscriptionId = subscriptionId;
      this.resourceGroup = resourceGroup;
      this.deploymentSlot = deploymentSlot;
    }
  }
}
