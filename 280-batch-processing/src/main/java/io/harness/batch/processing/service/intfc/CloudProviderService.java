package io.harness.batch.processing.service.intfc;

import io.harness.ccm.commons.constants.CloudProvider;

import java.util.List;

public interface CloudProviderService {
  CloudProvider getK8SCloudProvider(String cloudProviderId, String providerId);

  List<CloudProvider> getFirstClassSupportedCloudProviders();
}
