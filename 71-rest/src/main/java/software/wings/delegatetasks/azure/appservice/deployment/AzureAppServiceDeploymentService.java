package software.wings.delegatetasks.azure.appservice.deployment;

import static io.harness.azure.model.AzureConstants.SLOT_NAME_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.SLOT_STARTING_STATUS_CHECK_INTERVAL;
import static io.harness.azure.model.AzureConstants.SLOT_STOPPING_STATUS_CHECK_INTERVAL;
import static io.harness.azure.model.AzureConstants.SLOT_SWAP;
import static io.harness.azure.model.AzureConstants.SLOT_TRAFFIC_WEIGHT;
import static io.harness.azure.model.AzureConstants.START_DEPLOYMENT_SLOT;
import static io.harness.azure.model.AzureConstants.STOP_DEPLOYMENT_SLOT;
import static io.harness.azure.model.AzureConstants.SUCCESS_REQUEST;
import static io.harness.azure.model.AzureConstants.UPDATE_DEPLOYMENT_SLOT_CONFIGURATION_SETTINGS;
import static io.harness.azure.model.AzureConstants.UPDATE_DEPLOYMENT_SLOT_CONTAINER_SETTINGS;
import static io.harness.azure.model.AzureConstants.WEB_APP_NAME_BLANK_ERROR_MSG;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.INFO;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.client.AzureContainerRegistryClient;
import io.harness.azure.client.AzureMonitorClient;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureContainerRegistryClientContext;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureAppServiceDockerSetting;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.AzureConstants;
import io.harness.azure.model.WebAppHostingOS;
import io.harness.azure.utility.AzureResourceUtility;
import io.harness.delegate.beans.connector.azureconnector.AzureContainerRegistryConnectorDTO;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureAppDeploymentData;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.LogCallback;

import software.wings.delegatetasks.azure.AzureTimeLimiter;
import software.wings.delegatetasks.azure.DefaultCompletableSubscriber;
import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServiceDeploymentContext;
import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServiceDockerDeploymentContext;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import com.microsoft.azure.management.monitor.EventData;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

@Singleton
@NoArgsConstructor
@Slf4j
public class AzureAppServiceDeploymentService {
  @Inject private AzureWebClient azureWebClient;
  @Inject private AzureContainerRegistryClient azureContainerRegistryClient;
  @Inject private AzureTimeLimiter azureTimeLimiter;
  @Inject private AzureMonitorClient azureMonitorClient;

  public void deployDockerImage(AzureAppServiceDockerDeploymentContext deploymentContext) {
    validateContextForDockerDeployment(deploymentContext);
    log.info("Start deploying docker image: {} on slot: {}", deploymentContext.getImagePathAndTag(),
        deploymentContext.getSlotName());
    int steadyStateTimeoutInMin = deploymentContext.getSteadyStateTimeoutInMin();
    ILogStreamingTaskClient logStreamingTaskClient = deploymentContext.getLogStreamingTaskClient();

    DeploymentSlot deploymentSlot =
        getDeploymentSlot(deploymentContext.getAzureWebClientContext(), deploymentContext.getSlotName());

    stopSlotAsyncWithSteadyCheck(deploymentSlot, logStreamingTaskClient, steadyStateTimeoutInMin);
    updateDeploymentSlotConfigurationSettings(deploymentContext);
    updateDeploymentSlotContainerSettings(deploymentContext);
    startSlotAsyncWithSteadyCheck(deploymentSlot, logStreamingTaskClient, steadyStateTimeoutInMin);
  }

  public List<AzureAppDeploymentData> fetchDeploymentData(
      AzureWebClientContext azureWebClientContext, String slotName) {
    log.info("Start fetching deployment data for app name: {}, slot: {}", azureWebClientContext.getAppName(), slotName);
    Optional<DeploymentSlot> deploymentSlotName =
        azureWebClient.getDeploymentSlotByName(azureWebClientContext, slotName);

    if (!deploymentSlotName.isPresent()) {
      throw new InvalidRequestException(
          format("Deployment slot - [%s] not found for Web App - [%s]", slotName, azureWebClientContext.getAppName()));
    }

    DeploymentSlot deploymentSlot = deploymentSlotName.get();

    return Collections.singletonList(AzureAppDeploymentData.builder()
                                         .subscriptionId(azureWebClientContext.getSubscriptionId())
                                         .resourceGroup(azureWebClientContext.getResourceGroupName())
                                         .appName(azureWebClientContext.getAppName())
                                         .deploySlot(slotName)
                                         .deploySlotId(deploymentSlot.id())
                                         .appServicePlanId(deploymentSlot.appServicePlanId())
                                         .hostName(deploymentSlot.defaultHostName())
                                         .build());
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
    Map<String, AzureAppServiceApplicationSetting> appSettingsToAdd = deploymentContext.getAppSettingsToAdd();
    Map<String, AzureAppServiceApplicationSetting> appSettingsToRemove = deploymentContext.getAppSettingsToRemove();
    Map<String, AzureAppServiceConnectionString> connSettingsToAdd = deploymentContext.getConnSettingsToAdd();
    Map<String, AzureAppServiceConnectionString> connSettingsToRemove = deploymentContext.getConnSettingsToRemove();
    String slotName = deploymentContext.getSlotName();
    ILogStreamingTaskClient logStreamingTaskClient = deploymentContext.getLogStreamingTaskClient();
    LogCallback configurationLogCallback =
        logStreamingTaskClient.obtainLogCallback(UPDATE_DEPLOYMENT_SLOT_CONFIGURATION_SETTINGS);

    configurationLogCallback.saveExecutionLog(format("Start updating [%s] deployment slot configuration", slotName));
    deleteDeploymentSlotAppSettings(azureWebClientContext, slotName, appSettingsToRemove, configurationLogCallback);
    updateDeploymentSlotAppSettings(azureWebClientContext, slotName, appSettingsToAdd, configurationLogCallback);
    deleteDeploymentSlotConnectionSettings(
        azureWebClientContext, slotName, connSettingsToRemove, configurationLogCallback);
    updateDeploymentSlotConnectionSettings(
        azureWebClientContext, slotName, connSettingsToAdd, configurationLogCallback);
    configurationLogCallback.saveExecutionLog("Deployment slot configuration updated successfully", INFO, SUCCESS);
  }

  private void deleteDeploymentSlotAppSettings(AzureWebClientContext azureWebClientContext, String slotName,
      Map<String, AzureAppServiceApplicationSetting> appSettingsToRemove, LogCallback configurationLogCallback) {
    if (appSettingsToRemove == null || appSettingsToRemove.keySet().isEmpty()) {
      configurationLogCallback.saveExecutionLog(
          format("Application settings list for deleting slot configuration is empty, slot name [%s]", slotName));
      return;
    }

    String appSettingKeysStr = Arrays.toString(appSettingsToRemove.keySet().toArray(new String[0]));
    configurationLogCallback.saveExecutionLog(
        format("Start deleting [%s] deployment slot application settings: [%s]", slotName, appSettingKeysStr));
    azureWebClient.deleteDeploymentSlotAppSettings(azureWebClientContext, slotName, appSettingsToRemove);
    configurationLogCallback.saveExecutionLog(
        format("Application settings deleted successfully for slot [%s]", slotName));
  }

  private void updateDeploymentSlotAppSettings(AzureWebClientContext azureWebClientContext, String slotName,
      Map<String, AzureAppServiceApplicationSetting> appSettings, LogCallback configurationLogCallback) {
    if (appSettings == null || appSettings.keySet().isEmpty()) {
      configurationLogCallback.saveExecutionLog(
          format("Application settings list for updating slot configuration is empty, slot name [%s]", slotName));
      return;
    }

    String appSettingKeysStr = Arrays.toString(appSettings.keySet().toArray(new String[0]));
    configurationLogCallback.saveExecutionLog(
        format("Start updating [%s] deployment slot application settings: [%s]", slotName, appSettingKeysStr));
    azureWebClient.updateDeploymentSlotAppSettings(azureWebClientContext, slotName, appSettings);
    configurationLogCallback.saveExecutionLog(
        format("Application settings updated successfully for slot [%s]", slotName));
  }

  private void deleteDeploymentSlotConnectionSettings(AzureWebClientContext azureWebClientContext, String slotName,
      Map<String, AzureAppServiceConnectionString> connSettingsToRemove, LogCallback configurationLogCallback) {
    if (connSettingsToRemove == null || connSettingsToRemove.keySet().isEmpty()) {
      configurationLogCallback.saveExecutionLog(
          format("Connection settings list for deleting slot configuration is empty, slot name [%s]", slotName));
      return;
    }

    String connSettingKeysStr = Arrays.toString(connSettingsToRemove.keySet().toArray(new String[0]));
    configurationLogCallback.saveExecutionLog(
        format("Start deleting [%s] deployment slot connection settings: [%s]", slotName, connSettingKeysStr));
    azureWebClient.deleteDeploymentSlotConnectionSettings(azureWebClientContext, slotName, connSettingsToRemove);
    configurationLogCallback.saveExecutionLog(
        format("Connection settings deleted successfully for slot [%s]", slotName));
  }

  private void updateDeploymentSlotConnectionSettings(AzureWebClientContext azureWebClientContext, String slotName,
      Map<String, AzureAppServiceConnectionString> connSettings, LogCallback configurationLogCallback) {
    if (connSettings == null || connSettings.keySet().isEmpty()) {
      configurationLogCallback.saveExecutionLog(
          format("Connection settings list for updating slot configuration is empty, slot name [%s]", slotName));
      return;
    }

    String connSettingKeysStr = Arrays.toString(connSettings.keySet().toArray(new String[0]));
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
    startLogCallback.saveExecutionLog(SUCCESS_REQUEST);

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
    stopLogCallback.saveExecutionLog(SUCCESS_REQUEST);

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
      AzureWebClientContext azureWebClientContext, final String slotName,
      Map<String, AzureAppServiceApplicationSetting> userAddedAppSettings,
      Map<String, AzureAppServiceConnectionString> userAddedConnSettings) {
    // app settings
    Map<String, AzureAppServiceApplicationSetting> existingAppSettingsOnSlot =
        azureWebClient.listDeploymentSlotAppSettings(azureWebClientContext, slotName);
    Map<String, AzureAppServiceApplicationSetting> appSettingsNeedBeDeletedInRollback =
        getAppSettingsNeedBeDeletedInRollback(userAddedAppSettings, existingAppSettingsOnSlot);
    Map<String, AzureAppServiceApplicationSetting> appSettingsNeedBeUpdatedInRollback =
        getAppSettingsNeedToBeUpdatedInRollback(userAddedAppSettings, existingAppSettingsOnSlot);

    // connection settings
    Map<String, AzureAppServiceConnectionString> existingConnSettingsOnSlot =
        azureWebClient.listDeploymentSlotConnectionSettings(azureWebClientContext, slotName);
    Map<String, AzureAppServiceConnectionString> connSettingsNeedBeDeletedInRollback =
        getConnSettingsNeedBeDeletedInRollback(userAddedConnSettings, existingConnSettingsOnSlot);
    Map<String, AzureAppServiceConnectionString> connSettingsNeedBeUpdatedInRollback =
        getConnSettingsNeedBeUpdatedInRollback(userAddedConnSettings, existingConnSettingsOnSlot);

    Map<String, AzureAppServiceDockerSetting> dockerSettingsNeedBeUpdatedInRollback =
        azureWebClient.listDeploymentSlotDockerSettings(azureWebClientContext, slotName);
    String dockerImageNameAndTag =
        azureWebClient.getSlotDockerImageNameAndTag(azureWebClientContext, slotName).orElse(EMPTY);
    double slotTrafficWeight = azureWebClient.getDeploymentSlotTrafficWeight(azureWebClientContext, slotName);

    return AzureAppServicePreDeploymentData.builder()
        .appSettingsToRemove(appSettingsNeedBeDeletedInRollback)
        .appSettingsToAdd(appSettingsNeedBeUpdatedInRollback)
        .connSettingsToRemove(connSettingsNeedBeDeletedInRollback)
        .connSettingsToAdd(connSettingsNeedBeUpdatedInRollback)
        .dockerSettingsToAdd(dockerSettingsNeedBeUpdatedInRollback)
        .slotName(slotName)
        .appName(azureWebClientContext.getAppName())
        .imageNameAndTag(dockerImageNameAndTag)
        .trafficWeight(slotTrafficWeight)
        .failedTaskType(AzureAppServiceTaskParameters.AzureAppServiceTaskType.SLOT_SWAP)
        .build();
  }

  @NotNull
  private Map<String, AzureAppServiceConnectionString> getConnSettingsNeedBeDeletedInRollback(
      Map<String, AzureAppServiceConnectionString> userAddedConnSettings,
      Map<String, AzureAppServiceConnectionString> existingConnSettingsOnSlot) {
    Sets.SetView<String> newConnSettingsNeedBeAddedInSlotSetup =
        Sets.difference(userAddedConnSettings.keySet(), existingConnSettingsOnSlot.keySet());

    return userAddedConnSettings.entrySet()
        .stream()
        .filter(entry -> newConnSettingsNeedBeAddedInSlotSetup.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @NotNull
  private Map<String, AzureAppServiceConnectionString> getConnSettingsNeedBeUpdatedInRollback(
      Map<String, AzureAppServiceConnectionString> userAddedConnSettings,
      Map<String, AzureAppServiceConnectionString> existingConnSettingsOnSlot) {
    Sets.SetView<String> commonConnSettingsNeedBeUpdatedInSlotSetup =
        Sets.intersection(userAddedConnSettings.keySet(), existingConnSettingsOnSlot.keySet());

    return existingConnSettingsOnSlot.entrySet()
        .stream()
        .filter(entry -> commonConnSettingsNeedBeUpdatedInSlotSetup.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @NotNull
  private Map<String, AzureAppServiceApplicationSetting> getAppSettingsNeedBeDeletedInRollback(
      Map<String, AzureAppServiceApplicationSetting> userAddedAppSettings,
      Map<String, AzureAppServiceApplicationSetting> existingAppSettingsOnSlot) {
    Sets.SetView<String> newAppSettingsNeedBeAddedInSlotSetup =
        Sets.difference(userAddedAppSettings.keySet(), existingAppSettingsOnSlot.keySet());

    return userAddedAppSettings.entrySet()
        .stream()
        .filter(entry -> newAppSettingsNeedBeAddedInSlotSetup.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @NotNull
  private Map<String, AzureAppServiceApplicationSetting> getAppSettingsNeedToBeUpdatedInRollback(
      Map<String, AzureAppServiceApplicationSetting> userAddedAppSettings,
      Map<String, AzureAppServiceApplicationSetting> existingAppSettingsOnSlot) {
    Sets.SetView<String> commonAppSettingsNeedBeUpdatedInSlotSetup =
        Sets.intersection(userAddedAppSettings.keySet(), existingAppSettingsOnSlot.keySet());

    return existingAppSettingsOnSlot.entrySet()
        .stream()
        .filter(entry -> commonAppSettingsNeedBeUpdatedInSlotSetup.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

  public void rerouteProductionSlotTraffic(AzureWebClientContext webClientContext, String shiftTrafficSlotName,
      double trafficWeightInPercentage, ILogStreamingTaskClient logStreamingTaskClient) {
    LogCallback rerouteTrafficLogCallback = logStreamingTaskClient.obtainLogCallback(SLOT_TRAFFIC_WEIGHT);

    rerouteTrafficLogCallback.saveExecutionLog(format(
        "Start rerouting [%.2f] traffic to deployment slot: [%s] ", trafficWeightInPercentage, shiftTrafficSlotName));
    azureWebClient.rerouteProductionSlotTraffic(webClientContext, shiftTrafficSlotName, trafficWeightInPercentage);
    rerouteTrafficLogCallback.saveExecutionLog("Traffic rerouted successfully", INFO, SUCCESS);
  }

  public void swapSlots(AzureAppServiceDeploymentContext azureAppServiceDeploymentContext, String targetSlotName,
      ILogStreamingTaskClient logStreamingTaskClient) {
    String sourceSlotName = azureAppServiceDeploymentContext.getSlotName();
    int steadyStateTimeoutInMinutes = azureAppServiceDeploymentContext.getSteadyStateTimeoutInMin();
    AzureWebClientContext webClientContext = azureAppServiceDeploymentContext.getAzureWebClientContext();

    LogCallback slotSwapLogCallback = logStreamingTaskClient.obtainLogCallback(SLOT_SWAP);
    DefaultCompletableSubscriber defaultSubscriber = new DefaultCompletableSubscriber();

    slotSwapLogCallback.saveExecutionLog(
        format("Sending request for swapping source [%s] slot with target [%s]", sourceSlotName, targetSlotName));
    AtomicReference<DateTime> startTime = new AtomicReference<>(DateTime.now());
    azureWebClient.swapDeploymentSlotsAsync(webClientContext, sourceSlotName, targetSlotName)
        .subscribe(defaultSubscriber);
    slotSwapLogCallback.saveExecutionLog(SUCCESS_REQUEST);

    slotSwapLogCallback.saveExecutionLog(
        format("Swapping deployment source slot [%s] with target [%s]", sourceSlotName, targetSlotName));
    Supplier<Void> getSwappingStatus = getSwappingSlotsStatus(webClientContext, slotSwapLogCallback, startTime);

    azureTimeLimiter.waitUntilCompleteWithTimeout(steadyStateTimeoutInMinutes, SLOT_STOPPING_STATUS_CHECK_INTERVAL,
        defaultSubscriber, getSwappingStatus, slotSwapLogCallback, SLOT_SWAP);
    slotSwapLogCallback.saveExecutionLog("Swapping slots done successfully", INFO, SUCCESS);
  }

  @NotNull
  private Supplier<Void> getSwappingSlotsStatus(
      AzureWebClientContext webClientContext, LogCallback slotSwapLogCallback, AtomicReference<DateTime> startTime) {
    AzureConfig azureConfig = webClientContext.getAzureConfig();
    String subscriptionId = webClientContext.getSubscriptionId();
    String resourceGroupName = webClientContext.getResourceGroupName();
    return () -> {
      slotSwapLogCallback.saveExecutionLog("Checking swapping slots status");
      List<EventData> eventData = azureMonitorClient.listEventDataWithAllPropertiesByResourceGroupName(
          azureConfig, subscriptionId, resourceGroupName, startTime.get(), DateTime.now());
      slotSwapLogCallback.saveExecutionLog(AzureResourceUtility.activityLogEventDataToString(eventData));
      startTime.set(DateTime.now());
      return null;
    };
  }
}
