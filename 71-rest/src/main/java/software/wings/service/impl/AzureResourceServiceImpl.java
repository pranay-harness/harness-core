package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidArgumentsException;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.app.MainConfiguration;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureContainerRegistry;
import software.wings.beans.AzureKubernetesCluster;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.service.intfc.AzureResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;
import java.util.Map;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class AzureResourceServiceImpl implements AzureResourceService {
  @Inject private MainConfiguration mainConfiguration;

  @Inject private AzureHelperService azureHelperService;
  @Inject private SettingsService settingService;
  @Inject private SecretManager secretManager;

  @Override
  public Map<String, String> listSubscriptions(String cloudProviderId) {
    SettingAttribute cloudProviderSetting = settingService.get(cloudProviderId);
    AzureConfig azureConfig = validateAndGetAzureConfig(cloudProviderSetting);
    return azureHelperService.listSubscriptions(
        azureConfig, secretManager.getEncryptionDetails(azureConfig, null, null));
  }

  @Override
  public List<String> listContainerRegistryNames(String cloudProviderId, String subscriptionId) {
    SettingAttribute cloudProviderSetting = settingService.get(cloudProviderId);
    AzureConfig azureConfig = validateAndGetAzureConfig(cloudProviderSetting);
    return azureHelperService.listContainerRegistryNames(
        azureConfig, secretManager.getEncryptionDetails(azureConfig, null, null), subscriptionId);
  }

  @Override
  public List<AzureContainerRegistry> listContainerRegistries(String cloudProviderId, String subscriptionId) {
    SettingAttribute cloudProviderSetting = settingService.get(cloudProviderId);
    AzureConfig azureConfig = validateAndGetAzureConfig(cloudProviderSetting);
    return azureHelperService.listContainerRegistries(
        azureConfig, secretManager.getEncryptionDetails(azureConfig, null, null), subscriptionId);
  }

  @Override
  public List<String> listRepositories(String cloudProviderId, String subscriptionId, String registryName) {
    SettingAttribute cloudProviderSetting = settingService.get(cloudProviderId);
    AzureConfig azureConfig = validateAndGetAzureConfig(cloudProviderSetting);
    return azureHelperService.listRepositories(
        azureConfig, secretManager.getEncryptionDetails(azureConfig, null, null), subscriptionId, registryName);
  }

  @Override
  public List<String> listRepositoryTags(
      String cloudProviderId, String subscriptionId, String registryName, String repositoryName) {
    SettingAttribute cloudProviderSetting = settingService.get(cloudProviderId);
    AzureConfig azureConfig = validateAndGetAzureConfig(cloudProviderSetting);
    return azureHelperService.listRepositoryTags(azureConfig,
        secretManager.getEncryptionDetails(azureConfig, null, null), subscriptionId, registryName, repositoryName);
  }

  @Override
  public List<AzureKubernetesCluster> listKubernetesClusters(String cloudProviderId, String subscriptionId) {
    SettingAttribute cloudProviderSetting = settingService.get(cloudProviderId);
    AzureConfig azureConfig = validateAndGetAzureConfig(cloudProviderSetting);
    return azureHelperService.listKubernetesClusters(
        azureConfig, secretManager.getEncryptionDetails(azureConfig, null, null), subscriptionId);
  }

  private AzureConfig validateAndGetAzureConfig(SettingAttribute cloudProviderSetting) {
    if (cloudProviderSetting == null || !(cloudProviderSetting.getValue() instanceof AzureConfig)) {
      throw new InvalidArgumentsException(Pair.of("args", "No cloud provider exist or not of type Azure"));
    }

    return (AzureConfig) cloudProviderSetting.getValue();
  }
}