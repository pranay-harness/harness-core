/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.beans.SplunkDSConfig;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.CVConfigTransformer;

import com.google.common.base.Preconditions;
import java.util.List;

public class SplunkCVConfigTransformer implements CVConfigTransformer<SplunkCVConfig, SplunkDSConfig> {
  @Override
  public SplunkDSConfig transformToDSConfig(List<SplunkCVConfig> cvConfigs) {
    Preconditions.checkArgument(
        cvConfigs.size() == 1, "Splunk Config should be of size 1 since it's a one to one mapping.");
    SplunkDSConfig splunkDSConfig = new SplunkDSConfig();
    SplunkCVConfig splunkCVConfig = cvConfigs.get(0);
    splunkDSConfig.populateCommonFields(splunkCVConfig);
    splunkDSConfig.setEventType(splunkCVConfig.getCategory().getDisplayName());
    splunkDSConfig.setQuery(splunkCVConfig.getQuery());
    splunkDSConfig.setServiceInstanceIdentifier(splunkCVConfig.getServiceInstanceIdentifier());
    splunkDSConfig.setServiceIdentifier(splunkCVConfig.getServiceIdentifier());
    return splunkDSConfig;
  }
}
