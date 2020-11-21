package io.harness.azure.client;

import io.harness.azure.context.AzureClientContext;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureAppServiceApplicationSetting;
import io.harness.azure.model.AzureAppServiceConnectionString;
import io.harness.azure.model.AzureAppServiceDockerSetting;
import io.harness.azure.model.WebAppHostingOS;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import rx.Completable;
import rx.Observable;

public interface AzureWebClient {
  /**
   * List web application by resource group name.
   *
   * @param context
   * @return
   */
  List<WebApp> listWebAppsByResourceGroupName(AzureClientContext context);

  /**
   * List deployment slots by web application name.
   *
   * @param context
   * @return
   */
  List<DeploymentSlot> listDeploymentSlotsByWebAppName(AzureWebClientContext context);

  /**
   * Get Web Application by name.
   *
   * @param context
   * @return
   */
  Optional<WebApp> getWebAppByName(AzureWebClientContext context);

  /**
   * Get deployment slot by name.
   *
   * @param context
   * @param slotName
   * @return
   */
  Optional<DeploymentSlot> getDeploymentSlotByName(AzureWebClientContext context, String slotName);

  /**
   * Start deployment slot.
   *
   * @param context
   * @param slotName
   */
  void startDeploymentSlot(AzureWebClientContext context, String slotName);

  /**
   * Start deployment slot async.
   *
   * @param context
   * @param slotName
   * @return
   */
  Completable startDeploymentSlotAsync(AzureWebClientContext context, String slotName);

  /**
   * Start deployment slot.
   *
   * @param slot
   */
  void startDeploymentSlot(DeploymentSlot slot);

  /**
   * Start deployment slot async.
   *
   * @param slot
   * @return
   */
  Completable startDeploymentSlotAsync(DeploymentSlot slot);

  /**
   * Stop deployment slot.
   *
   * @param context
   * @param slotName
   */
  void stopDeploymentSlot(AzureWebClientContext context, String slotName);

  /**
   * Stop deployment slot async.
   *
   * @param context
   * @param slotName
   * @return
   */
  Completable stopDeploymentSlotAsync(AzureWebClientContext context, String slotName);

  /**
   * Stop deployment slot.
   *
   * @param slot
   */
  void stopDeploymentSlot(DeploymentSlot slot);

  /**
   * Stop deployment slot async.
   *
   * @param slot
   * @return
   */
  Completable stopDeploymentSlotAsync(DeploymentSlot slot);

  /**
   * Get deployment slot state.
   *
   * @param context
   * @param slotName
   * @return
   */
  String getSlotState(AzureWebClientContext context, String slotName);

  /**
   * Update deployment slot application settings.
   *
   * @param context
   * @param slotName
   * @param appSettings
   */
  void updateDeploymentSlotAppSettings(
      AzureWebClientContext context, String slotName, Map<String, AzureAppServiceApplicationSetting> appSettings);

  /**
   * List deployment slot application settings.
   *
   * @param context
   * @param slotName
   * @return
   */
  Map<String, AzureAppServiceApplicationSetting> listDeploymentSlotAppSettings(
      AzureWebClientContext context, String slotName);

  /**
   * Update deployment slot connection settings.
   *
   * @param context
   * @param slotName
   * @param connectionSettings
   */
  void updateDeploymentSlotConnectionSettings(
      AzureWebClientContext context, String slotName, Map<String, AzureAppServiceConnectionString> connectionSettings);

  /**
   * List deployment slot connection settings.
   *
   * @param context
   * @param slotName
   * @return
   */
  Map<String, AzureAppServiceConnectionString> listDeploymentSlotConnectionSettings(
      AzureWebClientContext context, String slotName);

  /**
   * Update deployment slot docker settings.
   *
   * @param context
   * @param slotName
   * @param dockerSettings
   */
  void updateDeploymentSlotDockerSettings(
      AzureWebClientContext context, String slotName, Map<String, AzureAppServiceDockerSetting> dockerSettings);

  /**
   * List deployment slot docker settings.
   *
   * @param azureWebClientContext
   * @param slotName
   * @return
   */
  Map<String, AzureAppServiceDockerSetting> listDeploymentSlotDockerSettings(
      AzureWebClientContext azureWebClientContext, String slotName);

  /**
   * List deployment slot docker settings.
   *
   * @param context
   * @param slotName
   */
  void deleteDeploymentSlotDockerSettings(AzureWebClientContext context, String slotName);

  /**
   * Update deployment slot docker image name and tag settings.
   *
   * @param context
   * @param slotName
   * @param newImageAndTag
   * @param hostingOS
   */
  void updateDeploymentSlotDockerImageNameAndTagSettings(
      AzureWebClientContext context, String slotName, String newImageAndTag, WebAppHostingOS hostingOS);

  /**
   * Delete deployment slot docker image name and tag settings.
   *
   * @param context
   * @param slotName
   */
  void deleteDeploymentSlotDockerImageNameAndTagSettings(AzureWebClientContext context, String slotName);

  /**
   * Update deployment slot traffic.
   *
   * @param context
   * @param slotName
   * @param trafficReroutePercentage
   */
  void updateDeploymentSlotTraffic(AzureWebClientContext context, String slotName, double trafficReroutePercentage);

  /**
   * Swap deployment slot with production slot.
   *
   * @param context
   * @param sourceSlotName
   */
  void swapDeploymentSlotWithProduction(AzureWebClientContext context, String sourceSlotName);

  /**
   * Swap deployment slots.
   *
   * @param context
   * @param sourceSlotName
   * @param targetSlotName
   */
  void swapDeploymentSlots(AzureWebClientContext context, String sourceSlotName, String targetSlotName);

  /**
   * Swap deployment slot with production async.
   *
   * @param context
   * @param sourceSlotName
   * @return
   */
  Observable<Void> swapDeploymentSlotWithProductionAsync(AzureWebClientContext context, String sourceSlotName);

  /**
   * Swap deployment slots async.
   *
   * @param context
   * @param sourceSlotName
   * @param targetSlotName
   * @return
   */
  Observable<Void> swapDeploymentSlotsAsync(
      AzureWebClientContext context, final String sourceSlotName, String targetSlotName);

  /**
   * Get Web App hosting operating system.
   *
   * @param azureWebClientContext
   * @return
   */
  WebAppHostingOS getWebAppHostingOS(AzureWebClientContext azureWebClientContext);
}
