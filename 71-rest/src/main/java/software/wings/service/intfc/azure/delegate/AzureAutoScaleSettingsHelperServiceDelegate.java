package software.wings.service.intfc.azure.delegate;

import com.microsoft.azure.management.monitor.AutoscaleProfile;
import com.microsoft.azure.management.monitor.AutoscaleSetting;
import com.microsoft.azure.management.monitor.ScaleCapacity;
import software.wings.beans.AzureConfig;

import java.util.List;
import java.util.Optional;

public interface AzureAutoScaleSettingsHelperServiceDelegate {
  /**
   * Get Auto Scale Setting in JSON format
   *
   * @param azureConfig
   * @param resourceGroupName
   * @param targetResourceId
   * @return
   */
  Optional<String> getAutoScaleSettingJSONByTargetResourceId(
      AzureConfig azureConfig, String resourceGroupName, String targetResourceId);

  /**
   *
   * @param azureConfig
   * @param resourceGroupName
   * @param targetResourceId
   * @param autoScaleSettingResourceInnerJson
   * @param defaultProfileScaleCapacity
   */
  void attachAutoScaleSettingToTargetResourceId(AzureConfig azureConfig, String resourceGroupName,
      String targetResourceId, List<String> autoScaleSettingResourceInnerJson,
      ScaleCapacity defaultProfileScaleCapacity);
  /**
   * Get Auto Scale Setting
   *
   * @param azureConfig
   * @param resourceGroupName
   * @param targetResourceId
   * @return
   */
  Optional<AutoscaleSetting> getAutoScaleSettingByTargetResourceId(
      AzureConfig azureConfig, String resourceGroupName, String targetResourceId);

  /**
   * Attach Auto Scale Setting to target resource
   *
   * @param azureConfig
   * @param resourceGroupName
   * @param targetResourceId
   * @param autoScaleSettingResourceInnerJson
   */
  void attachAutoScaleSettingToTargetResourceId(AzureConfig azureConfig, String resourceGroupName,
      String targetResourceId, String autoScaleSettingResourceInnerJson);
  /**
   * Attach Auto Scale Setting to target resource
   * @param azureConfig
   * @param resourceGroupName
   * @param targetResourceId
   * @param autoScaleSettingResourceInnerJson
   * @param defaultProfileScaleCapacity
   */
  void attachAutoScaleSettingToTargetResourceId(AzureConfig azureConfig, String resourceGroupName,
      String targetResourceId, String autoScaleSettingResourceInnerJson, ScaleCapacity defaultProfileScaleCapacity);

  /**
   * Clear Auto Scale Setting on target resource
   *
   * @param azureConfig
   * @param resourceGroupName
   * @param targetResourceId
   */
  void clearAutoScaleSettingOnTargetResourceId(
      AzureConfig azureConfig, String resourceGroupName, String targetResourceId);

  /**
   * Get default Auto Scale profile
   *
   * @param azureConfig
   * @param resourceGroupName
   * @param targetResourceId
   * @return
   */
  Optional<AutoscaleProfile> getDefaultAutoScaleProfile(
      AzureConfig azureConfig, String resourceGroupName, String targetResourceId);

  /**
   * List Auto Scale Profiles on target resource
   *
   * @param azureConfig
   * @param resourceGroupName
   * @param targetResourceId
   * @return
   */
  List<AutoscaleProfile> listAutoScaleProfilesByTargetResourceId(
      AzureConfig azureConfig, String resourceGroupName, String targetResourceId);

  /**
   * List Auto Scale Profiles in JSON format on target resource
   *
   * @param azureConfig
   * @param resourceGroupName
   * @param targetResourceId
   * @return
   */
  List<String> listAutoScaleProfileJSONsByTargetResourceId(
      AzureConfig azureConfig, String resourceGroupName, String targetResourceId);
}
