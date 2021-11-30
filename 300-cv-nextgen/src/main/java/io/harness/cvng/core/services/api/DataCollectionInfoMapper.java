package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.DataCollectionInfo;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import java.util.List;

public interface DataCollectionInfoMapper<R extends DataCollectionInfo, T extends CVConfig> {
  R toDataCollectionInfo(T cvConfig);
  R toDataCollectionInfoForSLI(List<T> cvConfig, ServiceLevelIndicator serviceLevelIndicator);
}
