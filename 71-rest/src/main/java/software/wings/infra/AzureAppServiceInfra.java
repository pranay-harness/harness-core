package software.wings.infra;

import static io.harness.validation.Validator.ensureType;
import static io.harness.validation.Validator.notNullCheck;

import static java.lang.String.format;

import io.harness.exception.InvalidRequestException;

import software.wings.api.CloudProviderType;
import software.wings.service.impl.yaml.handler.InfraDefinition.CloudProviderInfrastructureYaml;

import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.StringUtils;

@Data
@FieldNameConstants(innerTypeName = "AzureAppServiceInfraKeys")
public abstract class AzureAppServiceInfra
    implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider, ProvisionerAware {
  protected String cloudProviderId;
  protected String subscriptionId;
  protected String resourceGroup;
  protected String deploymentSlot;
  protected Map<String, String> expressions;

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

  @Override
  public void applyExpressions(
      Map<String, Object> resolvedExpressions, String appId, String envId, String infraDefinitionId) {
    setAppServiceGenericExpression(resolvedExpressions);
    setDeploymentSpecificExpression(resolvedExpressions);
  }

  private void setAppServiceGenericExpression(Map<String, Object> resolvedExpressions) {
    for (Map.Entry<String, Object> entry : resolvedExpressions.entrySet()) {
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
          throw new InvalidRequestException(format("Unknown expression : [%s]", entry.getKey()));
      }
    }
    validateFieldDefined(subscriptionId, "Subscription Id");
    validateFieldDefined(resourceGroup, "Resource Group");
    validateFieldDefined(deploymentSlot, "Deployment Slot");
  }

  @Override
  public Set<String> getSupportedExpressions() {
    Set<String> supportedExpression = getDeploymentSpecificExpression();
    supportedExpression.add(AzureAppServiceInfraKeys.subscriptionId);
    supportedExpression.add(AzureAppServiceInfraKeys.resourceGroup);
    supportedExpression.add(AzureAppServiceInfraKeys.deploymentSlot);
    return ImmutableSet.copyOf(supportedExpression);
  }

  protected abstract void setDeploymentSpecificExpression(Map<String, Object> resolvedExpressions);

  protected abstract Set<String> getDeploymentSpecificExpression();

  protected void validateFieldDefined(String field, String fieldName) {
    if (StringUtils.isEmpty(field)) {
      String message = fieldName + " is required";
      throw new InvalidRequestException(message);
    }
  }
}
