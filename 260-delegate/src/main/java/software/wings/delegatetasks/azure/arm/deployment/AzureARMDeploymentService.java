package software.wings.delegatetasks.azure.arm.deployment;

import static java.lang.String.format;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.client.AzureManagementClient;
import io.harness.azure.context.AzureClientContext;
import io.harness.azure.model.AzureARMTemplate;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.AzureConstants;
import io.harness.exception.InvalidRequestException;

import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentManagementGroupContext;
import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentResourceGroupContext;
import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentSubscriptionContext;
import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentTenantContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.ErrorResponse;
import com.microsoft.azure.management.resources.implementation.DeploymentExtendedInner;
import com.microsoft.azure.management.resources.implementation.DeploymentValidateResultInner;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@NoArgsConstructor
@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public class AzureARMDeploymentService {
  @Inject private AzureManagementClient azureManagementClient;

  public String deployAtResourceGroupScope(DeploymentResourceGroupContext context) {
    AzureClientContext azureClientContext = context.getAzureClientContext();
    AzureARMTemplate azureARMTemplate = AzureARMTemplate.builder()
                                            .deploymentMode(context.getMode())
                                            .deploymentName(context.getDeploymentName())
                                            .parametersJSON(context.getParametersJson())
                                            .templateJSON(context.getTemplateJson())
                                            .build();

    DeploymentValidateResultInner deploymentValidateResultInner =
        azureManagementClient.validateDeploymentAtResourceGroupScope(azureClientContext, azureARMTemplate);
    ErrorResponse errorResponse = deploymentValidateResultInner.error();
    if (errorResponse != null) {
      throw new InvalidRequestException(
          format("Unable to deploy at resource group scope, deployment validation failed: %s",
              getValidationErrorMsg(errorResponse)));
    }

    Deployment deployment = azureManagementClient.deployAtResourceGroupScope(azureClientContext, azureARMTemplate);
    // polling for status, when deployment is done return outputs
    return deployment.outputs().toString();
  }

  public String deployAtSubscriptionScope(DeploymentSubscriptionContext context) {
    AzureConfig azureConfig = context.getAzureConfig();
    String subscriptionId = context.getSubscriptionId();
    AzureARMTemplate azureARMTemplate = AzureARMTemplate.builder()
                                            .location(context.getDeploymentDataLocation())
                                            .deploymentMode(context.getMode())
                                            .deploymentName(context.getDeploymentName())
                                            .parametersJSON(context.getParametersJson())
                                            .templateJSON(context.getTemplateJson())
                                            .build();

    DeploymentValidateResultInner deploymentValidateResultInner =
        azureManagementClient.validateDeploymentAtSubscriptionScope(azureConfig, subscriptionId, azureARMTemplate);
    ErrorResponse errorResponse = deploymentValidateResultInner.error();
    if (errorResponse != null) {
      throw new InvalidRequestException(
          format("Unable to deploy at subscription scope, deployment validation failed: %s",
              getValidationErrorMsg(errorResponse)));
    }

    DeploymentExtendedInner deploymentExtendedInner =
        azureManagementClient.deployAtSubscriptionScope(azureConfig, subscriptionId, azureARMTemplate);
    // polling for status, when deployment is done return outputs
    return deploymentExtendedInner.properties().outputs().toString();
  }

  public String deployAtManagementGroupScope(DeploymentManagementGroupContext context) {
    AzureConfig azureConfig = context.getAzureConfig();
    String managementGroupId = context.getManagementGroupId();
    AzureARMTemplate azureARMTemplate = AzureARMTemplate.builder()
                                            .location(context.getDeploymentDataLocation())
                                            .deploymentMode(context.getMode())
                                            .deploymentName(context.getDeploymentName())
                                            .parametersJSON(context.getParametersJson())
                                            .templateJSON(context.getTemplateJson())
                                            .build();

    DeploymentValidateResultInner deploymentValidateResultInner =
        azureManagementClient.validateDeploymentAtManagementGroupScope(
            azureConfig, managementGroupId, azureARMTemplate);
    ErrorResponse errorResponse = deploymentValidateResultInner.error();
    if (errorResponse != null) {
      throw new InvalidRequestException(
          format("Unable to deploy at management group scope, deployment validation failed: %s",
              getValidationErrorMsg(errorResponse)));
    }

    DeploymentExtendedInner deploymentExtendedInner =
        azureManagementClient.deployAtManagementGroupScope(azureConfig, managementGroupId, azureARMTemplate);
    // polling for status, when deployment is done return outputs
    return deploymentExtendedInner.properties().outputs().toString();
  }

  public String deployAtTenantScope(DeploymentTenantContext context) {
    AzureConfig azureConfig = context.getAzureConfig();
    AzureARMTemplate azureARMTemplate = AzureARMTemplate.builder()
                                            .location(context.getDeploymentDataLocation())
                                            .deploymentMode(context.getMode())
                                            .deploymentName(context.getDeploymentName())
                                            .parametersJSON(context.getParametersJson())
                                            .templateJSON(context.getTemplateJson())
                                            .build();

    DeploymentValidateResultInner deploymentValidateResultInner =
        azureManagementClient.validateDeploymentAtTenantScope(azureConfig, azureARMTemplate);
    ErrorResponse errorResponse = deploymentValidateResultInner.error();
    if (errorResponse != null) {
      throw new InvalidRequestException(format(
          "Unable to deploy at tenant scope, deployment validation failed: %s", getValidationErrorMsg(errorResponse)));
    }

    DeploymentExtendedInner deploymentExtendedInner =
        azureManagementClient.deployAtTenantScope(azureConfig, azureARMTemplate);
    // polling for status, when deployment is done return outputs
    return deploymentExtendedInner.properties().outputs().toString();
  }

  private String getValidationErrorMsg(ErrorResponse error) {
    return format(
        AzureConstants.DEPLOYMENT_VALIDATION_FAILED_MSG_PATTERN, error.code(), error.message(), error.target());
  }
}
