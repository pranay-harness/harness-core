package software.wings.delegatetasks.azure.appservice.deployment;

import static io.harness.azure.model.AzureConstants.SLOT_NAME_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.SLOT_STARTING_STATUS_CHECK_INTERVAL;
import static io.harness.azure.model.AzureConstants.SLOT_STOPPING_STATUS_CHECK_INTERVAL;
import static io.harness.azure.model.AzureConstants.START_DEPLOYMENT_SLOT;
import static io.harness.azure.model.AzureConstants.STOP_DEPLOYMENT_SLOT;
import static io.harness.azure.model.AzureConstants.UPDATE_DEPLOYMENT_SLOT_CONFIGURATION_SETTINGS;
import static io.harness.azure.model.AzureConstants.UPDATE_DEPLOYMENT_SLOT_CONTAINER_SETTINGS;
import static io.harness.azure.model.AzureConstants.WEB_APP_NAME_BLANK_ERROR_MSG;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.INFO;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.client.AzureContainerRegistryClient;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureContainerRegistryClientContext;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureAppServiceDockerSetting;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.AzureConstants;
import io.harness.azure.model.WebAppHostingOS;
import io.harness.delegate.beans.connector.azureconnector.AzureContainerRegistryConnectorDTO;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.deployment.context.AzureAppServiceDeploymentContext;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.LogCallback;

import software.wings.delegatetasks.azure.AzureTimeLimiter;
import software.wings.delegatetasks.azure.DefaultCompletableSubscriber;
import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServiceDockerDeploymentContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureAppServiceDeploymentService {
  @Inject private AzureWebClient azureWebClient;
  @Inject private AzureContainerRegistryClient azureContainerRegistryClient;
  @Inject private AzureTimeLimiter azureTimeLimiter;

  public void deployDockerImage(AzureAppServiceDockerDeploymentContext deploymentContext) {
    validateContextForDockerDeployment(deploymentContext);

    AzureWebClientContext azureWebClientContext = deploymentContext.getAzureWebClientContext();
    String slotName = deploymentContext.getSlotName();
    long slotStoppingSteadyStateTimeoutInMinutes = deploymentContext.getSteadyStateTimeoutInMin();
    long slotStartingSteadyStateTimeoutInMinutes = deploymentContext.getSteadyStateTimeoutInMin();
    DeploymentSlot deploymentSlot = getDeploymentSlot(azureWebClientContext, slotName);
    ILogStreamingTaskClient logStreamingTaskClient = deploymentContext.getLogStreamingTaskClient();

    stopSlotAsyncWithSteadyCheck(deploymentSlot, logStreamingTaskClient, slotStoppingSteadyStateTimeoutInMinutes);
    updateDeploymentSlotConfigurationSettings(deploymentContext);
    updateDeploymentSlotContainerSettings(deploymentContext);
    startSlotAsyncWithSteadyCheck(deploymentSlot, logStreamingTaskClient, slotStartingSteadyStateTimeoutInMinutes);
  }

  private void validateContextForDockerDeployment(AzureAppServiceDockerDeploymentContext deploymentContext) {
    String webAppName = deploymentContext.getAzureWebClientContext().getAppName();
    if (isBlank(webAppName)) {
      throw new InvalidRequestException(WEB_APP_NAME_BLANK_ERROR_MSG);
    }
    String slotName = deploymentContext.getSlotName();
    if (isBlank(slotName)) {
      throw new InvalidRequestException(SLOT_NAME_BLANK_ERROR_MSG);
    }
    String imageAndTag = deploymentContext.getImagePathAndTag();
    if (isBlank(imageAndTag)) {
      throw new InvalidRequestException(AzureConstants.IMAGE_AND_TAG_BLANK_ERROR_MSG);
    }
  }

  private DeploymentSlot getDeploymentSlot(AzureWebClientContext azureWebClientContext, String slotName) {
    Optional<DeploymentSlot> deploymentSlotOptional =
        azureWebClient.getDeploymentSlotByName(azureWebClientContext, slotName);
    return deploymentSlotOptional.orElseThrow(
        () -> new InvalidRequestException(format("Unable to find deployment slot with name: %s", slotName)));
  }

  private void updateDeploymentSlotConfigurationSettings(AzureAppServiceDeploymentContext deploymentContext) {
    AzureWebClientContext azureWebClientContext = deploymentContext.getAzureWebClientContext();
    Map<String, AzureAppServiceApplicationSetting> appSettings = deploymentContext.getAppSettings();
    Map<String, AzureAppServiceConnectionString> connSettings = deploymentContext.getConnSettings();
    String slotName = deploymentContext.getSlotName();
    ILogStreamingTaskClient logStreamingTaskClient = deploymentContext.getLogStreamingTaskClient();
    LogCallback configurationLogCallback =
        logStreamingTaskClient.obtainLogCallback(UPDATE_DEPLOYMENT_SLOT_CONFIGURATION_SETTINGS);

    configurationLogCallback.saveExecutionLog(format("Start updating [%s] deployment slot configuration", slotName));
    updateDeploymentSlotAppSettings(azureWebClientContext, slotName, appSettings, configurationLogCallback);
    updateDeploymentSlotConnectionSettings(azureWebClientContext, slotName, connSettings, configurationLogCallback);
    configurationLogCallback.saveExecutionLog("Deployment slot configuration updated successfully", INFO, SUCCESS);
  }

  private void updateDeploymentSlotAppSettings(AzureWebClientContext azureWebClientContext, String slotName,
      Map<String, AzureAppServiceApplicationSetting> appSettings, LogCallback configurationLogCallback) {
    Set<String> appSettingKeys = appSettings.keySet();
    if (appSettingKeys.isEmpty()) {
      configurationLogCallback.saveExecutionLog(
          format("Application settings list for updating slot configuration is empty, slot name [%s]", slotName));
      return;
    }

    String appSettingKeysStr = Arrays.toString(appSettingKeys.toArray(new String[0]));
    configurationLogCallback.saveExecutionLog(
        format("Start updating [%s] deployment slot application settings: [%s]", slotName, appSettingKeysStr));
    azureWebClient.updateDeploymentSlotAppSettings(azureWebClientContext, slotName, appSettings);
    configurationLogCallback.saveExecutionLog(
        format("Application settings updated successfully for slot [%s]", slotName));
  }

  private void updateDeploymentSlotConnectionSettings(AzureWebClientContext azureWebClientContext, String slotName,
      Map<String, AzureAppServiceConnectionString> connSettings, LogCallback configurationLogCallback) {
    Set<String> connSettingKeys = connSettings.keySet();
    if (connSettingKeys.isEmpty()) {
      configurationLogCallback.saveExecutionLog(
          format("Connection settings list for updating slot configuration is empty, slot name [%s]", slotName));
      return;
    }

    String connSettingKeysStr = Arrays.toString(connSettingKeys.toArray(new String[0]));
    configurationLogCallback.saveExecutionLog(
        format("Start updating [%s] deployment slot connection settings: [%s]", slotName, connSettingKeysStr));
    azureWebClient.updateDeploymentSlotConnectionSettings(azureWebClientContext, slotName, connSettings);
    configurationLogCallback.saveExecutionLog(
        format("Connection settings updated successfully for slot [%s]", slotName));
  }

  private void updateDeploymentSlotContainerSettings(AzureAppServiceDockerDeploymentContext deploymentContext) {
    AzureWebClientContext azureWebClientContext = deploymentContext.getAzureWebClientContext();
    Map<String, AzureAppServiceDockerSetting> dockerSettings = deploymentContext.getDockerSettings();
    String imageAndTag = deploymentContext.getImagePathAndTag();
    String slotName = deploymentContext.getSlotName();
    ILogStreamingTaskClient logStreamingTaskClient = deploymentContext.getLogStreamingTaskClient();
    LogCallback containerLogCallback =
        logStreamingTaskClient.obtainLogCallback(UPDATE_DEPLOYMENT_SLOT_CONTAINER_SETTINGS);

    containerLogCallback.saveExecutionLog(format("Start updating [%s] deployment slot container settings", slotName));
    deleteDeploymentSlotContainerSettings(azureWebClientContext, slotName, containerLogCallback);
    deleteDeploymentSlotDockerImageNameAndTagSettings(azureWebClientContext, slotName, containerLogCallback);
    updateDeploymentSlotContainerSettings(azureWebClientContext, slotName, dockerSettings, containerLogCallback);
    updateDeploymentSlotDockerImageNameAndTagSettings(
        azureWebClientContext, slotName, imageAndTag, containerLogCallback);
    containerLogCallback.saveExecutionLog("Deployment slot container settings updated successfully", INFO, SUCCESS);
  }

  private void deleteDeploymentSlotContainerSettings(
      AzureWebClientContext azureWebClientContext, String slotName, LogCallback containerLogCallback) {
    containerLogCallback.saveExecutionLog(
        format("Start cleaning [%s] deployment slot existing container settings", slotName));
    azureWebClient.deleteDeploymentSlotDockerSettings(azureWebClientContext, slotName);
    containerLogCallback.saveExecutionLog(
        format("Existing container settings for [%s] deployment slot deleted successfully", slotName));
  }

  private void deleteDeploymentSlotDockerImageNameAndTagSettings(
      AzureWebClientContext azureWebClientContext, String slotName, LogCallback containerLogCallback) {
    containerLogCallback.saveExecutionLog(
        format("Start cleaning [%s] deployment slot existing image settings", slotName));
    azureWebClient.deleteDeploymentSlotDockerImageNameAndTagSettings(azureWebClientContext, slotName);
    containerLogCallback.saveExecutionLog(
        format("Existing image settings for [%s] deployment slot deleted successfully", slotName));
  }

  private void updateDeploymentSlotContainerSettings(AzureWebClientContext azureWebClientContext, String slotName,
      Map<String, AzureAppServiceDockerSetting> dockerSettings, LogCallback containerLogCallback) {
    Set<String> containerSettingKeys = dockerSettings.keySet();
    if (containerSettingKeys.isEmpty()) {
      containerLogCallback.saveExecutionLog(
          format("Docker settings list for updating slot configuration is empty, slot name [%s]", slotName));
      return;
    }

    String containerSettingKeysStr = Arrays.toString(containerSettingKeys.toArray(new String[0]));
    containerLogCallback.saveExecutionLog(
        format("Start updating [%s] deployment slot container settings: [%s]", slotName, containerSettingKeysStr));
    azureWebClient.updateDeploymentSlotDockerSettings(azureWebClientContext, slotName, dockerSettings);
    containerLogCallback.saveExecutionLog(format("Container settings updated successfully for slot [%s]", slotName));
  }

  private void updateDeploymentSlotDockerImageNameAndTagSettings(AzureWebClientContext azureWebClientContext,
      String slotName, String newImageAndTag, LogCallback containerLogCallback) {
    WebAppHostingOS webAppHostingOS = azureWebClient.getWebAppHostingOS(azureWebClientContext);
    containerLogCallback.saveExecutionLog(
        format("Start updating [%s] deployment slot container image and tag [%s], web app hosting OS [%s]", slotName,
            newImageAndTag, webAppHostingOS));
    azureWebClient.updateDeploymentSlotDockerImageNameAndTagSettings(
        azureWebClientContext, slotName, newImageAndTag, webAppHostingOS);
    containerLogCallback.saveExecutionLog(format("Image and tag updated successfully for slot [%s]", slotName));
  }

  private void startSlotAsyncWithSteadyCheck(DeploymentSlot deploymentSlot,
      ILogStreamingTaskClient logStreamingTaskClient, long slotStartingSteadyStateTimeoutInMinutes) {
    LogCallback startLogCallback = logStreamingTaskClient.obtainLogCallback(START_DEPLOYMENT_SLOT);
    String slotName = deploymentSlot.name();
    DefaultCompletableSubscriber defaultSubscriber = new DefaultCompletableSubscriber();

    startLogCallback.saveExecutionLog(format("Sending request for starting [%s] deployment slot", slotName));
    deploymentSlot.startAsync().subscribe(defaultSubscriber);
    startLogCallback.saveExecutionLog("Request sent successfully");

    startLogCallback.saveExecutionLog(format("Starting [%s] deployment slot", deploymentSlot.name()));
    Supplier<Void> getSlotStatus = () -> {
      String slotState = deploymentSlot.state();
      startLogCallback.saveExecutionLog(format("Current [%s] deployment slot state [%s]", slotName, slotState));
      return null;
    };

    azureTimeLimiter.waitUntilCompleteWithTimeout(slotStartingSteadyStateTimeoutInMinutes,
        SLOT_STARTING_STATUS_CHECK_INTERVAL, defaultSubscriber, getSlotStatus, startLogCallback, START_DEPLOYMENT_SLOT);
    startLogCallback.saveExecutionLog("Deployment slot started successfully", INFO, SUCCESS);
  }

  private void stopSlotAsyncWithSteadyCheck(DeploymentSlot deploymentSlot,
      ILogStreamingTaskClient logStreamingTaskClient, long slotStartingSteadyStateTimeoutInMinutes) {
    LogCallback stopLogCallback = logStreamingTaskClient.obtainLogCallback(STOP_DEPLOYMENT_SLOT);
    String slotName = deploymentSlot.name();
    DefaultCompletableSubscriber defaultSubscriber = new DefaultCompletableSubscriber();

    stopLogCallback.saveExecutionLog(format("Sending request for stopping [%s] deployment slot", slotName));
    deploymentSlot.stopAsync().subscribe(defaultSubscriber);
    stopLogCallback.saveExecutionLog("Request sent successfully");

    stopLogCallback.saveExecutionLog(format("Stopping [%s] deployment slot", slotName));
    Supplier<Void> getSlotStatus = () -> {
      String slotState = deploymentSlot.state();
      stopLogCallback.saveExecutionLog(format("Current [%s] deployment slot state [%s]", slotName, slotState));
      return null;
    };

    azureTimeLimiter.waitUntilCompleteWithTimeout(slotStartingSteadyStateTimeoutInMinutes,
        SLOT_STOPPING_STATUS_CHECK_INTERVAL, defaultSubscriber, getSlotStatus, stopLogCallback, STOP_DEPLOYMENT_SLOT);
    stopLogCallback.saveExecutionLog("Deployment slot stopped successfully", INFO, SUCCESS);
  }

  public AzureAppServicePreDeploymentData getAzureAppServicePreDeploymentData(
      AzureWebClientContext azureWebClientContext, final String slotName) {
    Map<String, AzureAppServiceApplicationSetting> appSettings =
        azureWebClient.listDeploymentSlotAppSettings(azureWebClientContext, slotName);
    Map<String, AzureAppServiceConnectionString> connSettings =
        azureWebClient.listDeploymentSlotConnectionSettings(azureWebClientContext, slotName);
    Map<String, AzureAppServiceDockerSetting> dockerSettings =
        azureWebClient.listDeploymentSlotDockerSettings(azureWebClientContext, slotName);
    return AzureAppServicePreDeploymentData.builder()
        .appSettings(appSettings)
        .connSettings(connSettings)
        .dockerSettings(dockerSettings)
        .appName(azureWebClientContext.getAppName())
        .slotName(slotName)
        .build();
  }

  public RegistryCredentials getContainerRegistryCredentials(
      AzureConfig azureConfig, AzureContainerRegistryConnectorDTO connectorConfigDTO) {
    String azureRegistryName = connectorConfigDTO.getAzureRegistryName();
    String subscriptionId = connectorConfigDTO.getSubscriptionId();
    String resourceGroupName = fixResourceGroupName(azureConfig, connectorConfigDTO, azureRegistryName, subscriptionId);
    log.info(
        "Start getting container registry credentials azureRegistryName: {}, resourceGroupName: {}, subscriptionId: {}",
        azureRegistryName, resourceGroupName, subscriptionId);
    Optional<RegistryCredentials> containerRegistryCredentialsOp =
        azureContainerRegistryClient.getContainerRegistryCredentials(AzureContainerRegistryClientContext.builder()
                                                                         .azureConfig(azureConfig)
                                                                         .subscriptionId(subscriptionId)
                                                                         .registryName(azureRegistryName)
                                                                         .resourceGroupName(resourceGroupName)
                                                                         .build());

    return containerRegistryCredentialsOp.orElseThrow(
        ()
            -> new InvalidRequestException(format(
                "Not found container registry credentials, azureRegistryName: %s, subscriptionId: %s, resourceGroupName: %s ",
                azureRegistryName, subscriptionId, resourceGroupName)));
  }

  private String fixResourceGroupName(AzureConfig azureConfig, AzureContainerRegistryConnectorDTO acrConnectorConfigDTO,
      String azureRegistryName, String subscriptionId) {
    String resourceGroupName = acrConnectorConfigDTO.getResourceGroupName();
    if (isBlank(resourceGroupName)) {
      log.info(
          "Resource group name is blank, start filtering subscription by container registry name: {}, subscriptionId: {}",
          azureRegistryName, subscriptionId);
      Optional<Registry> registryOp = azureContainerRegistryClient.findFirstContainerRegistryByNameOnSubscription(
          azureConfig, subscriptionId, azureRegistryName);
      Registry registry =
          registryOp.orElseThrow(()
                                     -> new InvalidRequestException(
                                         format("Not found Azure container registry by name: %s, subscription id: %s",
                                             azureRegistryName, subscriptionId)));
      resourceGroupName = registry.resourceGroupName();
    }
    return resourceGroupName;
  }
}
