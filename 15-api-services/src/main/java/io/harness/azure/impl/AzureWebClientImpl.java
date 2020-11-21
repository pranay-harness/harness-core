package io.harness.azure.impl;

import static io.harness.azure.model.AzureAppServiceConnectionStringType.fromValue;
import static io.harness.azure.model.AzureConstants.DOCKER_CUSTOM_IMAGE_NAME_PROPERTY_NAME;
import static io.harness.azure.model.AzureConstants.DOCKER_REGISTRY_SERVER_SECRET_PROPERTY_NAME;
import static io.harness.azure.model.AzureConstants.DOCKER_REGISTRY_SERVER_URL_PROPERTY_NAME;
import static io.harness.azure.model.AzureConstants.DOCKER_REGISTRY_SERVER_USERNAME_PROPERTY_NAME;
import static io.harness.azure.model.AzureConstants.SLOT_NAME_BLANK_VALIDATION_MSG;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.azure.AzureClient;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureClientContext;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureAppServiceDockerSetting;
import io.harness.azure.model.WebAppHostingOS;
import io.harness.azure.utility.AzureResourceUtility;

import com.google.inject.Singleton;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.ConnectionString;
import com.microsoft.azure.management.appservice.ConnectionStringType;
import com.microsoft.azure.management.appservice.CsmSlotEntity;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.Experiments;
import com.microsoft.azure.management.appservice.RampUpRule;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteConfigResourceInner;
import com.microsoft.azure.management.appservice.implementation.StringDictionaryInner;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import rx.Completable;
import rx.Observable;

@Singleton
@Slf4j
public class AzureWebClientImpl extends AzureClient implements AzureWebClient {
  @Override
  public List<WebApp> listWebAppsByResourceGroupName(AzureClientContext context) {
    String subscriptionId = context.getSubscriptionId();
    String resourceGroupName = context.getResourceGroupName();
    Azure azure = getAzureClientByContext(context);

    log.debug("Start getting Web Applications by subscriptionId: {}, resourceGroupName: {}", subscriptionId,
        resourceGroupName);
    Instant startListingWebApps = Instant.now();
    PagedList<WebApp> webApps = azure.webApps().listByResourceGroup(resourceGroupName);

    List<WebApp> webAppsList = new ArrayList<>(webApps);
    long elapsedTime = Duration.between(startListingWebApps, Instant.now()).toMillis();
    log.info("Obtained Web Applications items: {} for elapsed time: {}, resourceGroupName: {}, subscriptionId: {} ",
        webAppsList.size(), elapsedTime, resourceGroupName, subscriptionId);

    return webAppsList;
  }

  @Override
  public List<DeploymentSlot> listDeploymentSlotsByWebAppName(AzureWebClientContext context) {
    String subscriptionId = context.getSubscriptionId();
    String resourceGroupName = context.getResourceGroupName();
    String webAppName = context.getAppName();
    Azure azure = getAzureClientByContext(context);

    log.debug("Start listing deployment slots by subscriptionId: {}, resourceGroupName: {}, webAppName: {}",
        subscriptionId, resourceGroupName, webAppName);
    PagedList<DeploymentSlot> deploymentSlots =
        azure.webApps().getByResourceGroup(resourceGroupName, webAppName).deploymentSlots().list();

    return new ArrayList<>(deploymentSlots);
  }

  @Override
  public Optional<WebApp> getWebAppByName(AzureWebClientContext context) {
    String webAppName = context.getAppName();
    Azure azure = getAzureClientByContext(context);
    log.debug("Start getting web app by webAppName: {}, context: {}", webAppName, context);
    return Optional.ofNullable(azure.webApps().getByResourceGroup(context.getResourceGroupName(), webAppName));
  }

  @Override
  public Optional<DeploymentSlot> getDeploymentSlotByName(AzureWebClientContext context, final String slotName) {
    String resourceGroupName = context.getResourceGroupName();
    String webAppName = context.getAppName();
    Azure azure = getAzureClientByContext(context);
    if (isBlank(slotName)) {
      throw new IllegalArgumentException(SLOT_NAME_BLANK_VALIDATION_MSG);
    }

    log.debug(
        "Start getting deployment slot by slotName: {} webAppName: {}, context: {}", slotName, webAppName, context);
    try {
      return Optional.ofNullable(
          azure.webApps().getByResourceGroup(resourceGroupName, webAppName).deploymentSlots().getByName(slotName));
    } catch (NoSuchElementException e) {
      log.warn(format("Unable to find deployment slot with name: %s, for app name: %s", slotName, webAppName), e);
      return Optional.empty();
    }
  }

  @Override
  public void startDeploymentSlot(AzureWebClientContext context, final String slotName) {
    DeploymentSlot deploymentSlot = getDeploymentSlot(context, slotName);
    log.debug("Starting slot with name: {}, context: {}", slotName, context);
    deploymentSlot.start();
  }

  @Override
  public Completable startDeploymentSlotAsync(AzureWebClientContext context, final String slotName) {
    DeploymentSlot deploymentSlot = getDeploymentSlot(context, slotName);
    log.debug("Starting async slot with name: {}, context: {}", slotName, context);
    return deploymentSlot.startAsync();
  }

  @Override
  public void startDeploymentSlot(DeploymentSlot slot) {
    log.debug("Starting slot with name: {}", slot.name());
    slot.start();
  }

  @Override
  public Completable startDeploymentSlotAsync(DeploymentSlot slot) {
    log.debug("Starting async slot with name: {}", slot.name());
    return slot.startAsync();
  }

  @Override
  public void stopDeploymentSlot(AzureWebClientContext context, final String slotName) {
    DeploymentSlot deploymentSlot = getDeploymentSlot(context, slotName);
    log.debug("Stopping slot with name: {}, context: {}", slotName, context);
    deploymentSlot.stop();
  }

  @Override
  public Completable stopDeploymentSlotAsync(AzureWebClientContext context, final String slotName) {
    DeploymentSlot deploymentSlot = getDeploymentSlot(context, slotName);
    log.debug("Stopping async slot with name: {}, context: {}", slotName, context);
    return deploymentSlot.stopAsync();
  }

  @Override
  public void stopDeploymentSlot(DeploymentSlot slot) {
    log.debug("Stopping slot with name: {}", slot.name());
    slot.stop();
  }

  @Override
  public Completable stopDeploymentSlotAsync(DeploymentSlot slot) {
    log.debug("Stopping async slot with name: {}", slot.name());
    return slot.stopAsync();
  }

  @Override
  public String getSlotState(AzureWebClientContext context, final String slotName) {
    DeploymentSlot deploymentSlot = getDeploymentSlot(context, slotName);
    log.debug("Start getting slot with slotName: {}, context: {}", slotName, context);
    return deploymentSlot.state();
  }

  @Override
  public void updateDeploymentSlotAppSettings(AzureWebClientContext context, final String slotName,
      Map<String, AzureAppServiceApplicationSetting> appSettings) {
    if (appSettings.isEmpty()) {
      log.info("Slot app settings list is empty, slotName: {}, context: {}", slotName, context);
      return;
    }
    DeploymentSlot deploymentSlot = getDeploymentSlot(context, slotName);

    log.debug("Start updating slot app settings by slotName: {}, context: {}", slotName, context);
    appSettings.values().forEach(appSetting
        -> deploymentSlot.update()
               .withAppSetting(appSetting.getName(), appSetting.getValue())
               .withAppSettingStickiness(appSetting.getName(), appSetting.isSticky())
               .apply());
  }

  @Override
  public Map<String, AzureAppServiceApplicationSetting> listDeploymentSlotAppSettings(
      AzureWebClientContext context, final String slotName) {
    log.debug("Start listing slot app settings by slotName: {}, context: {}", slotName, context);
    DeploymentSlot deploymentSlot = getDeploymentSlot(context, slotName);
    Map<String, AppSetting> appSettings = deploymentSlot.getAppSettings();

    return appSettings.values().stream().collect(
        Collectors.toMap(AppSetting::key, this::buildAzureAppServiceApplicationSettings));
  }

  public AzureAppServiceApplicationSetting buildAzureAppServiceApplicationSettings(AppSetting appSetting) {
    return AzureAppServiceApplicationSetting.builder()
        .name(appSetting.key())
        .value(appSetting.value())
        .sticky(appSetting.sticky())
        .build();
  }

  @Override
  public void updateDeploymentSlotConnectionSettings(AzureWebClientContext context, final String slotName,
      Map<String, AzureAppServiceConnectionString> connectionSettings) {
    if (connectionSettings.isEmpty()) {
      log.info("Slot connection settings list is empty, slotName: {}, context: {}", slotName, context);
      return;
    }

    DeploymentSlot deploymentSlot = getDeploymentSlot(context, slotName);
    connectionSettings.values().forEach(connectionSetting
        -> deploymentSlot.update()
               .withConnectionString(connectionSetting.getName(), connectionSetting.getValue(),
                   ConnectionStringType.fromString(connectionSetting.getType().getValue()))
               .withConnectionStringStickiness(connectionSetting.getName(), connectionSetting.isSticky())
               .apply());
  }

  @Override
  public Map<String, AzureAppServiceConnectionString> listDeploymentSlotConnectionSettings(
      AzureWebClientContext context, final String slotName) {
    log.debug("Start listing slot connection settings by slotName: {}, context: {}", slotName, context);
    DeploymentSlot deploymentSlot = getDeploymentSlot(context, slotName);
    Map<String, ConnectionString> connSettings = deploymentSlot.getConnectionStrings();

    return connSettings.values().stream().collect(
        Collectors.toMap(ConnectionString::name, this::buildAzureAppServiceConnectionStrings));
  }

  public AzureAppServiceConnectionString buildAzureAppServiceConnectionStrings(ConnectionString connectionString) {
    return AzureAppServiceConnectionString.builder()
        .name(connectionString.name())
        .value(connectionString.value())
        .sticky(connectionString.sticky())
        .type(fromValue(connectionString.type().toString()))
        .build();
  }

  @Override
  public void updateDeploymentSlotDockerSettings(
      AzureWebClientContext context, final String slotName, Map<String, AzureAppServiceDockerSetting> dockerSettings) {
    if (dockerSettings.isEmpty()) {
      log.info("Docker settings list is empty, slotName: {}, context: {}", slotName, context);
      return;
    }
    validateDockerSettings(dockerSettings);

    DeploymentSlot deploymentSlot = getDeploymentSlot(context, slotName);

    log.debug("Start updating slot docker settings by slotName: {}, context: {}", slotName, context);
    dockerSettings.values().forEach(dockerSetting
        -> deploymentSlot.update()
               .withAppSetting(dockerSetting.getName(), dockerSetting.getValue())
               .withAppSettingStickiness(dockerSetting.getName(), dockerSetting.isSticky())
               .apply());
  }

  private void validateDockerSettings(Map<String, AzureAppServiceDockerSetting> dockerSettings) {
    dockerSettings.values().forEach(dockerSetting -> {
      String dockerSettingName = dockerSetting.getName();
      if (!AzureResourceUtility.DOCKER_REGISTRY_PROPERTY_NAMES.contains(dockerSettingName)) {
        throw new IllegalArgumentException(format("Not valid docker settings: %s", dockerSettingName));
      }
    });
  }

  @Override
  public Map<String, AzureAppServiceDockerSetting> listDeploymentSlotDockerSettings(
      AzureWebClientContext context, String slotName) {
    DeploymentSlot deploymentSlot = getDeploymentSlot(context, slotName);
    Map<String, AppSetting> appSettings = deploymentSlot.getAppSettings();

    return appSettings.values()
        .stream()
        .filter(appSetting -> AzureResourceUtility.DOCKER_REGISTRY_PROPERTY_NAMES.contains(appSetting.key()))
        .collect(Collectors.toMap(AppSetting::key, this::buildAzureAppServiceDockerSetting));
  }

  public AzureAppServiceDockerSetting buildAzureAppServiceDockerSetting(AppSetting appSetting) {
    return AzureAppServiceDockerSetting.builder()
        .name(appSetting.key())
        .value(appSetting.value())
        .sticky(appSetting.sticky())
        .build();
  }

  @Override
  public void deleteDeploymentSlotDockerSettings(AzureWebClientContext context, final String slotName) {
    String resourceGroupName = context.getResourceGroupName();
    String webAppName = context.getAppName();
    Azure azure = getAzureClientByContext(context);

    StringDictionaryInner siteConfigResourceInner =
        azure.webApps().inner().listApplicationSettingsSlot(resourceGroupName, webAppName, slotName);
    Map<String, String> existingSlotProperties = siteConfigResourceInner.properties();
    existingSlotProperties.remove(DOCKER_REGISTRY_SERVER_URL_PROPERTY_NAME);
    existingSlotProperties.remove(DOCKER_REGISTRY_SERVER_USERNAME_PROPERTY_NAME);
    existingSlotProperties.remove(DOCKER_REGISTRY_SERVER_SECRET_PROPERTY_NAME);
    existingSlotProperties.remove(DOCKER_CUSTOM_IMAGE_NAME_PROPERTY_NAME);
    siteConfigResourceInner.withProperties(existingSlotProperties);

    log.debug("Start deleting slot docker settings by slotName: {}, context: {}", slotName, context);
    azure.webApps().inner().updateApplicationSettingsSlot(
        resourceGroupName, webAppName, slotName, siteConfigResourceInner);
  }

  @Override
  public void updateDeploymentSlotDockerImageNameAndTagSettings(
      AzureWebClientContext context, final String slotName, final String imageNameAndTag, WebAppHostingOS hostingOS) {
    String resourceGroupName = context.getResourceGroupName();
    String webAppName = context.getAppName();
    Azure azure = getAzureClientByContext(context);

    final SiteConfigResourceInner siteConfigResourceInner =
        azure.webApps().inner().getConfigurationSlot(resourceGroupName, webAppName, slotName);

    String dockerImageAndTagPath = AzureResourceUtility.getDockerImageAndTagFullPath(imageNameAndTag);
    if (WebAppHostingOS.LINUX == hostingOS) {
      siteConfigResourceInner.withLinuxFxVersion(dockerImageAndTagPath);
    } else if (WebAppHostingOS.WINDOWS == hostingOS) {
      siteConfigResourceInner.withWindowsFxVersion(dockerImageAndTagPath);
    } else {
      throw new IllegalArgumentException(format("Unsupported app hosting OS type: %s", hostingOS));
    }

    log.debug(
        "Start updating slot docker image name and tag settings, slotName: {}, context: {}, dockerImageAndTagPath: {}",
        slotName, context, dockerImageAndTagPath);
    azure.webApps().inner().updateConfigurationSlot(resourceGroupName, webAppName, slotName, siteConfigResourceInner);
  }

  @Override
  public void deleteDeploymentSlotDockerImageNameAndTagSettings(AzureWebClientContext context, final String slotName) {
    String resourceGroupName = context.getResourceGroupName();
    String webAppName = context.getAppName();
    Azure azure = getAzureClientByContext(context);

    SiteConfigResourceInner siteConfigResourceInner =
        azure.webApps().inner().getConfigurationSlot(resourceGroupName, webAppName, slotName);
    siteConfigResourceInner.withLinuxFxVersion(EMPTY);
    siteConfigResourceInner.withWindowsFxVersion(EMPTY);

    log.debug("Start deleting slot docker image name and tag by slotName: {}, context: {}", slotName, context);
    azure.webApps().inner().updateConfigurationSlot(resourceGroupName, webAppName, slotName, siteConfigResourceInner);
  }

  @Override
  public void updateDeploymentSlotTraffic(
      AzureWebClientContext context, final String slotName, double trafficReroutePercentage) {
    String resourceGroupName = context.getResourceGroupName();
    String webAppName = context.getAppName();
    Azure azure = getAzureClientByContext(context);

    DeploymentSlot deploymentSlot = getDeploymentSlot(context, slotName);
    String defaultHostName = deploymentSlot.defaultHostName();

    List<RampUpRule> rampUpRules = new ArrayList<>();
    RampUpRule rampUpRule = new RampUpRule();
    rampUpRule.withActionHostName(defaultHostName);
    rampUpRule.withName(slotName);
    rampUpRule.withReroutePercentage(trafficReroutePercentage);
    rampUpRules.add(rampUpRule);
    Experiments experiments = new Experiments();
    experiments.withRampUpRules(rampUpRules);
    SiteConfigResourceInner siteConfig = azure.webApps().inner().getConfiguration(resourceGroupName, webAppName);
    siteConfig.withExperiments(experiments);

    log.debug("Start updating slot traffic by slotName: {}, context: {}, defaultHostName: {}", slotName, context,
        defaultHostName);
    azure.webApps().inner().updateConfiguration(resourceGroupName, webAppName, siteConfig);
  }

  @Override
  public void swapDeploymentSlotWithProduction(AzureWebClientContext context, final String sourceSlotName) {
    String resourceGroupName = context.getResourceGroupName();
    String webAppName = context.getAppName();
    Azure azure = getAzureClientByContext(context);
    CsmSlotEntity slotSwapEntity = getTargetCsmSlotEntity(sourceSlotName);

    log.debug("Start swapping slot with production, slotName: {}, context: {}", sourceSlotName, context);
    azure.webApps().inner().swapSlotWithProduction(resourceGroupName, webAppName, slotSwapEntity);
  }

  @Override
  public void swapDeploymentSlots(
      AzureWebClientContext context, final String sourceSlotName, final String targetSlotName) {
    String resourceGroupName = context.getResourceGroupName();
    String webAppName = context.getAppName();
    Azure azure = getAzureClientByContext(context);
    CsmSlotEntity targetSlotSwapEntity = getTargetCsmSlotEntity(targetSlotName);

    log.debug("Start swapping slot with production, slotName: {}, context: {}", sourceSlotName, context);
    azure.webApps().inner().swapSlotSlot(resourceGroupName, webAppName, sourceSlotName, targetSlotSwapEntity);
  }

  @Override
  public Observable<Void> swapDeploymentSlotWithProductionAsync(
      AzureWebClientContext context, final String sourceSlotName) {
    String resourceGroupName = context.getResourceGroupName();
    String webAppName = context.getAppName();
    Azure azure = getAzureClientByContext(context);
    CsmSlotEntity slotSwapEntity = getTargetCsmSlotEntity(sourceSlotName);

    log.debug("Start async swapping slot with production, slotName: {}, context: {}", sourceSlotName, context);
    return azure.webApps().inner().swapSlotWithProductionAsync(resourceGroupName, webAppName, slotSwapEntity);
  }

  @Override
  public Observable<Void> swapDeploymentSlotsAsync(
      AzureWebClientContext context, final String sourceSlotName, final String targetSlotName) {
    String resourceGroupName = context.getResourceGroupName();
    String webAppName = context.getAppName();
    Azure azure = getAzureClientByContext(context);
    CsmSlotEntity targetSlotSwapEntity = getTargetCsmSlotEntity(targetSlotName);

    log.debug("Start async swapping slot with production, slotName: {}, context: {}", sourceSlotName, context);
    return azure.webApps().inner().swapSlotSlotAsync(
        resourceGroupName, webAppName, sourceSlotName, targetSlotSwapEntity);
  }

  @NotNull
  public CsmSlotEntity getTargetCsmSlotEntity(String targetSlotName) {
    CsmSlotEntity targetSlotSwapEntity = new CsmSlotEntity();
    targetSlotSwapEntity.withPreserveVnet(true);
    targetSlotSwapEntity.withTargetSlot(targetSlotName);
    return targetSlotSwapEntity;
  }

  @NotNull
  private DeploymentSlot getDeploymentSlot(AzureWebClientContext context, final String slotName) {
    Optional<DeploymentSlot> deploymentSlotOp = getDeploymentSlotByName(context, slotName);
    if (!deploymentSlotOp.isPresent()) {
      throw new IllegalArgumentException(format(
          "Unable to get deployment slot by slot name: %s, app name: %s, subscription id: %s, resource group name: %s",
          slotName, context.getAppName(), context.getSubscriptionId(), context.getResourceGroupName()));
    }
    return deploymentSlotOp.get();
  }

  @Override
  public WebAppHostingOS getWebAppHostingOS(AzureWebClientContext context) {
    String resourceGroupName = context.getResourceGroupName();
    String webAppName = context.getAppName();
    Azure azure = getAzureClientByContext(context);
    SiteConfigResourceInner config = azure.webApps().inner().getConfiguration(resourceGroupName, webAppName);
    if (isBlank(config.windowsFxVersion()) && isBlank(config.linuxFxVersion())) {
      throw new IllegalArgumentException(
          format("There is no hosting operating system for subscriptionId: %s, resourceGroupName: %s,"
                  + " webAppName: %s",
              context.getSubscriptionId(), resourceGroupName, webAppName));
    }

    return isNotBlank(config.windowsFxVersion()) ? WebAppHostingOS.WINDOWS : WebAppHostingOS.LINUX;
  }
}
