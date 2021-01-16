package software.wings.delegatetasks.azure.appservice.deployment;

import static io.harness.azure.model.AzureConstants.SAVE_EXISTING_CONFIGURATIONS;
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
import static io.harness.azure.model.AzureConstants.WEB_APP_INSTANCE_STATUS_RUNNING;
import static io.harness.azure.model.AzureConstants.WEB_APP_NAME_BLANK_ERROR_MSG;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.azure.appservicesettings.value.AzureAppServiceSettingValueType.AZURE_SETTING;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.delegatetasks.azure.appservice.deployment.SlotStatusVerifier.SlotStatusVerifierType.START_VERIFIER;
import static software.wings.delegatetasks.azure.appservice.deployment.SlotStatusVerifier.SlotStatusVerifierType.STOP_VERIFIER;
import static software.wings.delegatetasks.azure.appservice.deployment.SlotStatusVerifier.SlotStatusVerifierType.SWAP_VERIFIER;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
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
import io.harness.delegate.beans.azure.mapper.AzureAppServiceConfigurationDTOMapper;
import io.harness.delegate.beans.connector.azureconnector.AzureContainerRegistryConnectorDTO;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData.AzureAppServicePreDeploymentDataBuilder;
import io.harness.delegate.task.azure.appservice.AzureAppServiceTaskParameters;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureAppDeploymentData;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.LogCallback;

import software.wings.delegatetasks.azure.AzureServiceCallBack;
import software.wings.delegatetasks.azure.AzureTimeLimiter;
import software.wings.delegatetasks.azure.DefaultCompletableSubscriber;
import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServiceDeploymentContext;
import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServiceDockerDeploymentContext;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.implementation.SiteInstanceInner;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import com.microsoft.azure.management.monitor.EventData;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
@TargetModule(Module._930_DELEGATE_TASKS)
public class AzureAppServiceDeploymentService {
  @Inject private AzureWebClient azureWebClient;
  @Inject private AzureContainerRegistryClient azureContainerRegistryClient;
  @Inject private AzureTimeLimiter azureTimeLimiter;
  @Inject private SlotSteadyStateChecker slotSteadyStateChecker;
  @Inject private AzureMonitorClient azureMonitorClient;

  public void deployDockerImage(AzureAppServiceDockerDeploymentContext deploymentContext) {
    validateContextForDockerDeployment(deploymentContext);
    log.info("Start deploying docker image: {} on slot: {}", deploymentContext.getImagePathAndTag(),
        deploymentContext.getSlotName());
    int steadyStateTimeoutInMin = deploymentContext.getSteadyStateTimeoutInMin();
    ILogStreamingTaskClient logStreamingTaskClient = deploymentContext.getLogStreamingTaskClient();

    DeploymentSlot deploymentSlot =
        getDeploymentSlot(deploymentContext.getAzureWebClientContext(), deploymentContext.getSlotName());

    stopSlotAsyncWithSteadyCheck(deploymentSlot, logStreamingTaskClient, steadyStateTimeoutInMin, deploymentContext);
    updateDeploymentSlotConfigurationSettings(deploymentContext);
    updateDeploymentSlotContainerSettings(deploymentContext);
    startSlotAsyncWithSteadyCheck(deploymentSlot, logStreamingTaskClient, steadyStateTimeoutInMin, deploymentContext);
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

    List<SiteInstanceInner> siteInstanceInners =
        azureWebClient.listInstanceIdentifiersSlot(azureWebClientContext, slotName);

    return siteInstanceInners.stream()
        .map(siteInstanceInner
            -> AzureAppDeploymentData.builder()
                   .subscriptionId(azureWebClientContext.getSubscriptionId())
                   .resourceGroup(azureWebClientContext.getResourceGroupName())
                   .appName(azureWebClientContext.getAppName())
                   .deploySlot(slotName)
                   .deploySlotId(deploymentSlot.id())
                   .appServicePlanId(deploymentSlot.appServicePlanId())
                   .hostName(deploymentSlot.defaultHostName())
                   .instanceId(siteInstanceInner.id())
                   .instanceName(siteInstanceInner.name())
                   .instanceType(siteInstanceInner.type())
                   .instanceState(WEB_APP_INSTANCE_STATUS_RUNNING)
                   .build())
        .collect(Collectors.toList());
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

    configurationLogCallback.saveExecutionLog(
        format("Start updating application configurations for slot - [%s]", slotName));

    try {
      deleteDeploymentSlotAppSettings(azureWebClientContext, slotName, appSettingsToRemove, configurationLogCallback);
      updateDeploymentSlotAppSettings(azureWebClientContext, slotName, appSettingsToAdd, configurationLogCallback);
      deleteDeploymentSlotConnectionSettings(
          azureWebClientContext, slotName, connSettingsToRemove, configurationLogCallback);
      updateDeploymentSlotConnectionSettings(
          azureWebClientContext, slotName, connSettingsToAdd, configurationLogCallback);

      configurationLogCallback.saveExecutionLog("Deployment slot configuration updated successfully", INFO, SUCCESS);
    } catch (Exception ex) {
      String message = String.format("Failed to update slot configurations - [%s]", ex.getMessage());
      configurationLogCallback.saveExecutionLog(message, ERROR, FAILURE);
      throw ex;
    }
  }

  private void deleteDeploymentSlotAppSettings(AzureWebClientContext azureWebClientContext, String slotName,
      Map<String, AzureAppServiceApplicationSetting> appSettingsToRemove, LogCallback configurationLogCallback) {
    if (isEmpty(appSettingsToRemove)) {
      return;
    }
    String appSettingKeysStr = Arrays.toString(appSettingsToRemove.keySet().toArray(new String[0]));
    configurationLogCallback.saveExecutionLog(
        format("Deleting following Application settings: %n[%s]", appSettingKeysStr));
    azureWebClient.deleteDeploymentSlotAppSettings(azureWebClientContext, slotName, appSettingsToRemove);
    configurationLogCallback.saveExecutionLog("Application settings deleted successfully");
  }

  private void updateDeploymentSlotAppSettings(AzureWebClientContext azureWebClientContext, String slotName,
      Map<String, AzureAppServiceApplicationSetting> appSettings, LogCallback configurationLogCallback) {
    if (isEmpty(appSettings)) {
      return;
    }

    String appSettingKeysStr = Arrays.toString(appSettings.keySet().toArray(new String[0]));
    configurationLogCallback.saveExecutionLog(
        format("Adding following Application settings: %n[%s]", appSettingKeysStr));
    azureWebClient.updateDeploymentSlotAppSettings(azureWebClientContext, slotName, appSettings);
    configurationLogCallback.saveExecutionLog("Application settings updated successfully");
  }

  private void deleteDeploymentSlotConnectionSettings(AzureWebClientContext azureWebClientContext, String slotName,
      Map<String, AzureAppServiceConnectionString> connSettingsToRemove, LogCallback configurationLogCallback) {
    if (isEmpty(connSettingsToRemove)) {
      return;
    }

    String connSettingKeysStr = Arrays.toString(connSettingsToRemove.keySet().toArray(new String[0]));
    configurationLogCallback.saveExecutionLog(
        format("Deleting following Connection strings: %n[%s]", connSettingKeysStr));
    azureWebClient.deleteDeploymentSlotConnectionStrings(azureWebClientContext, slotName, connSettingsToRemove);
    configurationLogCallback.saveExecutionLog("Connection strings deleted successfully");
  }

  private void updateDeploymentSlotConnectionSettings(AzureWebClientContext azureWebClientContext, String slotName,
      Map<String, AzureAppServiceConnectionString> connSettings, LogCallback configurationLogCallback) {
    if (isEmpty(connSettings)) {
      return;
    }
    String connSettingKeysStr = Arrays.toString(connSettings.keySet().toArray(new String[0]));
    configurationLogCallback.saveExecutionLog(
        format("Adding following Connection strings: %n[%s]", connSettingKeysStr));
    azureWebClient.updateDeploymentSlotConnectionStrings(azureWebClientContext, slotName, connSettings);
    configurationLogCallback.saveExecutionLog("Connection strings updated successfully");
  }

  private void updateDeploymentSlotContainerSettings(AzureAppServiceDockerDeploymentContext deploymentContext) {
    AzureWebClientContext azureWebClientContext = deploymentContext.getAzureWebClientContext();
    Map<String, AzureAppServiceDockerSetting> dockerSettings = deploymentContext.getDockerSettings();
    String imageAndTag = deploymentContext.getImagePathAndTag();
    String slotName = deploymentContext.getSlotName();
    ILogStreamingTaskClient logStreamingTaskClient = deploymentContext.getLogStreamingTaskClient();
    LogCallback containerLogCallback =
        logStreamingTaskClient.obtainLogCallback(UPDATE_DEPLOYMENT_SLOT_CONTAINER_SETTINGS);

    containerLogCallback.saveExecutionLog(format("Start updating Container settings for slot - [%s]", slotName));
    try {
      deleteDeploymentSlotContainerSettings(azureWebClientContext, slotName, containerLogCallback);
      deleteDeploymentSlotDockerImageNameAndTagSettings(azureWebClientContext, slotName, containerLogCallback);
      updateDeploymentSlotContainerSettings(azureWebClientContext, slotName, dockerSettings, containerLogCallback);
      updateDeploymentSlotDockerImageNameAndTagSettings(
          azureWebClientContext, slotName, imageAndTag, containerLogCallback);

      containerLogCallback.saveExecutionLog("Deployment slot container settings updated successfully", INFO, SUCCESS);
    } catch (Exception ex) {
      containerLogCallback.saveExecutionLog(
          String.format("Failed to update Container settings - [%s]", ex.getMessage()), ERROR, FAILURE);
      throw ex;
    }
  }

  private void deleteDeploymentSlotContainerSettings(
      AzureWebClientContext azureWebClientContext, String slotName, LogCallback containerLogCallback) {
    containerLogCallback.saveExecutionLog("Start cleaning existing container settings");
    azureWebClient.deleteDeploymentSlotDockerSettings(azureWebClientContext, slotName);
    containerLogCallback.saveExecutionLog("Existing container settings deleted successfully");
  }

  private void deleteDeploymentSlotDockerImageNameAndTagSettings(
      AzureWebClientContext azureWebClientContext, String slotName, LogCallback containerLogCallback) {
    containerLogCallback.saveExecutionLog("Start cleaning existing image settings");
    azureWebClient.deleteDeploymentSlotDockerImageNameAndTagSettings(azureWebClientContext, slotName);
    containerLogCallback.saveExecutionLog("Existing image settings deleted successfully");
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
    containerLogCallback.saveExecutionLog(format("Start updating Container settings: %n[%s]", containerSettingKeysStr));
    azureWebClient.updateDeploymentSlotDockerSettings(azureWebClientContext, slotName, dockerSettings);
    containerLogCallback.saveExecutionLog("Container settings updated successfully");
  }

  private void updateDeploymentSlotDockerImageNameAndTagSettings(AzureWebClientContext azureWebClientContext,
      String slotName, String newImageAndTag, LogCallback containerLogCallback) {
    WebAppHostingOS webAppHostingOS = azureWebClient.getWebAppHostingOS(azureWebClientContext);
    containerLogCallback.saveExecutionLog(format(
        "Start updating container image and tag: %n[%s], web app hosting OS [%s]", newImageAndTag, webAppHostingOS));
    azureWebClient.updateDeploymentSlotDockerImageNameAndTagSettings(
        azureWebClientContext, slotName, newImageAndTag, webAppHostingOS);
    containerLogCallback.saveExecutionLog(format("Image and tag updated successfully for slot [%s]", slotName));
  }

  private void startSlotAsyncWithSteadyCheck(DeploymentSlot deploymentSlot,
      ILogStreamingTaskClient logStreamingTaskClient, long slotStartingSteadyStateTimeoutInMinutes) {
    LogCallback startLogCallback = logStreamingTaskClient.obtainLogCallback(START_DEPLOYMENT_SLOT);
    String slotName = deploymentSlot.name();
    DefaultCompletableSubscriber defaultSubscriber = new DefaultCompletableSubscriber();

    startLogCallback.saveExecutionLog(format("Sending request for starting deployment slot - [%s]", slotName));
    deploymentSlot.startAsync().subscribe(defaultSubscriber);
    startLogCallback.saveExecutionLog(SUCCESS_REQUEST);

    Supplier<Void> getSlotStatus = () -> {
      String slotState = deploymentSlot.state();
      startLogCallback.saveExecutionLog(format("Current state for deployment slot is - [%s]", slotState));
      return null;
    };

    azureTimeLimiter.waitUntilCompleteWithTimeout(slotStartingSteadyStateTimeoutInMinutes,
        SLOT_STARTING_STATUS_CHECK_INTERVAL, defaultSubscriber, getSlotStatus, startLogCallback, START_DEPLOYMENT_SLOT);
    startLogCallback.saveExecutionLog("Deployment slot started successfully", INFO, SUCCESS);
  }

  private void startSlotAsyncWithSteadyCheck(DeploymentSlot deploymentSlot,
      ILogStreamingTaskClient logStreamingTaskClient, long slotStartingSteadyStateTimeoutInMinutes,
      AzureAppServiceDockerDeploymentContext deploymentContext) {
    LogCallback startLogCallback = logStreamingTaskClient.obtainLogCallback(START_DEPLOYMENT_SLOT);
    String slotName = deploymentSlot.name();

    AzureServiceCallBack restCallBack = new AzureServiceCallBack(startLogCallback, START_DEPLOYMENT_SLOT);
    startLogCallback.saveExecutionLog(format("Sending request for starting deployment slot - [%s]", slotName));
    azureWebClient.startDeploymentSlotAsync(deploymentContext.getAzureWebClientContext(), slotName, restCallBack);
    startLogCallback.saveExecutionLog(SUCCESS_REQUEST);

    SlotStatusVerifier statusVerifier = SlotStatusVerifier.getStatusVerifier(START_VERIFIER, startLogCallback, slotName,
        azureWebClient, null, deploymentContext.getAzureWebClientContext(), restCallBack);
    slotSteadyStateChecker.waitUntilCompleteWithTimeout(slotStartingSteadyStateTimeoutInMinutes,
        SLOT_STARTING_STATUS_CHECK_INTERVAL, startLogCallback, START_DEPLOYMENT_SLOT, statusVerifier);
    startLogCallback.saveExecutionLog("Deployment slot started successfully", INFO, SUCCESS);
  }

  private void stopSlotAsyncWithSteadyCheck(DeploymentSlot deploymentSlot,
      ILogStreamingTaskClient logStreamingTaskClient, long slotStartingSteadyStateTimeoutInMinutes) {
    LogCallback stopLogCallback = logStreamingTaskClient.obtainLogCallback(STOP_DEPLOYMENT_SLOT);
    String slotName = deploymentSlot.name();
    DefaultCompletableSubscriber defaultSubscriber = new DefaultCompletableSubscriber();

    stopLogCallback.saveExecutionLog(format("Sending request for stopping deployment slot - [%s]", slotName));
    deploymentSlot.stopAsync().subscribe(defaultSubscriber);
    stopLogCallback.saveExecutionLog(SUCCESS_REQUEST);

    Supplier<Void> getSlotStatus = () -> {
      String slotState = deploymentSlot.state();
      stopLogCallback.saveExecutionLog(format("Current state for deployment slot is - [%s]", slotState));
      return null;
    };

    azureTimeLimiter.waitUntilCompleteWithTimeout(slotStartingSteadyStateTimeoutInMinutes,
        SLOT_STOPPING_STATUS_CHECK_INTERVAL, defaultSubscriber, getSlotStatus, stopLogCallback, STOP_DEPLOYMENT_SLOT);
    stopLogCallback.saveExecutionLog("Deployment slot stopped successfully", INFO, SUCCESS);
  }

  private void stopSlotAsyncWithSteadyCheck(DeploymentSlot deploymentSlot,
      ILogStreamingTaskClient logStreamingTaskClient, long slotStartingSteadyStateTimeoutInMinutes,
      AzureAppServiceDockerDeploymentContext deploymentContext) {
    LogCallback stopLogCallback = logStreamingTaskClient.obtainLogCallback(STOP_DEPLOYMENT_SLOT);
    String slotName = deploymentSlot.name();

    stopLogCallback.saveExecutionLog(format("Sending request for stopping deployment slot - [%s]", slotName));
    AzureServiceCallBack restCallBack = new AzureServiceCallBack(stopLogCallback, STOP_DEPLOYMENT_SLOT);
    azureWebClient.stopDeploymentSlotAsync(deploymentContext.getAzureWebClientContext(), slotName, restCallBack);
    stopLogCallback.saveExecutionLog(SUCCESS_REQUEST);

    SlotStatusVerifier statusVerifier = SlotStatusVerifier.getStatusVerifier(STOP_VERIFIER, stopLogCallback, slotName,
        azureWebClient, null, deploymentContext.getAzureWebClientContext(), restCallBack);

    slotSteadyStateChecker.waitUntilCompleteWithTimeout(slotStartingSteadyStateTimeoutInMinutes,
        SLOT_STOPPING_STATUS_CHECK_INTERVAL, stopLogCallback, STOP_DEPLOYMENT_SLOT, statusVerifier);
    stopLogCallback.saveExecutionLog("Deployment slot stopped successfully", INFO, SUCCESS);
  }

  @NotNull
  private Supplier<String> getSlotStatusSupplier(
      AzureAppServiceDockerDeploymentContext deploymentContext, LogCallback logCallback, String slotName) {
    return () -> {
      DeploymentSlot slot = getDeploymentSlot(deploymentContext.getAzureWebClientContext(), slotName);
      String slotState = slot.state();
      logCallback.saveExecutionLog(format("Current state for deployment slot is - [%s]", slotState));
      return slotState;
    };
  }

  public AzureAppServicePreDeploymentData getAzureAppServicePreDeploymentData(
      AzureWebClientContext azureWebClientContext, final String slotName,
      Map<String, AzureAppServiceApplicationSetting> userAddedAppSettings,
      Map<String, AzureAppServiceConnectionString> userAddedConnStrings,
      ILogStreamingTaskClient logStreamingTaskClient) {
    LogCallback logCallback = logStreamingTaskClient.obtainLogCallback(SAVE_EXISTING_CONFIGURATIONS);
    logCallback.saveExecutionLog(String.format("Saving existing configurations for slot - [%s] of App Service - [%s]",
        slotName, azureWebClientContext.getAppName()));

    AzureAppServicePreDeploymentDataBuilder preDeploymentDataBuilder = AzureAppServicePreDeploymentData.builder();

    try {
      saveApplicationSettings(
          azureWebClientContext, slotName, userAddedAppSettings, preDeploymentDataBuilder, logCallback);
      saveConnectionStrings(
          azureWebClientContext, slotName, userAddedConnStrings, preDeploymentDataBuilder, logCallback);
      saveDockerSettings(azureWebClientContext, slotName, preDeploymentDataBuilder, logCallback);
      saveTrafficWeight(azureWebClientContext, slotName, preDeploymentDataBuilder, logCallback);
      logCallback.saveExecutionLog(
          String.format("All configurations saved successfully for slot - [%s] of App Service - [%s]", slotName,
              azureWebClientContext.getAppName()),
          INFO, SUCCESS);
      return preDeploymentDataBuilder.slotName(slotName)
          .appName(azureWebClientContext.getAppName())
          .failedTaskType(AzureAppServiceTaskParameters.AzureAppServiceTaskType.SLOT_SWAP)
          .build();
    } catch (Exception exception) {
      logCallback.saveExecutionLog(
          String.format("Failed to save the deployment slot existing configurations - [%s]", exception.getMessage()),
          ERROR, FAILURE);
      throw exception;
    }
  }

  private void saveApplicationSettings(AzureWebClientContext azureWebClientContext, String slotName,
      Map<String, AzureAppServiceApplicationSetting> userAddedAppSettings,
      AzureAppServicePreDeploymentDataBuilder preDeploymentDataBuilder, LogCallback logCallback) {
    Map<String, AzureAppServiceApplicationSetting> existingAppSettingsOnSlot =
        azureWebClient.listDeploymentSlotAppSettings(azureWebClientContext, slotName);

    Map<String, AzureAppServiceApplicationSetting> appSettingsNeedBeDeletedInRollback =
        getAppSettingsNeedBeDeletedInRollback(userAddedAppSettings, existingAppSettingsOnSlot);

    Map<String, AzureAppServiceApplicationSetting> appSettingsNeedBeUpdatedInRollback =
        getAppSettingsNeedToBeUpdatedInRollback(userAddedAppSettings, existingAppSettingsOnSlot);

    preDeploymentDataBuilder
        .appSettingsToRemove(AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettingDTOs(
            appSettingsNeedBeDeletedInRollback, AZURE_SETTING))
        .appSettingsToAdd(AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettingDTOs(
            appSettingsNeedBeUpdatedInRollback, AZURE_SETTING));

    logCallback.saveExecutionLog(String.format("Saved existing Application settings for slot - [%s]", slotName));
  }

  private void saveConnectionStrings(AzureWebClientContext azureWebClientContext, String slotName,
      Map<String, AzureAppServiceConnectionString> userAddedConnStrings,
      AzureAppServicePreDeploymentDataBuilder preDeploymentDataBuilder, LogCallback logCallback) {
    Map<String, AzureAppServiceConnectionString> existingConnSettingsOnSlot =
        azureWebClient.listDeploymentSlotConnectionStrings(azureWebClientContext, slotName);

    Map<String, AzureAppServiceConnectionString> connSettingsNeedBeDeletedInRollback =
        getConnSettingsNeedBeDeletedInRollback(userAddedConnStrings, existingConnSettingsOnSlot);

    Map<String, AzureAppServiceConnectionString> connSettingsNeedBeUpdatedInRollback =
        getConnSettingsNeedBeUpdatedInRollback(userAddedConnStrings, existingConnSettingsOnSlot);

    preDeploymentDataBuilder
        .connStringsToRemove(AzureAppServiceConfigurationDTOMapper.getAzureAppServiceConnStringDTOs(
            connSettingsNeedBeDeletedInRollback, AZURE_SETTING))
        .connStringsToAdd(AzureAppServiceConfigurationDTOMapper.getAzureAppServiceConnStringDTOs(
            connSettingsNeedBeUpdatedInRollback, AZURE_SETTING));
    logCallback.saveExecutionLog(String.format("Saved existing Connection strings for slot - [%s]", slotName));
  }

  private void saveDockerSettings(AzureWebClientContext azureWebClientContext, String slotName,
      AzureAppServicePreDeploymentDataBuilder preDeploymentDataBuilder, LogCallback logCallback) {
    Map<String, AzureAppServiceDockerSetting> dockerSettingsNeedBeUpdatedInRollback =
        azureWebClient.listDeploymentSlotDockerSettings(azureWebClientContext, slotName);
    String dockerImageNameAndTag =
        azureWebClient.getSlotDockerImageNameAndTag(azureWebClientContext, slotName).orElse(EMPTY);
    preDeploymentDataBuilder
        .dockerSettingsToAdd(AzureAppServiceConfigurationDTOMapper.getAzureAppServiceDockerSettingDTOs(
            dockerSettingsNeedBeUpdatedInRollback, AZURE_SETTING))
        .imageNameAndTag(dockerImageNameAndTag);
    logCallback.saveExecutionLog(String.format("Saved existing Container settings for slot - [%s]", slotName));
  }

  private void saveTrafficWeight(AzureWebClientContext azureWebClientContext, String slotName,
      AzureAppServicePreDeploymentDataBuilder preDeploymentDataBuilder, LogCallback logCallback) {
    double slotTrafficWeight = azureWebClient.getDeploymentSlotTrafficWeight(azureWebClientContext, slotName);
    logCallback.saveExecutionLog(String.format("Saved existing Traffic percentage for slot - [%s]", slotName));
    preDeploymentDataBuilder.trafficWeight(slotTrafficWeight);
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

    rerouteTrafficLogCallback.saveExecutionLog(
        format("Sending request to shift [%.2f] traffic to deployment slot: [%s]", trafficWeightInPercentage,
            shiftTrafficSlotName));
    azureWebClient.rerouteProductionSlotTraffic(webClientContext, shiftTrafficSlotName, trafficWeightInPercentage);
    rerouteTrafficLogCallback.saveExecutionLog("Traffic percentage updated successfully", INFO, SUCCESS);
  }

  public void swapSlotsUsingCallback(AzureAppServiceDeploymentContext azureAppServiceDeploymentContext,
      String targetSlotName, ILogStreamingTaskClient logStreamingTaskClient) {
    String sourceSlotName = azureAppServiceDeploymentContext.getSlotName();
    int steadyStateTimeoutInMinutes = azureAppServiceDeploymentContext.getSteadyStateTimeoutInMin();
    AzureWebClientContext webClientContext = azureAppServiceDeploymentContext.getAzureWebClientContext();
    LogCallback slotSwapLogCallback = logStreamingTaskClient.obtainLogCallback(SLOT_SWAP);
    AzureServiceCallBack restCallBack = new AzureServiceCallBack(slotSwapLogCallback, SLOT_SWAP);

    SlotStatusVerifier statusVerifier =
        SlotStatusVerifier.getStatusVerifier(SWAP_VERIFIER, slotSwapLogCallback, sourceSlotName, azureWebClient,
            azureMonitorClient, azureAppServiceDeploymentContext.getAzureWebClientContext(), restCallBack);

    ExecutorService executorService = Executors.newFixedThreadPool(1);
    executorService.submit(new SlotSwapper(
        sourceSlotName, targetSlotName, azureWebClient, webClientContext, restCallBack, slotSwapLogCallback));
    executorService.shutdown();

    slotSteadyStateChecker.waitUntilCompleteWithTimeout(steadyStateTimeoutInMinutes,
        SLOT_STOPPING_STATUS_CHECK_INTERVAL, slotSwapLogCallback, SLOT_SWAP, statusVerifier);
    slotSwapLogCallback.saveExecutionLog("Swapping slots done successfully", INFO, SUCCESS);
  }

  public void swapSlots(AzureAppServiceDeploymentContext azureAppServiceDeploymentContext, String targetSlotName,
      ILogStreamingTaskClient logStreamingTaskClient) {
    String sourceSlotName = azureAppServiceDeploymentContext.getSlotName();
    int steadyStateTimeoutInMinutes = azureAppServiceDeploymentContext.getSteadyStateTimeoutInMin();
    AzureWebClientContext webClientContext = azureAppServiceDeploymentContext.getAzureWebClientContext();

    LogCallback slotSwapLogCallback = logStreamingTaskClient.obtainLogCallback(SLOT_SWAP);
    DefaultCompletableSubscriber defaultSubscriber = new DefaultCompletableSubscriber();

    slotSwapLogCallback.saveExecutionLog(format(
        "Sending request for swapping source slot: [%s] with target slot: [%s]", sourceSlotName, targetSlotName));
    AtomicReference<DateTime> startTime = new AtomicReference<>(DateTime.now());
    azureWebClient.swapDeploymentSlotsAsync(webClientContext, sourceSlotName, targetSlotName)
        .subscribe(defaultSubscriber);
    slotSwapLogCallback.saveExecutionLog(SUCCESS_REQUEST);

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
