package software.wings.infra;

import static io.harness.validation.Validator.ensureType;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.InfrastructureType.AZURE_WEBAPP;

import static java.lang.String.format;

import io.harness.exception.InvalidRequestException;

import software.wings.api.CloudProviderType;
import software.wings.beans.AzureWebAppInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName(AZURE_WEBAPP)
@Data
@Builder
@FieldNameConstants(innerTypeName = "AzureWebAppInfraKeys")
public class AzureWebAppInfra implements InfraMappingInfrastructureProvider, FieldKeyValMapProvider, ProvisionerAware {
  private String cloudProviderId;
  private String subscriptionId;
  private String resourceGroup;
  private Map<String, String> expressions;

  @Override
  public CloudProviderType getCloudProviderType() {
    return CloudProviderType.AZURE;
  }

  @Override
  public String getInfrastructureType() {
    return AZURE_WEBAPP;
  }

  @Override
  public InfrastructureMapping getInfraMapping() {
    AzureWebAppInfrastructureMapping infrastructureMapping =
        AzureWebAppInfrastructureMapping.builder().subscriptionId(subscriptionId).resourceGroup(resourceGroup).build();
    infrastructureMapping.setComputeProviderSettingId(cloudProviderId);
    return infrastructureMapping;
  }

  @Override
  public Class<? extends InfrastructureMapping> getMappingClass() {
    return AzureWebAppInfrastructureMapping.class;
  }

  @Override
  public void applyExpressions(
      Map<String, Object> resolvedExpressions, String appId, String envId, String infraDefinitionId) {
    setAppServiceExpression(resolvedExpressions);
  }

  @Override
  public Set<String> getSupportedExpressions() {
    Set<String> supportedExpression = new HashSet<>();
    supportedExpression.add(AzureWebAppInfraKeys.subscriptionId);
    supportedExpression.add(AzureWebAppInfraKeys.resourceGroup);
    return ImmutableSet.copyOf(supportedExpression);
  }

  private void setAppServiceExpression(Map<String, Object> resolvedExpressions) {
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

        default:
          throw new InvalidRequestException(format("Unknown expression : [%s]", entry.getKey()));
      }
    }
    validateFieldDefined(subscriptionId, "Subscription Id");
    validateFieldDefined(resourceGroup, "Resource Group");
  }

  private void validateFieldDefined(String field, String fieldName) {
    if (StringUtils.isEmpty(field)) {
      String message = fieldName + " is required";
      throw new InvalidRequestException(message);
    }
  }
}
