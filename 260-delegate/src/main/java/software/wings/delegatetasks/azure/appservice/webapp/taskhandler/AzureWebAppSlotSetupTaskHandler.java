/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.azure.appservice.webapp.taskhandler;

import static io.harness.azure.model.AzureConstants.ACR_ACCESS_KEYS_BLANK_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.ACR_USERNAME_BLANK_VALIDATION_MSG;
import static io.harness.azure.model.AzureConstants.DOCKER_REGISTRY_SERVER_SECRET_PROPERTY_NAME;
import static io.harness.azure.model.AzureConstants.DOCKER_REGISTRY_SERVER_USERNAME_PROPERTY_NAME;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
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
import io.harness.delegate.task.azure.appservice.webapp.response.AzureAppDeploymentData;
import io.harness.delegate.task.azure.appservice.webapp.response.AzureWebAppSlotSetupResponse;
import io.harness.exception.InvalidArgumentsException;

import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServiceDockerDeploymentContext;
import software.wings.delegatetasks.azure.appservice.deployment.context.AzureAppServicePackageDeploymentContext;
import software.wings.delegatetasks.azure.appservice.webapp.AbstractAzureWebAppTaskHandler;

import com.google.inject.Singleton;
import com.microsoft.azure.management.containerregistry.AccessKeyType;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Singleton
@NoArgsConstructor
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class AzureWebAppSlotSetupTaskHandler extends AbstractAzureWebAppTaskHandler {
  @Override
  protected AzureAppServiceTaskResponse executeTaskInternal(AzureAppServiceTaskParameters azureAppServiceTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient,
      ArtifactStreamAttributes artifactStreamAttributes) {
    return artifactStreamAttributes == null
        ? executeDockerTask(azureAppServiceTaskParameters, azureConfig, logStreamingTaskClient)
        : executePackageTask(
            azureAppServiceTaskParameters, azureConfig, logStreamingTaskClient, artifactStreamAttributes);
  }

  private AzureAppServiceTaskResponse executeDockerTask(AzureAppServiceTaskParameters azureAppServiceTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    AzureWebAppSlotSetupParameters azureWebAppSlotSetupParameters =
        (AzureWebAppSlotSetupParameters) azureAppServiceTaskParameters;

    AzureWebClientContext azureWebClientContext =
        buildAzureWebClientContext(azureWebAppSlotSetupParameters, azureConfig);
    AzureAppServiceDockerDeploymentContext dockerDeploymentContext = toAzureAppServiceDockerDeploymentContext(
        azureWebAppSlotSetupParameters, azureWebClientContext, azureConfig, logStreamingTaskClient);
    AzureAppServicePreDeploymentData azureAppServicePreDeploymentData =
        azureAppServiceDeploymentService
            .getDefaultPreDeploymentDataBuilder(
                azureWebAppSlotSetupParameters.getAppName(), azureWebAppSlotSetupParameters.getSlotName())
            .build();
    try {
      azureAppServicePreDeploymentData = getAzureAppServicePreDeploymentData(dockerDeploymentContext);
      azureAppServiceDeploymentService.deployDockerImage(dockerDeploymentContext, azureAppServicePreDeploymentData);
      List<AzureAppDeploymentData> azureAppDeploymentData = azureAppServiceDeploymentService.fetchDeploymentData(
          azureWebClientContext, azureWebAppSlotSetupParameters.getSlotName());
      markDeploymentStatusAsSuccess(azureAppServiceTaskParameters, logStreamingTaskClient);

      return AzureWebAppSlotSetupResponse.builder()
          .azureAppDeploymentData(azureAppDeploymentData)
          .preDeploymentData(azureAppServicePreDeploymentData)
          .build();
    } catch (Exception ex) {
      String message = AzureResourceUtility.getAzureCloudExceptionMessage(ex);
      logErrorMsg(azureAppServiceTaskParameters, logStreamingTaskClient, ex, message);
      return AzureWebAppSlotSetupResponse.builder()
          .errorMsg(message)
          .preDeploymentData(azureAppServicePreDeploymentData)
          .build();
    }
  }

  public AzureAppServiceTaskResponse executePackageTask(AzureAppServiceTaskParameters azureAppServiceTaskParameters,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient,
      ArtifactStreamAttributes streamAttributes) {
    AzureWebAppSlotSetupParameters azureWebAppSlotSetupParameters =
        (AzureWebAppSlotSetupParameters) azureAppServiceTaskParameters;

    AzureWebClientContext azureWebClientContext =
        buildAzureWebClientContext(azureWebAppSlotSetupParameters, azureConfig);
    AzureAppServicePackageDeploymentContext dockerDeploymentContext = toAzureAppServicePackageDeploymentContext(
        azureWebAppSlotSetupParameters, azureWebClientContext, streamAttributes, logStreamingTaskClient);

    try {
      markDeploymentStatusAsSuccess(azureAppServiceTaskParameters, logStreamingTaskClient);
      return AzureWebAppSlotSetupResponse.builder().azureAppDeploymentData(null).preDeploymentData(null).build();
    } catch (Exception ex) {
      String message = AzureResourceUtility.getAzureCloudExceptionMessage(ex);
      logErrorMsg(azureAppServiceTaskParameters, logStreamingTaskClient, ex, message);
      return AzureWebAppSlotSetupResponse.builder().errorMsg(message).preDeploymentData(null).build();
    }
  }

  private AzureAppServiceDockerDeploymentContext toAzureAppServiceDockerDeploymentContext(
      AzureWebAppSlotSetupParameters azureWebAppSlotSetupParameters, AzureWebClientContext azureWebClientContext,
      AzureConfig azureConfig, ILogStreamingTaskClient logStreamingTaskClient) {
    ConnectorConfigDTO connectorConfigDTO = azureWebAppSlotSetupParameters.getConnectorConfigDTO();
    AzureRegistryType azureRegistryType = azureWebAppSlotSetupParameters.getAzureRegistryType();

    List<AzureAppServiceApplicationSetting> applicationSettings =
        azureWebAppSlotSetupParameters.getApplicationSettings();
    Map<String, AzureAppServiceApplicationSetting> appSettingsToAdd = applicationSettings.stream().collect(
        Collectors.toMap(AzureAppServiceApplicationSetting::getName, Function.identity()));

    List<AzureAppServiceConnectionString> connectionStrings = azureWebAppSlotSetupParameters.getConnectionStrings();
    Map<String, AzureAppServiceConnectionString> connSettingsToAdd = connectionStrings.stream().collect(
        Collectors.toMap(AzureAppServiceConnectionString::getName, Function.identity()));

    Map<String, AzureAppServiceApplicationSetting> dockerSettings =
        getAzureAppServiceDockerSettings(connectorConfigDTO, azureRegistryType, azureConfig);

    String imagePathAndTag = AzureResourceUtility.getDockerImageFullNameAndTag(
        azureWebAppSlotSetupParameters.getImageName(), azureWebAppSlotSetupParameters.getImageTag());

    return AzureAppServiceDockerDeploymentContext.builder()
        .logStreamingTaskClient(logStreamingTaskClient)
        .appSettingsToAdd(appSettingsToAdd)
        .connSettingsToAdd(connSettingsToAdd)
        .dockerSettings(dockerSettings)
        .imagePathAndTag(imagePathAndTag)
        .slotName(azureWebAppSlotSetupParameters.getSlotName())
        .targetSlotName(azureWebAppSlotSetupParameters.getTargetSlotName())
        .azureWebClientContext(azureWebClientContext)
        .steadyStateTimeoutInMin(azureWebAppSlotSetupParameters.getTimeoutIntervalInMin())
        .build();
  }

  private Map<String, AzureAppServiceApplicationSetting> getAzureAppServiceDockerSettings(
      ConnectorConfigDTO connectorConfigDTO, AzureRegistryType azureRegistryType, AzureConfig azureConfig) {
    AzureRegistry azureRegistry = AzureRegistryFactory.getAzureRegistry(azureRegistryType);
    Map<String, AzureAppServiceApplicationSetting> dockerSettings =
        azureRegistry.getContainerSettings(connectorConfigDTO);

    if (AzureRegistryType.ACR == azureRegistryType) {
      RegistryCredentials registryCredentials = azureAppServiceDeploymentService.getContainerRegistryCredentials(
          azureConfig, (AzureContainerRegistryConnectorDTO) connectorConfigDTO);
      updateACRDockerSettingByCredentials(dockerSettings, registryCredentials);
    }

    return dockerSettings;
  }

  private void updateACRDockerSettingByCredentials(
      Map<String, AzureAppServiceApplicationSetting> dockerSettings, RegistryCredentials registryCredentials) {
    String username = getACRUsername(registryCredentials);
    String accessKey = getACRAccessKey(registryCredentials);

    dockerSettings.put(DOCKER_REGISTRY_SERVER_USERNAME_PROPERTY_NAME,
        AzureAppServiceApplicationSetting.builder()
            .name(DOCKER_REGISTRY_SERVER_USERNAME_PROPERTY_NAME)
            .sticky(false)
            .value(username)
            .build());

    dockerSettings.put(DOCKER_REGISTRY_SERVER_SECRET_PROPERTY_NAME,
        AzureAppServiceApplicationSetting.builder()
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

  private AzureAppServicePreDeploymentData getAzureAppServicePreDeploymentData(
      AzureAppServiceDockerDeploymentContext dockerDeploymentContext) {
    String slotName = dockerDeploymentContext.getSlotName();
    String targetSlotName = dockerDeploymentContext.getTargetSlotName();
    AzureWebClientContext azureWebClientContext = dockerDeploymentContext.getAzureWebClientContext();
    Map<String, AzureAppServiceApplicationSetting> userAddedAppSettings = dockerDeploymentContext.getAppSettingsToAdd();
    Map<String, AzureAppServiceConnectionString> userAddedConnSettings = dockerDeploymentContext.getConnSettingsToAdd();
    return azureAppServiceDeploymentService.getAzureAppServicePreDeploymentData(azureWebClientContext, slotName,
        targetSlotName, userAddedAppSettings, userAddedConnSettings,
        dockerDeploymentContext.getLogStreamingTaskClient());
  }

  private AzureAppServicePackageDeploymentContext toAzureAppServicePackageDeploymentContext(
      AzureWebAppSlotSetupParameters azureWebAppSlotSetupParameters, AzureWebClientContext azureWebClientContext,
      ArtifactStreamAttributes streamAttributes, ILogStreamingTaskClient logStreamingTaskClient) {
    List<AzureAppServiceApplicationSetting> applicationSettings =
        azureWebAppSlotSetupParameters.getApplicationSettings();
    Map<String, AzureAppServiceApplicationSetting> appSettingsToAdd = applicationSettings.stream().collect(
        Collectors.toMap(AzureAppServiceApplicationSetting::getName, Function.identity()));

    List<AzureAppServiceConnectionString> connectionStrings = azureWebAppSlotSetupParameters.getConnectionStrings();
    Map<String, AzureAppServiceConnectionString> connSettingsToAdd = connectionStrings.stream().collect(
        Collectors.toMap(AzureAppServiceConnectionString::getName, Function.identity()));

    return AzureAppServicePackageDeploymentContext.builder()
        .logStreamingTaskClient(logStreamingTaskClient)
        .appSettingsToAdd(appSettingsToAdd)
        .connSettingsToAdd(connSettingsToAdd)
        .slotName(azureWebAppSlotSetupParameters.getSlotName())
        .targetSlotName(azureWebAppSlotSetupParameters.getTargetSlotName())
        .azureWebClientContext(azureWebClientContext)
        .artifactStreamAttributes(streamAttributes)
        .startupCommand(azureWebAppSlotSetupParameters.getStartupCommand())
        .steadyStateTimeoutInMin(azureWebAppSlotSetupParameters.getTimeoutIntervalInMin())
        .build();
  }
}
