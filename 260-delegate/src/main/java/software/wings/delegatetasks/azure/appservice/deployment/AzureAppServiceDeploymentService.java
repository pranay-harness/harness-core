/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.azure.appservice.deployment;

import static io.harness.azure.model.AzureConstants.SAVE_EXISTING_CONFIGURATIONS;
import static io.harness.azure.model.AzureConstants.SLOT_NAME_BLANK_ERROR_MSG;
import static io.harness.azure.model.AzureConstants.SLOT_STARTING_STATUS_CHECK_INTERVAL;
import static io.harness.azure.model.AzureConstants.SLOT_STOPPING_STATUS_CHECK_INTERVAL;
import static io.harness.azure.model.AzureConstants.SLOT_SWAP;
import static io.harness.azure.model.AzureConstants.SLOT_TRAFFIC_PERCENTAGE;
import static io.harness.azure.model.AzureConstants.START_DEPLOYMENT_SLOT;
import static io.harness.azure.model.AzureConstants.STOP_DEPLOYMENT_SLOT;
import static io.harness.azure.model.AzureConstants.SUCCESS_REQUEST;
import static io.harness.azure.model.AzureConstants.TARGET_SLOT_CANNOT_BE_IN_STOPPED_STATE;
import static io.harness.azure.model.AzureConstants.UPDATE_DEPLOYMENT_SLOT_CONFIGURATION_SETTINGS;
import static io.harness.azure.model.AzureConstants.UPDATE_DEPLOYMENT_SLOT_CONTAINER_SETTINGS;
import static io.harness.azure.model.AzureConstants.WEB_APP_INSTANCE_STATUS_RUNNING;
import static io.harness.azure.model.AzureConstants.WEB_APP_NAME_BLANK_ERROR_MSG;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.delegatetasks.azure.appservice.deployment.SlotStatusVerifier.SlotStatus.STOPPED;
import static software.wings.delegatetasks.azure.appservice.deployment.SlotStatusVerifier.SlotStatusVerifierType.START_VERIFIER;
import static software.wings.delegatetasks.azure.appservice.deployment.SlotStatusVerifier.SlotStatusVerifierType.STOP_VERIFIER;
import static software.wings.delegatetasks.azure.appservice.deployment.SlotStatusVerifier.SlotStatusVerifierType.SWAP_VERIFIER;
import static software.wings.delegatetasks.azure.appservice.webapp.AppServiceDeploymentProgress.SAVE_CONFIGURATION;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.client.AzureContainerRegistryClient;
import io.harness.azure.client.AzureMonitorClient;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureContainerRegistryClientContext;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.AzureConstants;
import io.harness.azure.model.WebAppHostingOS;
import io.harness.delegate.beans.azure.mapper.AzureAppServiceConfigurationDTOMapper;
import io.harness.delegate.beans.connector.azureconnector.AzureContainerRegistryConnectorDTO;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData.AzureAppServicePreDeploymentDataBuilder;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureAppDeploymentData;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.LogCallback;

import software.wings.delegatetasks.azure.AzureServiceCallBack;
import software.wings.delegatetasks.azure.AzureTimeLimiter;
import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServiceDeploymentContext;
import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServiceDockerDeploymentContext;
import software.wings.delegatetasks.azure.appservice.webapp.AppServiceDeploymentProgress;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.implementation.SiteInstanceInner;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Singleton
@NoArgsConstructor
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class AzureAppServiceDeploymentService {
  @Inject private AzureWebClient azureWebClient;
  @Inject private AzureContainerRegistryClient azureContainerRegistryClient;
  @Inject private AzureTimeLimiter azureTimeLimiter;
  @Inject private SlotSteadyStateChecker slotSteadyStateChecker;
  @Inject private AzureMonitorClient azureMonitorClient;

  public void deployDockerImage(
      AzureAppServiceDockerDeploymentContext deploymentContext, AzureAppServicePreDeploymentData preDeploymentData) {
    validateContextForDockerDeployment(deploymentContext);
    log.info("Start deploying docker image: {} on slot: {}", deploymentContext.getImagePathAndTag(),
        deploymentContext.getSlotName());
    int steadyStateTimeoutInMin = deploymentContext.getSteadyStateTimeoutInMin();
    ILogStreamingTaskClient logStreamingTaskClient = deploymentContext.getLogStreamingTaskClient();

    stopSlotAsyncWithSteadyCheck(logStreamingTaskClient, steadyStateTimeoutInMin, deploymentContext, preDeploymentData);
    updateDeploymentSlotConfigurationSettings(deploymentContext, preDeploymentData);
    updateDeploymentSlotContainerSettings(deploymentContext, preDeploymentData);
    startSlotAsyncWithSteadyCheck(
        logStreamingTaskClient, steadyStateTimeoutInMin, deploymentContext, preDeploymentData);
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

  public void updateDeploymentSlotConfigurationSettings(
      AzureAppServiceDeploymentContext deploymentContext, AzureAppServicePreDeploymentData preDeploymentData) {
    AzureWebClientContext azureWebClientContext = deploymentContext.getAzureWebClientContext();
    Map<String, AzureAppServiceApplicationSetting> appSettingsToAdd = deploymentContext.getAppSettingsToAdd();
    Map<String, AzureAppServiceApplicationSetting> appSettingsToRemove = deploymentContext.getAppSettingsToRemove();
    Map<String, AzureAppServiceConnectionString> connSettingsToAdd = deploymentContext.getConnSettingsToAdd();
    Map<String, AzureAppServiceConnectionString> connSettingsToRemove = deploymentContext.getConnSettingsToRemove();
    String slotName = deploymentContext.getSlotName();
    ILogStreamingTaskClient logStreamingTaskClient = deploymentContext.getLogStreamingTaskClient();
    LogCallback configurationLogCallback =
        logStreamingTaskClient.obtainLogCallback(UPDATE_DEPLOYMENT_SLOT_CONFIGURATION_SETTINGS);
    preDeploymentData.setDeploymentProgressMarker(AppServiceDeploymentProgress.UPDATE_SLOT_CONFIGURATIONS.name());

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

  private void updateDeploymentSlotContainerSettings(
      AzureAppServiceDockerDeploymentContext deploymentContext, AzureAppServicePreDeploymentData preDeploymentData) {
    AzureWebClientContext azureWebClientContext = deploymentContext.getAzureWebClientContext();
    Map<String, AzureAppServiceApplicationSetting> dockerSettings = deploymentContext.getDockerSettings();
    String imageAndTag = deploymentContext.getImagePathAndTag();
    String slotName = deploymentContext.getSlotName();
    ILogStreamingTaskClient logStreamingTaskClient = deploymentContext.getLogStreamingTaskClient();
    LogCallback containerLogCallback =
        logStreamingTaskClient.obtainLogCallback(UPDATE_DEPLOYMENT_SLOT_CONTAINER_SETTINGS);
    preDeploymentData.setDeploymentProgressMarker(AppServiceDeploymentProgress.UPDATE_SLOT_CONTAINER_SETTINGS.name());

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
      Map<String, AzureAppServiceApplicationSetting> dockerSettings, LogCallback containerLogCallback) {
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

  public void startSlotAsyncWithSteadyCheck(ILogStreamingTaskClient logStreamingTaskClient,
      long slotStartingSteadyStateTimeoutInMinutes, AzureAppServiceDockerDeploymentContext deploymentContext,
      AzureAppServicePreDeploymentData preDeploymentData) {
    LogCallback startLogCallback = logStreamingTaskClient.obtainLogCallback(START_DEPLOYMENT_SLOT);
    preDeploymentData.setDeploymentProgressMarker(AppServiceDeploymentProgress.START_SLOT.name());
    String slotName = deploymentContext.getSlotName();
    startLogCallback.saveExecutionLog(format("Sending request for starting deployment slot - [%s]", slotName));
    try {
      AzureServiceCallBack restCallBack = new AzureServiceCallBack(startLogCallback, START_DEPLOYMENT_SLOT);
      azureWebClient.startDeploymentSlotAsync(deploymentContext.getAzureWebClientContext(), slotName, restCallBack);
      startLogCallback.saveExecutionLog(SUCCESS_REQUEST);

      SlotStatusVerifier statusVerifier = SlotStatusVerifier.getStatusVerifier(START_VERIFIER.name(), startLogCallback,
          slotName, azureWebClient, null, deploymentContext.getAzureWebClientContext(), restCallBack);
      slotSteadyStateChecker.waitUntilCompleteWithTimeout(slotStartingSteadyStateTimeoutInMinutes,
          SLOT_STARTING_STATUS_CHECK_INTERVAL, startLogCallback, START_DEPLOYMENT_SLOT, statusVerifier);
      startLogCallback.saveExecutionLog("Deployment slot started successfully", INFO, SUCCESS);
    } catch (Exception exception) {
      startLogCallback.saveExecutionLog(
          String.format("Failed to start deployment slot - [%s]", exception.getMessage()), ERROR, FAILURE);
      throw exception;
    }
  }

  public void stopSlotAsyncWithSteadyCheck(ILogStreamingTaskClient logStreamingTaskClient,
      long slotStartingSteadyStateTimeoutInMinutes, AzureAppServiceDockerDeploymentContext deploymentContext,
      AzureAppServicePreDeploymentData preDeploymentData) {
    LogCallback stopLogCallback = logStreamingTaskClient.obtainLogCallback(STOP_DEPLOYMENT_SLOT);
    preDeploymentData.setDeploymentProgressMarker(AppServiceDeploymentProgress.STOP_SLOT.name());
    String slotName = deploymentContext.getSlotName();
    stopLogCallback.saveExecutionLog(format("Sending request for stopping deployment slot - [%s]", slotName));

    try {
      AzureServiceCallBack restCallBack = new AzureServiceCallBack(stopLogCallback, STOP_DEPLOYMENT_SLOT);
      azureWebClient.stopDeploymentSlotAsync(deploymentContext.getAzureWebClientContext(), slotName, restCallBack);
      stopLogCallback.saveExecutionLog(SUCCESS_REQUEST);

      SlotStatusVerifier statusVerifier = SlotStatusVerifier.getStatusVerifier(STOP_VERIFIER.name(), stopLogCallback,
          slotName, azureWebClient, null, deploymentContext.getAzureWebClientContext(), restCallBack);
      slotSteadyStateChecker.waitUntilCompleteWithTimeout(slotStartingSteadyStateTimeoutInMinutes,
          SLOT_STOPPING_STATUS_CHECK_INTERVAL, stopLogCallback, STOP_DEPLOYMENT_SLOT, statusVerifier);
      stopLogCallback.saveExecutionLog("Deployment slot stopped successfully", INFO, SUCCESS);
    } catch (Exception exception) {
      stopLogCallback.saveExecutionLog(
          String.format("Failed to stop deployment slot - [%s]", exception.getMessage()), ERROR, FAILURE);
      throw exception;
    }
  }

  public AzureAppServicePreDeploymentData getAzureAppServicePreDeploymentData(
      AzureWebClientContext azureWebClientContext, final String slotName, String targetSlotName,
      Map<String, AzureAppServiceApplicationSetting> userAddedAppSettings,
      Map<String, AzureAppServiceConnectionString> userAddedConnStrings,
      ILogStreamingTaskClient logStreamingTaskClient) {
    LogCallback logCallback = logStreamingTaskClient.obtainLogCallback(SAVE_EXISTING_CONFIGURATIONS);
    logCallback.saveExecutionLog(String.format("Saving existing configurations for slot - [%s] of App Service - [%s]",
        slotName, azureWebClientContext.getAppName()));
    try {
      validateSlotStatus(azureWebClientContext, slotName, targetSlotName, logCallback);

      AzureAppServicePreDeploymentDataBuilder preDeploymentDataBuilder =
          getDefaultPreDeploymentDataBuilder(azureWebClientContext.getAppName(), slotName);
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
      return preDeploymentDataBuilder.build();
    } catch (Exception exception) {
      logCallback.saveExecutionLog(
          String.format("Failed to save the deployment slot existing configurations - %n[%s]", exception.getMessage()),
          ERROR, FAILURE);
      throw exception;
    }
  }

  private void validateSlotStatus(
      AzureWebClientContext azureWebClientContext, String slotName, String targetSlotName, LogCallback logCallback) {
    if (isBlank(slotName)) {
      throw new InvalidRequestException(SLOT_NAME_BLANK_ERROR_MSG);
    }
    String slotState = azureWebClient.getSlotState(azureWebClientContext, targetSlotName);
    if (STOPPED.name().equalsIgnoreCase(slotState)) {
      throw new InvalidRequestException(
          String.format("Pre validation failed. " + TARGET_SLOT_CANNOT_BE_IN_STOPPED_STATE, targetSlotName));
    }
    logCallback.saveExecutionLog("Pre validation was success");
  }

  public AzureAppServicePreDeploymentDataBuilder getDefaultPreDeploymentDataBuilder(String appName, String slotName) {
    return AzureAppServicePreDeploymentData.builder()
        .deploymentProgressMarker(SAVE_CONFIGURATION.name())
        .slotName(slotName)
        .appName(appName)
        .appSettingsToAdd(Collections.emptyMap())
        .appSettingsToRemove(Collections.emptyMap())
        .connStringsToAdd(Collections.emptyMap())
        .connStringsToRemove(Collections.emptyMap())
        .dockerSettingsToAdd(Collections.emptyMap());
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
        .appSettingsToRemove(
            AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettingDTOs(appSettingsNeedBeDeletedInRollback))
        .appSettingsToAdd(
            AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettingDTOs(appSettingsNeedBeUpdatedInRollback));

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
        .connStringsToRemove(
            AzureAppServiceConfigurationDTOMapper.getAzureAppServiceConnStringDTOs(connSettingsNeedBeDeletedInRollback))
        .connStringsToAdd(AzureAppServiceConfigurationDTOMapper.getAzureAppServiceConnStringDTOs(
            connSettingsNeedBeUpdatedInRollback));
    logCallback.saveExecutionLog(String.format("Saved existing Connection strings for slot - [%s]", slotName));
  }

  private void saveDockerSettings(AzureWebClientContext azureWebClientContext, String slotName,
      AzureAppServicePreDeploymentDataBuilder preDeploymentDataBuilder, LogCallback logCallback) {
    Map<String, AzureAppServiceApplicationSetting> dockerSettingsNeedBeUpdatedInRollback =
        azureWebClient.listDeploymentSlotDockerSettings(azureWebClientContext, slotName);
    String dockerImageNameAndTag =
        azureWebClient.getSlotDockerImageNameAndTag(azureWebClientContext, slotName).orElse(EMPTY);
    preDeploymentDataBuilder
        .dockerSettingsToAdd(AzureAppServiceConfigurationDTOMapper.getAzureAppServiceAppSettingDTOs(
            dockerSettingsNeedBeUpdatedInRollback))
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
    LogCallback rerouteTrafficLogCallback = logStreamingTaskClient.obtainLogCallback(SLOT_TRAFFIC_PERCENTAGE);

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
        SlotStatusVerifier.getStatusVerifier(SWAP_VERIFIER.name(), slotSwapLogCallback, sourceSlotName, azureWebClient,
            azureMonitorClient, azureAppServiceDeploymentContext.getAzureWebClientContext(), restCallBack);

    ExecutorService executorService = Executors.newFixedThreadPool(1);
    executorService.submit(new SlotSwapper(
        sourceSlotName, targetSlotName, azureWebClient, webClientContext, restCallBack, slotSwapLogCallback));
    executorService.shutdown();

    slotSteadyStateChecker.waitUntilCompleteWithTimeout(steadyStateTimeoutInMinutes,
        SLOT_STOPPING_STATUS_CHECK_INTERVAL, slotSwapLogCallback, SLOT_SWAP, statusVerifier);
    slotSwapLogCallback.saveExecutionLog("Swapping slots done successfully", INFO, SUCCESS);
  }
}
