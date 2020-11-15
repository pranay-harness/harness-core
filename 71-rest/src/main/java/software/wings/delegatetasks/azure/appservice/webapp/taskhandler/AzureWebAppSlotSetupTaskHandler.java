package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.utility.AzureResourceUtility;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSlotSetupParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotSetupResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.azure.appservice.deployment.AzureAppServiceDeploymentContext;
import software.wings.delegatetasks.azure.appservice.deployment.AzureAppServiceDeploymentService;
import software.wings.delegatetasks.azure.appservice.webapp.AbstractAzureWebAppTaskHandler;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureWebAppSlotSetupTaskHandler extends AbstractAzureWebAppTaskHandler {
  @Inject private AzureAppServiceDeploymentService azureAppServiceDeploymentService;

  @Override
  protected AzureAppServiceTaskResponse executeTaskInternal(AzureAppServiceTaskParameters azureAppServiceTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    AzureWebAppSlotSetupParameters azureWebAppSlotSetupParameters =
        (AzureWebAppSlotSetupParameters) azureAppServiceTaskParameters;

    AzureAppServicePreDeploymentData azureAppServicePreDeploymentData =
        getAzureAppServicePreDeploymentData(azureWebAppSlotSetupParameters, azureConfig);

    AzureAppServiceDeploymentContext dockerDeploymentContext =
        toAzureAppServiceDeploymentContext(azureWebAppSlotSetupParameters, azureConfig, logStreamingTaskClient);
    azureAppServiceDeploymentService.deployDockerImage(dockerDeploymentContext);

    return AzureWebAppSlotSetupResponse.builder().preDeploymentData(azureAppServicePreDeploymentData).build();
  }

  private AzureAppServicePreDeploymentData getAzureAppServicePreDeploymentData(
      AzureWebAppSlotSetupParameters azureWebAppSlotSetupParameters, AzureConfig azureConfig) {
    String slotName = azureWebAppSlotSetupParameters.getSlotName();
    AzureWebClientContext azureWebClientContext =
        buildAzureWebClientContext(azureWebAppSlotSetupParameters, azureConfig);
    return azureAppServiceDeploymentService.getAzureAppServicePreDeploymentData(azureWebClientContext, slotName);
  }

  private AzureAppServiceDeploymentContext toAzureAppServiceDeploymentContext(
      AzureWebAppSlotSetupParameters azureWebAppSlotSetupParameters, AzureConfig azureConfig,
      ILogStreamingTaskClient logStreamingTaskClient) {
    AzureWebClientContext azureWebClientContext =
        buildAzureWebClientContext(azureWebAppSlotSetupParameters, azureConfig);
    String imageAndTag = buildDockerImageAndTagPath(
        azureWebAppSlotSetupParameters.getImageName(), azureWebAppSlotSetupParameters.getImageTag());

    return AzureAppServiceDeploymentContext.builder()
        .logStreamingTaskClient(logStreamingTaskClient)
        .appSettings(azureWebAppSlotSetupParameters.getAppSettings())
        .connSettings(azureWebAppSlotSetupParameters.getConnSettings())
        .dockerSettings(azureWebAppSlotSetupParameters.getDockerSettings())
        .imageAndTag(imageAndTag)
        .slotName(azureWebAppSlotSetupParameters.getSlotName())
        .azureWebClientContext(azureWebClientContext)
        .slotStartingSteadyStateTimeoutInMinutes(
            azureWebAppSlotSetupParameters.getSlotStartingSteadyStateTimeoutInMinutes())
        .slotStoppingSteadyStateTimeoutInMinutes(
            azureWebAppSlotSetupParameters.getSlotStoppingSteadyStateTimeoutInMinutes())
        .build();
  }

  private AzureWebClientContext buildAzureWebClientContext(
      AzureWebAppSlotSetupParameters azureWebAppSlotSetupParameters, AzureConfig azureConfig) {
    return AzureWebClientContext.builder()
        .azureConfig(azureConfig)
        .appName(azureWebAppSlotSetupParameters.getWebAppName())
        .subscriptionId(azureWebAppSlotSetupParameters.getSubscriptionId())
        .resourceGroupName(azureWebAppSlotSetupParameters.getResourceGroupName())
        .build();
  }

  private String buildDockerImageAndTagPath(String imageName, String imageTag) {
    return AzureResourceUtility.getDockerImageAndTagPath(imageName, imageTag);
  }
}
