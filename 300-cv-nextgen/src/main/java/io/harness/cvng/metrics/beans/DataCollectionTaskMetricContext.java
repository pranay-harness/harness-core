/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.metrics.beans;

import io.harness.metrics.AutoMetricContext;

public class DataCollectionTaskMetricContext extends AutoMetricContext {
  public DataCollectionTaskMetricContext(
      String accountId, String dataCollectionTaskType, String provider, long retryCount) {
    put("accountId", accountId);
    put("dataCollectionTaskType", dataCollectionTaskType);
    put("provider", provider);
    put("retryCount", String.valueOf(retryCount));
  }
}
