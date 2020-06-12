package io.harness.cvng.core.services.impl;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;

import io.harness.cvng.beans.SplunkDSConfig;
import io.harness.cvng.core.services.api.CVConfigTransformer;
import io.harness.cvng.core.services.entities.SplunkCVConfig;

import java.util.List;
@Singleton
public class SplunkCVConfigTransformer implements CVConfigTransformer<SplunkCVConfig, SplunkDSConfig> {
  @Override
  public SplunkDSConfig transformToDSConfig(List<SplunkCVConfig> cvConfigs) {
    Preconditions.checkArgument(
        cvConfigs.size() == 1, "Splunk Config should be of size 1 since it's a one to one mapping.");
    SplunkDSConfig splunkDataSourceCVConfig = new SplunkDSConfig();
    SplunkCVConfig splunkCVConfig = cvConfigs.get(0);
    splunkDataSourceCVConfig.populateCommonFields(splunkCVConfig);
    splunkDataSourceCVConfig.setEventType(splunkCVConfig.getCategory());
    splunkDataSourceCVConfig.setQuery(splunkCVConfig.getQuery());
    splunkDataSourceCVConfig.setServiceInstanceIdentifier(splunkCVConfig.getServiceInstanceIdentifier());
    return splunkDataSourceCVConfig;
  }
}
