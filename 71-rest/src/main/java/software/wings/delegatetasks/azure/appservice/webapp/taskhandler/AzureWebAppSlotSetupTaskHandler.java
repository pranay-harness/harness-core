package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import static io.harness.azure.model.AzureConstants.ACR_ACCESS_KEYS_BLANK_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.ACR_USERNAME_BLANK_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.DOCKER_REGISTRY_SERVER_SECRET_PROPERTY_NAME;
import static io.harness.azure.model.AzureConstants.DOCKER_REGISTRY_SERVER_USERNAME_PROPERTY_NAME;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceDockerSetting;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.utility.AzureResourceUtility;
import io.harness.delegate.beans.azure.registry.AzureRegistry;
import io.harness.delegate.beans.azure.registry.AzureRegistryFactory;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureContainerRegistryConnectorDTO;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskResponse;
import io.harness.delegate.task.azure.appservice.webapp.request.AzureWebAppSlotSetupParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotSetupResponse;
import io.harness.exception.InvalidArgumentsException;

import software.wings.delegatetasks.azure.appservice.deployment.AzureAppServiceDeploymentService;
import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServiceDockerDeploymentContext;
import software.wings.delegatetasks.azure.appservice.webapp.AbstractAzureWebAppTaskHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.azure.management.containerregistry.AccessKeyType;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

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

    AzureAppServiceDockerDeploymentContext dockerDeploymentContext =
        toAzureAppServiceDockerDeploymentContext(azureWebAppSlotSetupParameters, azureConfig, logStreamingTaskClient);
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

  private AzureAppServiceDockerDeploymentContext toAzureAppServiceDockerDeploymentContext(
      AzureWebAppSlotSetupParameters azureWebAppSlotSetupParameters, AzureConfig azureConfig,
      ILogStreamingTaskClient logStreamingTaskClient) {
    AzureWebClientContext azureWebClientContext =
        buildAzureWebClientContext(azureWebAppSlotSetupParameters, azureConfig);

    ConnectorConfigDTO connectorConfigDTO = azureWebAppSlotSetupParameters.getConnectorConfigDTO();
    AzureRegistryType azureRegistryType = azureWebAppSlotSetupParameters.getAzureRegistryType();
    Map<String, AzureAppServiceDockerSetting> dockerSettings =
        getAzureAppServiceDockerSettings(connectorConfigDTO, azureRegistryType, azureConfig);

    String imagePathAndTag = AzureResourceUtility.getDockerImagePathAndTagPath(
        azureWebAppSlotSetupParameters.getImageName(), azureWebAppSlotSetupParameters.getImageTag());

    return AzureAppServiceDockerDeploymentContext.builder()
        .logStreamingTaskClient(logStreamingTaskClient)
        .appSettings(azureWebAppSlotSetupParameters.getAppSettings())
        .connSettings(azureWebAppSlotSetupParameters.getConnSettings())
        .dockerSettings(dockerSettings)
        .imagePathAndTag(imagePathAndTag)
        .slotName(azureWebAppSlotSetupParameters.getSlotName())
        .azureWebClientContext(azureWebClientContext)
        .steadyStateTimeoutInMin(azureWebAppSlotSetupParameters.getTimeoutIntervalInMin())
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

  private Map<String, AzureAppServiceDockerSetting> getAzureAppServiceDockerSettings(
      ConnectorConfigDTO connectorConfigDTO, AzureRegistryType azureRegistryType, AzureConfig azureConfig) {
    AzureRegistry azureRegistry = AzureRegistryFactory.getAzureRegistry(azureRegistryType);
    Map<String, AzureAppServiceDockerSetting> dockerSettings = azureRegistry.getContainerSettings(connectorConfigDTO);

    if (AzureRegistryType.ACR == azureRegistryType) {
      RegistryCredentials registryCredentials = azureAppServiceDeploymentService.getContainerRegistryCredentials(
          azureConfig, (AzureContainerRegistryConnectorDTO) connectorConfigDTO);
      updateACRDockerSettingByCredentials(dockerSettings, registryCredentials);
    }

    return dockerSettings;
  }

  private void updateACRDockerSettingByCredentials(
      Map<String, AzureAppServiceDockerSetting> dockerSettings, RegistryCredentials registryCredentials) {
    String username = getACRUsername(registryCredentials);
    String accessKey = getACRAccessKey(registryCredentials);

    dockerSettings.put(DOCKER_REGISTRY_SERVER_USERNAME_PROPERTY_NAME,
        AzureAppServiceDockerSetting.builder()
            .name(DOCKER_REGISTRY_SERVER_USERNAME_PROPERTY_NAME)
            .sticky(false)
            .value(username)
            .build());

    dockerSettings.put(DOCKER_REGISTRY_SERVER_SECRET_PROPERTY_NAME,
        AzureAppServiceDockerSetting.builder()
            .name(DOCKER_REGISTRY_SERVER_SECRET_PROPERTY_NAME)
            .sticky(false)
            .value(accessKey)
            .build());
  }

  @NotNull
  private String getACRUsername(RegistryCredentials registryCredentials) {
    String username = registryCredentials.username();
    if (isBlank(username)) {
      throw new InvalidArgumentsException(ACR_USERNAME_BLANK_VALIDATION_MSG);
    }
    return username;
  }

  @NotNull
  private String getACRAccessKey(RegistryCredentials registryCredentials) {
    Map<AccessKeyType, String> accessKeyTypeStringMap = registryCredentials.accessKeys();
    String accessKey = accessKeyTypeStringMap.get(AccessKeyType.PRIMARY);

    if (isBlank(accessKey)) {
      log.warn("ACR primary access key is null or empty trying to use secondary");
      accessKey = accessKeyTypeStringMap.get(AccessKeyType.SECONDARY);
    }

    if (isBlank(accessKey)) {
      throw new InvalidArgumentsException(ACR_ACCESS_KEYS_BLANK_VALIDATION_MSG);
    }
    return accessKey;
  }
}
