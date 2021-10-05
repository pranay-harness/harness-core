package software.wings.delegatetasks.azure.appservice.deployment;

import static io.harness.azure.model.AzureConstants.SLOT_STARTING_STATUS_CHECK_INTERVAL;
import static io.harness.azure.model.AzureConstants.SLOT_STOPPING_STATUS_CHECK_INTERVAL;
import static io.harness.azure.model.AzureConstants.SLOT_SWAP;
import static io.harness.azure.model.AzureConstants.SLOT_TRAFFIC_PERCENTAGE;
import static io.harness.azure.model.AzureConstants.START_DEPLOYMENT_SLOT;
import static io.harness.azure.model.AzureConstants.STOP_DEPLOYMENT_SLOT;
import static io.harness.azure.model.AzureConstants.SUCCESS_REQUEST;
import static io.harness.azure.model.AzureConstants.UPDATE_DEPLOYMENT_SLOT_CONFIGURATION_SETTINGS;
import static io.harness.azure.model.AzureConstants.UPDATE_DEPLOYMENT_SLOT_CONTAINER_SETTINGS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.delegatetasks.azure.appservice.deployment.SlotStatusVerifier.SlotStatusVerifierType.START_VERIFIER;
import static software.wings.delegatetasks.azure.appservice.deployment.SlotStatusVerifier.SlotStatusVerifierType.STOP_VERIFIER;
import static software.wings.delegatetasks.azure.appservice.deployment.SlotStatusVerifier.SlotStatusVerifierType.SWAP_VERIFIER;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.client.AzureMonitorClient;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.WebAppHostingOS;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.azure.appservice.AzureAppServicePreDeploymentData;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import software.wings.delegatetasks.azure.AzureServiceCallBack;
import software.wings.delegatetasks.azure.AzureTimeLimiter;
import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServiceDeploymentContext;
import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServiceDockerDeploymentContext;
import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServicePackageDeploymentContext;
import software.wings.delegatetasks.azure.appservice.webapp.AppServiceDeploymentProgress;
import software.wings.delegatetasks.azure.common.validator.Validators;
import software.wings.utils.ArtifactType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rx.Completable;

@Singleton
@NoArgsConstructor
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class AzureAppServiceDeploymentService {
  @Inject private AzureWebClient azureWebClient;
  @Inject private AzureTimeLimiter azureTimeLimiter;
  @Inject private SlotSteadyStateChecker slotSteadyStateChecker;
  @Inject private AzureMonitorClient azureMonitorClient;

  public void deployDockerImage(
      AzureAppServiceDockerDeploymentContext deploymentContext, AzureAppServicePreDeploymentData preDeploymentData) {
    Validators.validateJsr380FailFast(deploymentContext, InvalidRequestException::new);
    Validators.validateJsr380FailFast(deploymentContext.getAzureWebClientContext(), InvalidRequestException::new);
    log.info("Start deploying docker image: {} on slot: {}", deploymentContext.getImagePathAndTag(),
        deploymentContext.getSlotName());

    stopSlotAsyncWithSteadyCheck(deploymentContext, preDeploymentData);
    updateDeploymentSlotConfigurationSettings(deploymentContext, preDeploymentData);
    updateDeploymentSlotContainerSettings(deploymentContext, preDeploymentData);
    startSlotAsyncWithSteadyCheck(deploymentContext, preDeploymentData);
  }

  public void deployPackage(
      AzureAppServicePackageDeploymentContext deploymentContext, AzureAppServicePreDeploymentData preDeploymentData) {
    Validators.validateJsr380FailFast(deploymentContext, InvalidRequestException::new);
    Validators.validateJsr380FailFast(deploymentContext.getAzureWebClientContext(), InvalidRequestException::new);
    log.info("Start deploying artifact file on slot, slotName: {}, artifactFileAbsolutePath: {}",
        deploymentContext.getSlotName(), deploymentContext.getArtifactFile().getAbsolutePath());

    stopSlotAsyncWithSteadyCheck(deploymentContext, preDeploymentData);
    updateDeploymentSlotConfigurationSettings(deploymentContext, preDeploymentData);
    deployArtifactFile(deploymentContext);
    startSlotAsyncWithSteadyCheck(deploymentContext, preDeploymentData);
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

  public void startSlotAsyncWithSteadyCheck(
      AzureAppServiceDeploymentContext deploymentContext, AzureAppServicePreDeploymentData preDeploymentData) {
    ILogStreamingTaskClient logStreamingTaskClient = deploymentContext.getLogStreamingTaskClient();
    long slotStartingSteadyStateTimeoutInMinutes = deploymentContext.getSteadyStateTimeoutInMin();
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

  public void stopSlotAsyncWithSteadyCheck(
      AzureAppServiceDeploymentContext deploymentContext, AzureAppServicePreDeploymentData preDeploymentData) {
    ILogStreamingTaskClient logStreamingTaskClient = deploymentContext.getLogStreamingTaskClient();
    long slotStartingSteadyStateTimeoutInMinutes = deploymentContext.getSteadyStateTimeoutInMin();
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

  private void deployArtifactFile(AzureAppServicePackageDeploymentContext context) {
    String slotName = context.getSlotName();
    ILogStreamingTaskClient logStreamingTaskClient = context.getLogStreamingTaskClient();
    LogCallback logCallback = logStreamingTaskClient.obtainLogCallback("DEPLOY_ARTIFACT_FILE");

    try {
      logCallback.saveExecutionLog("Start deploying artifact file");
      uploadStartupScript(context.getAzureWebClientContext(), slotName, context.getStartupCommand(), logCallback);
      Completable deployment = deployPackage(context.getAzureWebClientContext(), slotName, context.getArtifactFile(),
          context.getArtifactType(), logCallback);
      deployment.await(context.getSteadyStateTimeoutInMin(), TimeUnit.MINUTES);
      logCallback.saveExecutionLog("Deployment done", LogLevel.INFO, SUCCESS);
    } catch (Exception ex) {
      logCallback.saveExecutionLog("Failed to deploy artifact file", LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      throw ex;
    }
  }

  private void uploadStartupScript(
      AzureWebClientContext context, final String slotName, final String startupCommand, LogCallback logCallback) {
    logCallback.saveExecutionLog(
        format("Start updating slot configuration with startup command, %nApp name: %s%nSlot name: %s",
            context.getAppName(), slotName));
    azureWebClient.updateSlotConfigurationWithAppCommandLineScript(context, slotName, startupCommand);
    logCallback.saveExecutionLog("Slot configuration updated successfully");
  }

  private Completable deployPackage(AzureWebClientContext azureWebClientContext, final String slotName,
      final File artifactFile, ArtifactType artifactType, LogCallback logCallback) {
    if (ArtifactType.ZIP == artifactType || ArtifactType.NUGET == artifactType) {
      return deployZipToSlotAndLog(azureWebClientContext, slotName, artifactFile, logCallback);
    } else if (ArtifactType.WAR == artifactType) {
      return deployWarToSlotAndLog(azureWebClientContext, slotName, artifactFile, logCallback);
    } else {
      throw new InvalidRequestException(format(
          "Unsupported package deployment for artifact type, artifactType: %s, slotName: %s", artifactType, slotName));
    }
  }

  private Completable deployZipToSlotAndLog(
      AzureWebClientContext azureWebClientContext, String slotName, File artifactFile, LogCallback logCallback) {
    logCallback.saveExecutionLog(
        format("Deploying artifact ZIP file on slot, %nArtifact file: %s%nApp name: %s%nSlot name: %s",
            artifactFile.getAbsolutePath(), azureWebClientContext.getAppName(), slotName));
    Completable deployment = azureWebClient.deployZipToSlotAsync(azureWebClientContext, slotName, artifactFile);
    logCallback.saveExecutionLog("Deployment started. This operation can take a while to complete ...");

    return deployment;
  }

  private Completable deployWarToSlotAndLog(
      AzureWebClientContext azureWebClientContext, String slotName, File artifactFile, LogCallback logCallback) {
    logCallback.saveExecutionLog(
        format("Deploying artifact WAR file on slot, %nArtifact file: %s%nApp name: %s%nSlot name: %s",
            artifactFile.getAbsolutePath(), azureWebClientContext.getAppName(), slotName));
    Completable deployment = azureWebClient.deployWarToSlotAsync(azureWebClientContext, slotName, artifactFile);
    logCallback.saveExecutionLog("Deployment started. This operation can take a while to complete ...");

    return deployment;
  }
}
