/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.DataCollectionInfo;
import io.harness.cvng.core.entities.CVConfig;

public interface DataCollectionInfoMapper<R extends DataCollectionInfo, T extends CVConfig> {
  R toDataCollectionInfo(T cvConfig);
}
