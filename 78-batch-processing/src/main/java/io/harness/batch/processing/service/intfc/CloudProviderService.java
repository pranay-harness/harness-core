package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.pricing.data.CloudProvider;

import java.util.List;

public interface CloudProviderService {
  CloudProvider getK8SCloudProvider(String cloudProviderId, String providerId);

  List<CloudProvider> getFirstClassSupportedCloudProviders();
}
