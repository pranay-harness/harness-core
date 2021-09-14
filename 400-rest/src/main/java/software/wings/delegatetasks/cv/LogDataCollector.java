/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.cv;

import software.wings.service.impl.analysis.LogDataCollectionInfoV2;
import software.wings.service.impl.analysis.LogElement;

import java.util.List;

public interface LogDataCollector<T extends LogDataCollectionInfoV2> extends DataCollector<T> {
  /**
   * Called with list of hosts. This needs to be thread safe.
   * @param hostBatch
   * @return Returns list of log elements.
   */
  List<LogElement> fetchLogs(List<String> hostBatch) throws DataCollectionException;

  /**
   * Fetch logs for all the host. This needs to be thread safe.
   * @return Returns list of log elements.
   */
  List<LogElement> fetchLogs() throws DataCollectionException;
}
