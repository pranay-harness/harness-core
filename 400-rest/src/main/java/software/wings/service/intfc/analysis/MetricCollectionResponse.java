/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.analysis;

import software.wings.delegatetasks.DelegateCVActivityLogService.Logger;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

import com.google.common.collect.TreeBasedTable;

/**
 * Created by rsingh on 3/16/18.
 */
public interface MetricCollectionResponse {
  TreeBasedTable<String, Long, NewRelicMetricDataRecord> getMetricRecords(String transactionName, String metricName,
      String appId, String workflowId, String workflowExecutionId, String stateExecutionId, String serviceId,
      String host, String groupName, long collectionStartTime, String cvConfigId, boolean is247Task, String url,
      Logger activityLogger);
}
