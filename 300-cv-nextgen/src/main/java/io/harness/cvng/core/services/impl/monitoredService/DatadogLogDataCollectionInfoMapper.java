package io.harness.cvng.core.services.impl.monitoredService;

import io.harness.cvng.beans.DatadogLogDataCollectionInfo;
import io.harness.cvng.beans.datadog.DatadogLogDefinition;
import io.harness.cvng.core.entities.DatadogLogCVConfig;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import java.util.List;

public class DatadogLogDataCollectionInfoMapper
    implements DataCollectionInfoMapper<DatadogLogDataCollectionInfo, DatadogLogCVConfig> {
  @Override
  public DatadogLogDataCollectionInfo toDataCollectionInfo(DatadogLogCVConfig cvConfig) {
    DatadogLogDefinition definition = DatadogLogDefinition.builder()
                                          .name(cvConfig.getQueryName())
                                          .query(cvConfig.getQuery())
                                          .indexes(cvConfig.getIndexes())
                                          .serviceInstanceIdentifier(cvConfig.getServiceInstanceIdentifier())
                                          .build();
    DatadogLogDataCollectionInfo datadogLogDataCollectionInfo =
        DatadogLogDataCollectionInfo.builder().logDefinition(definition).build();
    datadogLogDataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());
    return datadogLogDataCollectionInfo;
  }

  @Override
  public DatadogLogDataCollectionInfo toDataCollectionInfoForSLI(
      List<DatadogLogCVConfig> cvConfig, ServiceLevelIndicator serviceLevelIndicator) {
    throw new IllegalStateException("SLI is not configured for DatadogLog");
  }
}
