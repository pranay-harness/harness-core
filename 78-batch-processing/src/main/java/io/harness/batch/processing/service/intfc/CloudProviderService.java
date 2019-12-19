package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.pricing.data.CloudProvider;

public interface CloudProviderService { CloudProvider getK8SCloudProvider(String cloudProviderId, String providerId); }
