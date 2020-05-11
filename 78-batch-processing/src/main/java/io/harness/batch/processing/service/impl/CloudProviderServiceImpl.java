package io.harness.batch.processing.service.impl;

import static io.harness.batch.processing.pricing.data.CloudProvider.AWS;
import static io.harness.batch.processing.pricing.data.CloudProvider.AZURE;
import static io.harness.batch.processing.pricing.data.CloudProvider.GCP;

import com.google.common.collect.ImmutableList;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.service.intfc.CloudProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.settings.SettingValue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CloudProviderServiceImpl implements CloudProviderService {
  private final CloudToHarnessMappingService cloudToHarnessMappingService;

  private static final String GCP_SEARCH_STRING = "gce:";
  private static final String AWS_SEARCH_STRING = "aws:";
  private static final String AZURE_SEARCH_STRING = "azure:";
  private static final String IBM_SEARCH_STRING = "ibm:";

  private static final CloudProvider DEFAULT_CLOUD_PROVIDER = GCP;

  private Cache<String, CloudProvider> cloudProviderInfoCache =
      Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();

  @Autowired
  public CloudProviderServiceImpl(CloudToHarnessMappingService cloudToHarnessMappingService) {
    this.cloudToHarnessMappingService = cloudToHarnessMappingService;
  }

  @Override
  public CloudProvider getK8SCloudProvider(String cloudProviderId, String providerId) {
    return cloudProviderInfoCache.get(cloudProviderId, key -> getK8SCloudProviderFromProviderId(key, providerId));
  }

  @Override
  public List<CloudProvider> getFirstClassSupportedCloudProviders() {
    return ImmutableList.of(AWS, AZURE, GCP);
  }

  private CloudProvider getK8SCloudProviderFromProviderId(String cloudProviderId, String providerId) {
    CloudProvider cloudProvider = getCloudProviderForK8SCluster(providerId);
    Optional<SettingAttribute> settingAttributeMaybe =
        cloudToHarnessMappingService.getSettingAttribute(cloudProviderId);
    if (settingAttributeMaybe.isPresent()) {
      SettingAttribute settingAttribute = settingAttributeMaybe.get();
      if (settingAttribute.getCategory() == SettingCategory.CLOUD_PROVIDER) {
        SettingValue value = settingAttribute.getValue();
        String cloudProviderType = value.getType();
        switch (cloudProviderType) {
          case "KUBERNETES_CLUSTER":
            return getCloudProviderForK8SCluster(providerId);
          default:
            return DEFAULT_CLOUD_PROVIDER;
        }
      }
    }
    return cloudProvider;
  }

  private CloudProvider getCloudProviderForK8SCluster(String providerId) {
    if (checkGCPCloudProvider(providerId)) {
      return GCP;
    } else if (checkAWSCloudProvider(providerId)) {
      return AWS;
    } else if (checkAzureCloudProvider(providerId)) {
      return AZURE;
    } else if (checkIbmCloudProvider(providerId)) {
      return CloudProvider.IBM;
    }
    return DEFAULT_CLOUD_PROVIDER;
  }

  private boolean checkGCPCloudProvider(String providerId) {
    return providerId.contains(GCP_SEARCH_STRING);
  }

  private boolean checkAWSCloudProvider(String providerId) {
    return providerId.contains(AWS_SEARCH_STRING);
  }

  private boolean checkAzureCloudProvider(String providerId) {
    return providerId.contains(AZURE_SEARCH_STRING);
  }

  private boolean checkIbmCloudProvider(String providerId) {
    return providerId.contains(IBM_SEARCH_STRING);
  }
}
