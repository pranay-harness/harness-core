package software.wings.helpers.ext.azure;

import static com.google.common.base.Charsets.UTF_8;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.encoding.EncodingUtils.decodeBase64ToString;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.CLUSTER_NOT_FOUND;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.eraro.ErrorCode.INVALID_AZURE_VAULT_CONFIGURATION;
import static io.harness.exception.WingsException.USER;
import static io.harness.network.Http.getOkHttpClientBuilder;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.infrastructure.Host.Builder.aHost;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerservice.KubernetesCluster;
import com.microsoft.azure.management.containerservice.OSType;
import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.rest.LogLevel;
import io.fabric8.kubernetes.api.model.AuthInfo;
import io.fabric8.kubernetes.api.model.Cluster;
import io.fabric8.kubernetes.api.model.Context;
import io.fabric8.kubernetes.client.internal.KubeConfigUtils;
import io.harness.beans.PageResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.http.HttpStatus;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.AzureAvailabilitySet;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureContainerRegistry;
import software.wings.beans.AzureInfrastructureMapping;
import software.wings.beans.AzureKubernetesCluster;
import software.wings.beans.AzureTag;
import software.wings.beans.AzureTagDetails;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.AzureVirtualMachineScaleSet;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.infra.AzureInstanceInfrastructure;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class AzureHelperService {
  private static final int CONNECT_TIMEOUT = 5; // TODO:: read from config
  private static final int READ_TIMEOUT = 10;
  @Inject private EncryptionService encryptionService;

  private AzureConfig validateAndGetAzureConfig(SettingAttribute computeProviderSetting) {
    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof AzureConfig)) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "InvalidConfiguration");
    }

    return (AzureConfig) computeProviderSetting.getValue();
  }

  public void validateAzureAccountCredential(AzureConfig azureConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      if (isNotEmpty(encryptedDataDetails)) {
        encryptionService.decrypt(azureConfig, encryptedDataDetails);
      }

      ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(azureConfig.getClientId(),
          azureConfig.getTenantId(), new String(azureConfig.getKey()), AzureEnvironment.AZURE);

      Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials).withDefaultSubscription();

    } catch (Exception e) {
      HandleAzureAuthenticationException(e);
    }
  }

  public Map<String, String> listSubscriptions(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(azureConfig, encryptionDetails);
    Azure azure = getAzureClient(azureConfig);
    Map<String, String> subscriptionMap = new HashMap<>();
    azure.subscriptions().list().forEach(sub -> subscriptionMap.put(sub.subscriptionId(), sub.displayName()));
    return subscriptionMap;
  }

  public boolean isValidSubscription(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, String subscriptionId) {
    encryptionService.decrypt(azureConfig, encryptionDetails);
    return getAzureClient(azureConfig)
               .subscriptions()
               .list()
               .stream()
               .filter(subscription -> subscription.subscriptionId().equalsIgnoreCase(subscriptionId))
               .count()
        != 0;
  }

  public List<VirtualMachine> listVms(AzureInfrastructureMapping azureInfrastructureMapping,
      SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails) {
    notNullCheck("Infra mapping", azureInfrastructureMapping);

    String subscriptionId = azureInfrastructureMapping.getSubscriptionId();
    String resourceGroup = azureInfrastructureMapping.getResourceGroup();
    List<AzureTag> tags = azureInfrastructureMapping.getTags();

    Map<String, String> tagsMap = new HashMap<>();
    for (AzureTag tag : tags) {
      tagsMap.put(tag.getKey(), tag.getValue());
    }

    notNullCheck("Compute Provider", computeProviderSetting);
    AzureConfig azureConfig = validateAndGetAzureConfig(computeProviderSetting);
    if (DeploymentType.WINRM.name().equals(azureInfrastructureMapping.getDeploymentType())) {
      return listVmsByTagsAndResourceGroup(
          azureConfig, encryptedDataDetails, subscriptionId, resourceGroup, tagsMap, OSType.WINDOWS);
    } else if (DeploymentType.SSH.name().equals(azureInfrastructureMapping.getDeploymentType())) {
      return listVmsByTagsAndResourceGroup(
          azureConfig, encryptedDataDetails, subscriptionId, resourceGroup, tagsMap, OSType.LINUX);
    }
    return Collections.EMPTY_LIST;
  }

  public List<VirtualMachine> listVms(AzureInstanceInfrastructure azureInstanceInfrastructure,
      SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails,
      DeploymentType deploymentType) {
    notNullCheck("Azure InfraStructure Definition", azureInstanceInfrastructure, USER);

    String subscriptionId = azureInstanceInfrastructure.getSubscriptionId();
    String resourceGroup = azureInstanceInfrastructure.getResourceGroup();
    List<AzureTag> tags = azureInstanceInfrastructure.getTags();

    Map<String, String> tagsMap = new HashMap<>();

    if (tags != null) {
      tags.stream().map(tag -> tagsMap.put(tag.getKey(), tag.getValue()));
    }

    AzureConfig azureConfig = validateAndGetAzureConfig(computeProviderSetting);
    if (DeploymentType.WINRM.equals(deploymentType)) {
      return listVmsByTagsAndResourceGroup(
          azureConfig, encryptedDataDetails, subscriptionId, resourceGroup, tagsMap, OSType.WINDOWS);
    } else if (DeploymentType.SSH.equals(deploymentType)) {
      return listVmsByTagsAndResourceGroup(
          azureConfig, encryptedDataDetails, subscriptionId, resourceGroup, tagsMap, OSType.LINUX);
    }
    return Collections.EMPTY_LIST;
  }

  public List<AzureAvailabilitySet> listAvailabilitySets(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, String subscriptionId) {
    encryptionService.decrypt(azureConfig, encryptionDetails);
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    return azure.availabilitySets()
        .list()
        .stream()
        .map(as
            -> AzureAvailabilitySet.builder()
                   .name(as.name())
                   .resourceGroup(as.resourceGroupName())
                   .type(as.type())
                   .id(as.id())
                   .build())
        .collect(toList());
  }

  private OSType getVmOSType(VirtualMachine vm) {
    if (vm.osProfile() != null && vm.osProfile().windowsConfiguration() != null) {
      return OSType.WINDOWS;
    } else if (vm.osProfile() != null && vm.osProfile().linuxConfiguration() != null) {
      return OSType.LINUX;
    } else {
      return null;
    }
  }

  public List<VirtualMachine> listVmsByTagsAndResourceGroup(AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptionDetails, String subscriptionId, String resourceGroupName,
      Map<String, String> tags, OSType osType) {
    List<VirtualMachine> matchingVMs = new ArrayList<>();

    encryptionService.decrypt(azureConfig, encryptionDetails);
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    List<VirtualMachine> listVms = azure.virtualMachines().listByResourceGroup(resourceGroupName);

    if (isEmpty(listVms)) {
      logger.info("List VMs by Tags and Resource group did not find any matching VMs in Azure for subscription : "
          + subscriptionId);
      return Collections.emptyList();
    }

    // Filter VMs by OS type
    if (osType != null && (OSType.WINDOWS.equals(osType) || OSType.LINUX.equals(osType))) {
      listVms = listVms.stream()
                    .filter(vm -> getVmOSType(vm) != null && getVmOSType(vm).equals(osType))
                    .collect(Collectors.toList());
    }

    // Filter by tags if present, tags are optional.
    for (VirtualMachine vm : listVms) {
      if (tags.isEmpty()) {
        matchingVMs.add(vm);
      } else if (vm.inner() != null && vm.inner().getTags() != null) {
        if (vm.inner().getTags().keySet().containsAll(tags.keySet())
            && vm.inner().getTags().values().containsAll(tags.values())) {
          matchingVMs.add(vm);
        }
      }
    }

    return matchingVMs;
  }

  public PageResponse<Host> listHosts(AzureInfrastructureMapping azureInfrastructureMapping,
      SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails,
      DeploymentType deploymentType) {
    List<VirtualMachine> vms = listVms(azureInfrastructureMapping, computeProviderSetting, encryptedDataDetails);

    if (isNotEmpty(vms)) {
      List<Host> azureHosts = new ArrayList<>();
      for (VirtualMachine vm : vms) {
        Host host =
            aHost()
                .withHostName(vm.name())
                .withPublicDns(azureInfrastructureMapping.isUsePublicDns()
                        ? (vm.getPrimaryPublicIPAddress() != null ? vm.getPrimaryPublicIPAddress().fqdn() : null)
                        : null)
                .withAppId(azureInfrastructureMapping.getAppId())
                .withEnvId(azureInfrastructureMapping.getEnvId())
                .withHostConnAttr(azureInfrastructureMapping.getHostConnectionAttrs())
                .withWinrmConnAttr(DeploymentType.WINRM.equals(deploymentType)
                        ? azureInfrastructureMapping.getWinRmConnectionAttributes()
                        : null)
                .withInfraMappingId(azureInfrastructureMapping.getUuid())
                .withInfraDefinitionId(azureInfrastructureMapping.getInfrastructureDefinitionId())
                .withServiceTemplateId(azureInfrastructureMapping.getServiceTemplateId())
                .build();
        azureHosts.add(host);
      }
      return aPageResponse().withResponse(azureHosts).build();
    }

    return aPageResponse().withResponse(Collections.emptyList()).build();
  }

  public PageResponse<Host> listHosts(InfrastructureDefinition infrastructureDefinition,
      SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails,
      DeploymentType deploymentType) {
    AzureInstanceInfrastructure azureInstanceInfrastructure =
        (AzureInstanceInfrastructure) infrastructureDefinition.getInfrastructure();
    List<VirtualMachine> vms =
        listVms(azureInstanceInfrastructure, computeProviderSetting, encryptedDataDetails, deploymentType);

    if (isNotEmpty(vms)) {
      List<Host> azureHosts = new ArrayList<>();
      for (VirtualMachine vm : vms) {
        Host host =
            aHost()
                .withHostName(vm.name())
                .withPublicDns(azureInstanceInfrastructure.isUsePublicDns()
                        ? (vm.getPrimaryPublicIPAddress() != null ? vm.getPrimaryPublicIPAddress().fqdn() : null)
                        : null)
                .withAppId(infrastructureDefinition.getAppId())
                .withEnvId(infrastructureDefinition.getEnvId())
                .withHostConnAttr(azureInstanceInfrastructure.getHostConnectionAttrs())
                .withWinrmConnAttr(DeploymentType.WINRM.equals(deploymentType)
                        ? azureInstanceInfrastructure.getWinRmConnectionAttributes()
                        : null)
                .withInfraMappingId(infrastructureDefinition.getUuid())
                .withInfraDefinitionId(infrastructureDefinition.getUuid())
                .build();
        azureHosts.add(host);
      }
      return aPageResponse().withResponse(azureHosts).build();
    }

    return aPageResponse().withResponse(Collections.emptyList()).build();
  }

  public List<AzureVirtualMachineScaleSet> listVirtualMachineScaleSets(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, String subscriptionId) {
    encryptionService.decrypt(azureConfig, encryptionDetails);
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    return azure.virtualMachineScaleSets()
        .list()
        .stream()
        .map(as
            -> AzureVirtualMachineScaleSet.builder()
                   .name(as.name())
                   .resourceGroup(as.resourceGroupName())
                   .type(as.type())
                   .id(as.id())
                   .build())
        .collect(toList());
  }

  public List<AzureTagDetails> listTags(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, String subscriptionId) {
    encryptionService.decrypt(azureConfig, encryptionDetails);
    try {
      Response<AzureListTagsResponse> response =
          getAzureManagementRestClient().listTags(getAzureBearerAuthToken(azureConfig), subscriptionId).execute();

      if (response.isSuccessful()) {
        return response.body()
            .getValue()
            .stream()
            .map(tagDetails
                -> AzureTagDetails.builder()
                       .tagName(tagDetails.getTagName())
                       .values(tagDetails.getValues().stream().map(value -> value.getTagValue()).collect(toList()))
                       .build())
            .collect(toList());
      } else {
        logger.error("Error occurred while getting Tags from subscriptionId : " + subscriptionId
            + " Response: " + response.raw());
        throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE).addParam("message", response.message());
      }
    } catch (Exception e) {
      HandleAzureAuthenticationException(e);
      return null;
    }
  }

  public Set<String> listTagsBySubscription(
      String subscriptionId, AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(azureConfig, encryptionDetails);
    try {
      Response<AzureListTagsResponse> response =
          getAzureManagementRestClient().listTags(getAzureBearerAuthToken(azureConfig), subscriptionId).execute();

      if (response.isSuccessful()) {
        return response.body().getValue().stream().map(tagDetails -> tagDetails.getTagName()).collect(toSet());
      } else {
        logger.error("Error occurred while getting Tags from subscriptionId : " + subscriptionId
            + " Response: " + response.raw());
        throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE).addParam("message", response.message());
      }
    } catch (Exception e) {
      HandleAzureAuthenticationException(e);
      return null;
    }
  }

  public Set<String> listResourceGroups(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, String subscriptionId) {
    encryptionService.decrypt(azureConfig, encryptionDetails);

    try {
      Azure azure = getAzureClient(azureConfig, subscriptionId);
      notNullCheck("Azure Client", azure);
      List<ResourceGroup> resourceGroupList = azure.resourceGroups().list();
      return resourceGroupList.stream().map(rg -> rg.name()).collect(Collectors.toSet());
    } catch (Exception e) {
      HandleAzureAuthenticationException(e);
    }
    return Collections.EMPTY_SET;
  }

  public List<String> listContainerRegistryNames(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, String subscriptionId) {
    encryptionService.decrypt(azureConfig, encryptionDetails);
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    List<String> registries = new ArrayList<>();
    azure.containerRegistries().list().forEach(registry -> registries.add(registry.name()));
    return registries;
  }

  public List<AzureContainerRegistry> listContainerRegistries(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, String subscriptionId) {
    encryptionService.decrypt(azureConfig, encryptionDetails);
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    List<AzureContainerRegistry> registries = new ArrayList<>();
    azure.containerRegistries().list().forEach(registry
        -> registries.add(AzureContainerRegistry.builder()
                              .id(registry.id())
                              .name(registry.name())
                              .resourceGroup(registry.resourceGroupName())
                              .subscriptionId(subscriptionId)
                              .loginServer(registry.loginServerUrl())
                              .build()));
    return registries;
  }

  public boolean isValidContainerRegistry(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String subscriptionId, String registryName) {
    encryptionService.decrypt(azureConfig, encryptionDetails);
    return getAzureClient(azureConfig, subscriptionId)
               .containerRegistries()
               .list()
               .stream()
               .filter(registry -> registry.name().equalsIgnoreCase(registryName))
               .count()
        != 0;
  }

  public String getLoginServerForRegistry(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String subscriptionId, String registryName) {
    encryptionService.decrypt(azureConfig, encryptionDetails);
    return getRegistry(azureConfig, encryptionDetails, subscriptionId, registryName).loginServerUrl();
  }

  private Registry getRegistry(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String subscriptionId, String registryName) {
    encryptionService.decrypt(azureConfig, encryptionDetails);
    return getAzureClient(azureConfig, subscriptionId)
        .containerRegistries()
        .list()
        .stream()
        .filter(item -> item.name().equals(registryName))
        .findFirst()
        .get();
  }

  public List<String> listRepositories(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String subscriptionId, String registryName) {
    encryptionService.decrypt(azureConfig, encryptionDetails);
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    try {
      Registry registry = azure.containerRegistries()
                              .list()
                              .stream()
                              .filter(item -> item.name().equals(registryName))
                              .findFirst()
                              .get();
      AcrRestClient acrRestClient = getAcrRestClient(registry.loginServerUrl());
      List<String> repositories =
          acrRestClient.listRepositories(getAuthHeader(azureConfig.getClientId(), new String(azureConfig.getKey())))
              .execute()
              .body()
              .getRepositories();
      if (repositories == null) {
        return Collections.emptyList();
      }
      return repositories;
    } catch (Exception e) {
      logger.error("Error occurred while getting repositories from subscriptionId/registryName :" + subscriptionId + "/"
              + registryName,
          e);
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE).addParam("message", ExceptionUtils.getMessage(e));
    }
  }

  public List<String> listRepositoryTags(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String subscriptionId, String registryName, String repositoryName) {
    encryptionService.decrypt(azureConfig, encryptionDetails);
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    try {
      Registry registry = azure.containerRegistries()
                              .list()
                              .stream()
                              .filter(item -> item.name().equals(registryName))
                              .findFirst()
                              .get();
      AcrRestClient acrRestClient = getAcrRestClient(registry.loginServerUrl());
      return acrRestClient
          .listRepositoryTags(
              getAuthHeader(azureConfig.getClientId(), new String(azureConfig.getKey())), repositoryName)
          .execute()
          .body()
          .getTags();
    } catch (Exception e) {
      logger.error("Error occurred while getting repositories from subscriptionId/registryName/repositoryName :"
              + subscriptionId + "/" + registryName + "/" + repositoryName,
          e);
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE).addParam("message", ExceptionUtils.getMessage(e));
    }
  }

  public List<String> listRepositoryTags(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String registryHostName, String repositoryName) {
    encryptionService.decrypt(azureConfig, encryptionDetails);
    try {
      AcrRestClient acrRestClient = getAcrRestClient(registryHostName);
      return acrRestClient
          .listRepositoryTags(
              getAuthHeader(azureConfig.getClientId(), new String(azureConfig.getKey())), repositoryName)
          .execute()
          .body()
          .getTags();
    } catch (Exception e) {
      logger.error("Error occurred while getting Tags for Repository :" + registryHostName + "/" + repositoryName, e);
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE).addParam("message", ExceptionUtils.getMessage(e));
    }
  }

  public List<AzureKubernetesCluster> listKubernetesClusters(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, String subscriptionId) {
    encryptionService.decrypt(azureConfig, encryptionDetails);

    return getAzureClient(azureConfig, subscriptionId)
        .kubernetesClusters()
        .list()
        .stream()
        .map(cluster
            -> AzureKubernetesCluster.builder()
                   .name(cluster.name())
                   .resourceGroup(cluster.resourceGroupName())
                   .type(cluster.type())
                   .id(cluster.id())
                   .build())
        .collect(toList());
  }

  public boolean isValidKubernetesCluster(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String subscriptionId, String resourceGroup, String clusterName) {
    encryptionService.decrypt(azureConfig, encryptionDetails);
    KubernetesCluster cluster =
        getAzureClient(azureConfig, subscriptionId).kubernetesClusters().getByResourceGroup(resourceGroup, clusterName);
    return cluster != null;
  }

  public KubernetesConfig getKubernetesClusterConfig(AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptionDetails, AzureKubernetesCluster azureKubernetesCluster, String namespace) {
    return getKubernetesClusterConfig(azureConfig, encryptionDetails, azureKubernetesCluster.getSubscriptionId(),
        azureKubernetesCluster.getResourceGroup(), azureKubernetesCluster.getName(), namespace);
  }

  public KubernetesConfig getKubernetesClusterConfig(AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptionDetails, String subscriptionId, String resourceGroup, String clusterName,
      String namespace) {
    encryptionService.decrypt(azureConfig, encryptionDetails);
    try {
      Response<AksGetCredentialsResponse> response =
          getAzureManagementRestClient()
              .getAdminCredentials(getAzureBearerAuthToken(azureConfig), subscriptionId, resourceGroup, clusterName)
              .execute();

      if (response.isSuccessful()) {
        return parseConfig(
            response.body().getProperties().getKubeConfig(), isNotBlank(namespace) ? namespace : "default");
      } else {
        String errorMessage =
            "Error occurred while getting KubernetesClusterConfig from subscriptionId/resourceGroup/clusterName :"
            + subscriptionId + "/" + resourceGroup + "/" + clusterName + response.raw();
        logger.error(errorMessage);
        int statusCode = response.code();
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
          throw new WingsException(CLUSTER_NOT_FOUND).addParam("message", errorMessage);
        } else {
          throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE).addParam("message", response.message());
        }
      }
    } catch (Exception e) {
      HandleAzureAuthenticationException(e);
    }
    return null;
  }

  private KubernetesConfig parseConfig(String configContent, String namespace) {
    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      io.fabric8.kubernetes.api.model.Config kubeConfig =
          mapper.readValue(decodeBase64ToString(configContent), io.fabric8.kubernetes.api.model.Config.class);

      Context currentContext = KubeConfigUtils.getCurrentContext(kubeConfig);
      Cluster currentCluster = KubeConfigUtils.getCluster(kubeConfig, currentContext);
      AuthInfo currentAuthInfo = KubeConfigUtils.getUserAuthInfo(kubeConfig, currentContext);

      return KubernetesConfig.builder()
          .namespace(namespace)
          .masterUrl(currentCluster.getServer())
          .caCert(currentCluster.getCertificateAuthorityData().toCharArray())
          .username(currentContext.getUser())
          .clientCert(currentAuthInfo.getClientCertificateData().toCharArray())
          .clientKey(currentAuthInfo.getClientKeyData().toCharArray())
          .build();
    } catch (Exception e) {
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE).addParam("message", ExceptionUtils.getMessage(e));
    }
  }

  private String getAzureBearerAuthToken(AzureConfig azureConfig) {
    try {
      ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(azureConfig.getClientId(),
          azureConfig.getTenantId(), new String(azureConfig.getKey()), AzureEnvironment.AZURE);
      String token = credentials.getToken("https://management.core.windows.net/");
      return "Bearer " + token;
    } catch (Exception e) {
      HandleAzureAuthenticationException(e);
    }
    return null;
  }

  private Azure getAzureClient(AzureConfig azureConfig) {
    try {
      ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(azureConfig.getClientId(),
          azureConfig.getTenantId(), new String(azureConfig.getKey()), AzureEnvironment.AZURE);

      return Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials).withDefaultSubscription();
    } catch (Exception e) {
      HandleAzureAuthenticationException(e);
    }
    return null;
  }

  private Azure getAzureClient(AzureConfig azureConfig, String subscriptionId) {
    try {
      ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(azureConfig.getClientId(),
          azureConfig.getTenantId(), new String(azureConfig.getKey()), AzureEnvironment.AZURE);

      return Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials).withSubscription(subscriptionId);
    } catch (Exception e) {
      HandleAzureAuthenticationException(e);
    }
    return null;
  }

  private AcrRestClient getAcrRestClient(String registryHostName) {
    String url = getUrl(registryHostName);
    OkHttpClient okHttpClient = getOkHttpClientBuilder()
                                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                                    .proxy(Http.checkAndGetNonProxyIfApplicable(url))
                                    .retryOnConnectionFailure(true)
                                    .build();
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(url)
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(AcrRestClient.class);
  }

  private AzureManagementRestClient getAzureManagementRestClient() {
    String url = getUrl("management.azure.com");
    OkHttpClient okHttpClient = getOkHttpClientBuilder()
                                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                                    .proxy(Http.checkAndGetNonProxyIfApplicable(url))
                                    .retryOnConnectionFailure(true)
                                    .build();
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(url)
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(AzureManagementRestClient.class);
  }

  private String getAuthHeader(String username, String password) {
    return "Basic " + encodeBase64String(format("%s:%s", username, password).getBytes(UTF_8));
  }

  public String getUrl(String acrHostName) {
    return "https://" + acrHostName + (acrHostName.endsWith("/") ? "" : "/");
  }

  private void HandleAzureAuthenticationException(Exception e) {
    logger.error("HandleAzureAuthenticationException: Exception:" + e);

    Throwable e1 = e;
    while (e1.getCause() != null) {
      e1 = e1.getCause();
      if (e1 instanceof AuthenticationException) {
        throw new WingsException(ErrorCode.INVALID_CLOUD_PROVIDER)
            .addParam("message", "Invalid Azure credentials." + e1.getMessage());
      }
    }

    throw new WingsException(ErrorCode.GENERAL_ERROR).addParam("message", ExceptionUtils.getMessage(e));
  }

  public List<Vault> listVaults(String accountId, AzureVaultConfig azureVaultConfig) {
    try {
      return listVaultsInternal(accountId, azureVaultConfig);
    } catch (Exception ex) {
      logger.error("Listing vaults failed for account Id {}", accountId, ex);
      throw new WingsException(INVALID_AZURE_VAULT_CONFIGURATION);
    }
  }

  public List<Vault> listVaultsInternal(String accountId, AzureVaultConfig azureVaultConfig) throws IOException {
    Azure azure = null;
    List<Vault> vaultList = new ArrayList<>();
    ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(azureVaultConfig.getClientId(),
        azureVaultConfig.getTenantId(), azureVaultConfig.getSecretKey(), AzureEnvironment.AZURE);

    Authenticated authenticate = Azure.configure().authenticate(credentials);

    if (isEmpty(azureVaultConfig.getSubscription())) {
      azure = authenticate.withDefaultSubscription();
    } else {
      azure = authenticate.withSubscription(azureVaultConfig.getSubscription());
    }
    logger.info("Subscription {} is being used for account Id {}", azure.subscriptionId(), accountId);

    for (ResourceGroup rGroup : azure.resourceGroups().list()) {
      vaultList.addAll(azure.vaults().listByResourceGroup(rGroup.name()));
    }
    logger.info("Found azure vaults {} or account id: {}", vaultList, accountId);
    return vaultList;
  }
}
