package software.wings.service.impl;

import static software.wings.service.impl.AzureUtils.AZURE_GOV_REGIONS_NAMES;

import io.harness.exception.InvalidArgumentsException;

import software.wings.app.MainConfiguration;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureContainerRegistry;
import software.wings.beans.AzureImageDefinition;
import software.wings.beans.AzureImageGallery;
import software.wings.beans.AzureKubernetesCluster;
import software.wings.beans.AzureResourceGroup;
import software.wings.beans.NameValuePair;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.service.intfc.AzureResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

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

  @Override
  public List<AzureResourceGroup> listResourceGroups(String cloudProviderId, String subscriptionId) {
    SettingAttribute cloudProviderSetting = settingService.get(cloudProviderId);
    AzureConfig azureConfig = validateAndGetAzureConfig(cloudProviderSetting);
    return azureHelperService
        .listResourceGroups(azureConfig, secretManager.getEncryptionDetails(azureConfig, null, null), subscriptionId)
        .stream()
        .map(name -> AzureResourceGroup.builder().name(name).subscriptionId(subscriptionId).build())
        .collect(Collectors.toList());
  }

  @Override
  public List<AzureImageGallery> listImageGalleries(
      String cloudProviderId, String subscriptionId, String resourceGroupName) {
    SettingAttribute cloudProviderSetting = settingService.get(cloudProviderId);
    AzureConfig azureConfig = validateAndGetAzureConfig(cloudProviderSetting);
    return azureHelperService.listImageGalleries(
        azureConfig, secretManager.getEncryptionDetails(azureConfig, null, null), subscriptionId, resourceGroupName);
  }

  @Override
  public List<AzureImageDefinition> listImageDefinitions(
      String cloudProviderId, String subscriptionId, String resourceGroupName, String galleryName) {
    SettingAttribute cloudProviderSetting = settingService.get(cloudProviderId);
    AzureConfig azureConfig = validateAndGetAzureConfig(cloudProviderSetting);
    return azureHelperService.listImageDefinitions(azureConfig,
        secretManager.getEncryptionDetails(azureConfig, null, null), subscriptionId, resourceGroupName, galleryName);
  }

  private AzureConfig validateAndGetAzureConfig(SettingAttribute cloudProviderSetting) {
    if (cloudProviderSetting == null || !(cloudProviderSetting.getValue() instanceof AzureConfig)) {
      throw new InvalidArgumentsException(Pair.of("args", "No cloud provider exist or not of type Azure"));
    }

    return (AzureConfig) cloudProviderSetting.getValue();
  }

  public List<NameValuePair> listAzureRegions() {
    return Arrays.stream(Region.values())
        .filter(nonGovernmentAzureRegionsNamesFilter())
        .map(toNameValuePair())
        .collect(Collectors.toList());
  }

  @NotNull
  private Predicate<Region> nonGovernmentAzureRegionsNamesFilter() {
    return region -> !AZURE_GOV_REGIONS_NAMES.contains(region.name());
  }

  @NotNull
  private Function<Region, NameValuePair> toNameValuePair() {
    return region -> NameValuePair.builder().name(region.label()).value(region.name()).build();
  }
}
