package software.wings.delegatetasks.azure.arm.taskhandler;

import static io.harness.azure.model.AzureConstants.DEPLOYMENT_NAME_PATTERN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.task.azure.arm.AzureARMPreDeploymentData.AzureARMPreDeploymentDataBuilder;
import static io.harness.delegate.task.azure.arm.AzureARMPreDeploymentData.builder;

import static java.lang.String.format;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.context.AzureClientContext;
import io.harness.azure.model.ARMScopeType;
import io.harness.azure.model.AzureConfig;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
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
import java.util.Random;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Singleton
@NoArgsConstructor
@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public class AzureARMDeploymentTaskHandler extends AbstractAzureARMTaskHandler {
  @Inject private AzureARMDeploymentService azureARMDeploymentService;
  private static final Random rand = new Random();

  @Override
  protected AzureARMTaskResponse executeTaskInternal(AzureARMTaskParameters azureARMTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    AzureARMDeploymentParameters deploymentParameters = (AzureARMDeploymentParameters) azureARMTaskParameters;
    ARMScopeType deploymentScope = deploymentParameters.getDeploymentScope();

    switch (deploymentScope) {
      case RESOURCE_GROUP:
        return deployAtResourceGroupScope(azureConfig, logStreamingTaskClient, deploymentParameters);
      case SUBSCRIPTION:
        return deployAtSubscriptionScope(azureConfig, logStreamingTaskClient, deploymentParameters);
      case MANAGEMENT_GROUP:
        return deployAtManagementGroupScope(azureConfig, logStreamingTaskClient, deploymentParameters);
      case TENANT:
        return deployAtTenantScope(azureConfig, logStreamingTaskClient, deploymentParameters);
      default:
        throw new IllegalArgumentException(format("Invalid Azure ARM deployment scope: [%s]", deploymentScope));
    }
  }

  private AzureARMDeploymentResponse deployAtResourceGroupScope(AzureConfig azureConfig,
      ILogStreamingTaskClient logStreamingTaskClient, AzureARMDeploymentParameters deploymentParameters) {
    AzureARMPreDeploymentDataBuilder preDeploymentDataBuilder =
        builder()
            .resourceGroup(deploymentParameters.getResourceGroupName())
            .subscriptionId(deploymentParameters.getSubscriptionId());
    try {
      DeploymentResourceGroupContext context =
          toDeploymentResourceGroupContext(deploymentParameters, azureConfig, logStreamingTaskClient);
      if (!deploymentParameters.isRollback()) {
        azureARMDeploymentService.validateTemplate(context);
        String existingResourceGroupTemplate = azureARMDeploymentService.exportExistingResourceGroupTemplate(context);
        preDeploymentDataBuilder.resourceGroupTemplateJson(existingResourceGroupTemplate);
      }
      String outPuts = azureARMDeploymentService.deployAtResourceGroupScope(context);
      return AzureARMDeploymentResponse.builder()
          .outputs(outPuts)
          .preDeploymentData(preDeploymentDataBuilder.build())
          .build();
    } catch (Exception exception) {
      return AzureARMDeploymentResponse.builder()
          .errorMsg(exception.getMessage())
          .preDeploymentData(preDeploymentDataBuilder.build())
          .build();
    }
  }

  private DeploymentResourceGroupContext toDeploymentResourceGroupContext(
      AzureARMDeploymentParameters deploymentParameters, AzureConfig azureConfig,
      ILogStreamingTaskClient logStreamingTaskClient) {
    AzureClientContext azureClientContext = getAzureClientContext(deploymentParameters, azureConfig);

    return DeploymentResourceGroupContext.builder()
        .azureClientContext(azureClientContext)
        .deploymentName(getDeploymentName(deploymentParameters))
        .mode(deploymentParameters.getDeploymentMode())
        .templateJson(deploymentParameters.getTemplateJson())
        .parametersJson(deploymentParameters.getParametersJson())
        .logStreamingTaskClient(logStreamingTaskClient)
        .steadyStateTimeoutInMin(deploymentParameters.getTimeoutIntervalInMin())
        .isRollback(deploymentParameters.isRollback())
        .build();
  }

  @NotNull
  private AzureClientContext getAzureClientContext(
      AzureARMDeploymentParameters deploymentParameters, AzureConfig azureConfig) {
    return new AzureClientContext(
        azureConfig, deploymentParameters.getSubscriptionId(), deploymentParameters.getResourceGroupName());
  }

  private AzureARMDeploymentResponse deployAtSubscriptionScope(AzureConfig azureConfig,
      ILogStreamingTaskClient logStreamingTaskClient, AzureARMDeploymentParameters deploymentParameters) {
    String outputs = azureARMDeploymentService.deployAtSubscriptionScope(
        toDeploymentSubscriptionContext(deploymentParameters, azureConfig, logStreamingTaskClient));
    return populateDeploymentResponse(outputs);
  }

  private DeploymentSubscriptionContext toDeploymentSubscriptionContext(
      AzureARMDeploymentParameters deploymentParameters, AzureConfig azureConfig,
      ILogStreamingTaskClient logStreamingTaskClient) {
    return DeploymentSubscriptionContext.builder()
        .azureConfig(azureConfig)
        .deploymentName(getDeploymentName(deploymentParameters))
        .deploymentDataLocation(deploymentParameters.getDeploymentDataLocation())
        .subscriptionId(deploymentParameters.getSubscriptionId())
        .mode(deploymentParameters.getDeploymentMode())
        .templateJson(deploymentParameters.getTemplateJson())
        .parametersJson(deploymentParameters.getParametersJson())
        .logStreamingTaskClient(logStreamingTaskClient)
        .steadyStateTimeoutInMin(deploymentParameters.getTimeoutIntervalInMin())
        .build();
  }

  private AzureARMDeploymentResponse deployAtManagementGroupScope(AzureConfig azureConfig,
      ILogStreamingTaskClient logStreamingTaskClient, AzureARMDeploymentParameters deploymentParameters) {
    String outputs = azureARMDeploymentService.deployAtManagementGroupScope(
        toDeploymentManagementGroupContext(deploymentParameters, azureConfig, logStreamingTaskClient));
    return populateDeploymentResponse(outputs);
  }

  private DeploymentManagementGroupContext toDeploymentManagementGroupContext(
      AzureARMDeploymentParameters deploymentParameters, AzureConfig azureConfig,
      ILogStreamingTaskClient logStreamingTaskClient) {
    return DeploymentManagementGroupContext.builder()
        .azureConfig(azureConfig)
        .deploymentName(getDeploymentName(deploymentParameters))
        .deploymentDataLocation(deploymentParameters.getDeploymentDataLocation())
        .managementGroupId(deploymentParameters.getManagementGroupId())
        .mode(deploymentParameters.getDeploymentMode())
        .templateJson(deploymentParameters.getTemplateJson())
        .parametersJson(deploymentParameters.getParametersJson())
        .logStreamingTaskClient(logStreamingTaskClient)
        .steadyStateTimeoutInMin(deploymentParameters.getTimeoutIntervalInMin())
        .build();
  }

  private AzureARMDeploymentResponse deployAtTenantScope(AzureConfig azureConfig,
      ILogStreamingTaskClient logStreamingTaskClient, AzureARMDeploymentParameters deploymentParameters) {
    String outputs = azureARMDeploymentService.deployAtTenantScope(
        toDeploymentTenantContext(deploymentParameters, azureConfig, logStreamingTaskClient));
    return populateDeploymentResponse(outputs);
  }

  private DeploymentTenantContext toDeploymentTenantContext(AzureARMDeploymentParameters deploymentParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    return DeploymentTenantContext.builder()
        .azureConfig(azureConfig)
        .deploymentName(getDeploymentName(deploymentParameters))
        .deploymentDataLocation(deploymentParameters.getDeploymentDataLocation())
        .mode(deploymentParameters.getDeploymentMode())
        .templateJson(deploymentParameters.getTemplateJson())
        .parametersJson(deploymentParameters.getParametersJson())
        .logStreamingTaskClient(logStreamingTaskClient)
        .steadyStateTimeoutInMin(deploymentParameters.getTimeoutIntervalInMin())
        .build();
  }

  private AzureARMDeploymentResponse populateDeploymentResponse(String outputs) {
    return AzureARMDeploymentResponse.builder().outputs(outputs).preDeploymentData(builder().build()).build();
  }

  private String getDeploymentName(AzureARMDeploymentParameters deploymentParameters) {
    if (!isEmpty(deploymentParameters.getDeploymentName())) {
      return deploymentParameters.getDeploymentName();
    }
    int randomNum = rand.nextInt(1000);
    return deploymentParameters.isRollback()
        ? String.format(DEPLOYMENT_NAME_PATTERN, "rollback_" + randomNum, System.currentTimeMillis())
        : String.format(DEPLOYMENT_NAME_PATTERN, randomNum, System.currentTimeMillis());
  }
}
