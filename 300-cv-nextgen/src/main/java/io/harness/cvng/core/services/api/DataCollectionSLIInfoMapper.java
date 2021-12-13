package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.DataCollectionInfo;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import java.util.List;

public interface DataCollectionSLIInfoMapper<R extends DataCollectionInfo, T extends CVConfig> {
  R toDataCollectionInfo(List<T> cvConfig, ServiceLevelIndicator serviceLevelIndicator);
}
