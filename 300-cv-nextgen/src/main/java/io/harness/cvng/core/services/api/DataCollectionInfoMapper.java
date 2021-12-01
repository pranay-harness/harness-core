package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.DataCollectionInfo;
import io.harness.cvng.core.entities.CVConfig;

public interface DataCollectionInfoMapper<R extends DataCollectionInfo, T extends CVConfig> {
  R toDataCollectionInfo(T cvConfig);
}
