/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks;

import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

import java.util.List;

/**
 * Created by rsingh on 5/18/17.
 */
public interface MetricDataStoreService {
  boolean saveNewRelicMetrics(String accountId, String applicationId, String stateExecutionId, String delegateTaskID,
      List<NewRelicMetricDataRecord> metricData) throws Exception;
}
