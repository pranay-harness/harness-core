package software.wings.delegatetasks.azure.arm.taskhandler;

import static java.lang.String.format;

import io.harness.azure.context.AzureClientContext;
import io.harness.azure.model.ARMScopeType;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.arm.AzureARMPreDeploymentData;
import io.harness.delegate.task.azure.arm.AzureARMTaskParameters;
import io.harness.delegate.task.azure.arm.AzureARMTaskResponse;
import io.harness.delegate.task.azure.arm.request.AzureARMDeploymentParameters;
import io.harness.delegate.task.azure.arm.response.AzureARMDeploymentResponse;

import software.wings.delegatetasks.azure.arm.AbstractAzureARMTaskHandler;
import software.wings.delegatetasks.azure.arm.deployment.AzureARMDeploymentService;
import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentManagementGroupContext;
import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentResourceGroupContext;
import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentSubscriptionContext;
import software.wings.delegatetasks.azure.arm.deployment.context.DeploymentTenantContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureARMDeploymentTaskHandler extends AbstractAzureARMTaskHandler {
  @Inject private AzureARMDeploymentService azureARMDeploymentService;

  @Override
  protected AzureARMTaskResponse executeTaskInternal(AzureARMTaskParameters azureARMTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    AzureARMDeploymentParameters deploymentParameters = (AzureARMDeploymentParameters) azureARMTaskParameters;
    ARMScopeType deploymentScope = deploymentParameters.getDeploymentScope();

    switch (deploymentScope) {
      case RESOURCE_GROUP:
        azureARMDeploymentService.deployAtResourceGroupScope(
            toDeploymentResourceGroupContext(deploymentParameters, azureConfig, logStreamingTaskClient));
        break;
      case SUBSCRIPTION:
        azureARMDeploymentService.deployAtSubscriptionScope(
            toDeploymentSubscriptionContext(deploymentParameters, azureConfig, logStreamingTaskClient));
        break;
      case MANAGEMENT_GROUP:
        azureARMDeploymentService.deployAtManagementGroupScope(
            toDeploymentManagementGroupContext(deploymentParameters, azureConfig, logStreamingTaskClient));
        break;
      case TENANT:
        azureARMDeploymentService.deployAtTenantScope(
            toDeploymentTenantContext(deploymentParameters, azureConfig, logStreamingTaskClient));
        break;
      default:
        throw new IllegalArgumentException(format("Invalid Azure ARM deployment scope: [%s]", deploymentScope));
    }

    return AzureARMDeploymentResponse.builder().preDeploymentData(AzureARMPreDeploymentData.builder().build()).build();
  }

  private DeploymentResourceGroupContext toDeploymentResourceGroupContext(
      AzureARMDeploymentParameters deploymentParameters, AzureConfig azureConfig,
      ILogStreamingTaskClient logStreamingTaskClient) {
    AzureClientContext azureClientContext = getAzureClientContext(deploymentParameters, azureConfig);

    return DeploymentResourceGroupContext.builder()
        .azureClientContext(azureClientContext)
        .deploymentName(deploymentParameters.getDeploymentName())
        .mode(deploymentParameters.getDeploymentMode())
        .templateJson(deploymentParameters.getTemplateJson())
        .parametersJson(deploymentParameters.getParametersJson())
        .logStreamingTaskClient(logStreamingTaskClient)
        .steadyStateTimeoutInMin(deploymentParameters.getTimeoutIntervalInMin())
        .build();
  }

  @NotNull
  private AzureClientContext getAzureClientContext(
      AzureARMDeploymentParameters deploymentParameters, AzureConfig azureConfig) {
    return new AzureClientContext(
        azureConfig, deploymentParameters.getSubscriptionId(), deploymentParameters.getResourceGroupName());
  }

  private DeploymentSubscriptionContext toDeploymentSubscriptionContext(
      AzureARMDeploymentParameters deploymentParameters, AzureConfig azureConfig,
      ILogStreamingTaskClient logStreamingTaskClient) {
    return DeploymentSubscriptionContext.builder()
        .azureConfig(azureConfig)
        .deploymentName(deploymentParameters.getDeploymentName())
        .deploymentDataLocation(deploymentParameters.getDeploymentDataLocation())
        .subscriptionId(deploymentParameters.getSubscriptionId())
        .mode(deploymentParameters.getDeploymentMode())
        .templateJson(deploymentParameters.getTemplateJson())
        .parametersJson(deploymentParameters.getParametersJson())
        .logStreamingTaskClient(logStreamingTaskClient)
        .steadyStateTimeoutInMin(deploymentParameters.getTimeoutIntervalInMin())
        .build();
  }

  private DeploymentManagementGroupContext toDeploymentManagementGroupContext(
      AzureARMDeploymentParameters deploymentParameters, AzureConfig azureConfig,
      ILogStreamingTaskClient logStreamingTaskClient) {
    return DeploymentManagementGroupContext.builder()
        .azureConfig(azureConfig)
        .deploymentName(deploymentParameters.getDeploymentName())
        .deploymentDataLocation(deploymentParameters.getDeploymentDataLocation())
        .managementGroupId(deploymentParameters.getManagementGroupId())
        .mode(deploymentParameters.getDeploymentMode())
        .templateJson(deploymentParameters.getTemplateJson())
        .parametersJson(deploymentParameters.getParametersJson())
        .logStreamingTaskClient(logStreamingTaskClient)
        .steadyStateTimeoutInMin(deploymentParameters.getTimeoutIntervalInMin())
        .build();
  }

  private DeploymentTenantContext toDeploymentTenantContext(AzureARMDeploymentParameters deploymentParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    return DeploymentTenantContext.builder()
        .azureConfig(azureConfig)
        .deploymentName(deploymentParameters.getDeploymentName())
        .deploymentDataLocation(deploymentParameters.getDeploymentDataLocation())
        .mode(deploymentParameters.getDeploymentMode())
        .templateJson(deploymentParameters.getTemplateJson())
        .parametersJson(deploymentParameters.getParametersJson())
        .logStreamingTaskClient(logStreamingTaskClient)
        .steadyStateTimeoutInMin(deploymentParameters.getTimeoutIntervalInMin())
        .build();
  }
}
