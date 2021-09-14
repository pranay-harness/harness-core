/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.service.intfc;

import io.harness.ccm.commons.constants.CloudProvider;

import java.util.List;

public interface CloudProviderService {
  CloudProvider getK8SCloudProvider(String cloudProviderId, String providerId);

  List<CloudProvider> getFirstClassSupportedCloudProviders();
}
