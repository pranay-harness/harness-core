package io.harness.cvng.core.services.impl.monitoredService;

import io.harness.cvng.beans.DatadogLogDataCollectionInfo;
import io.harness.cvng.beans.datadog.DatadogLogDefinition;
import io.harness.cvng.core.entities.DatadogLogCVConfig;
import io.harness.cvng.core.entities.VerificationTask.TaskType;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;

public class DatadogLogDataCollectionInfoMapper
    implements DataCollectionInfoMapper<DatadogLogDataCollectionInfo, DatadogLogCVConfig> {
  @Override
  public DatadogLogDataCollectionInfo toDataCollectionInfo(DatadogLogCVConfig cvConfig, TaskType taskType) {
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
}
