package io.harness.cvng.core.services.impl.monitoredService;

import io.harness.cvng.beans.DatadogDataCollectionInfo;
import io.harness.cvng.core.entities.DatadogMetricCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;

import java.util.ArrayList;
import java.util.List;

public class DatadogLogDataCollectionInfoMapper
    implements DataCollectionInfoMapper<DatadogDataCollectionInfo, DatadogMetricCVConfig> {
  @Override
  public DatadogDataCollectionInfo toDataCollectionInfo(DatadogMetricCVConfig cvConfig) {
  return null;
  }
}
