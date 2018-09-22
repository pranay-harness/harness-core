package software.wings.service.intfc.analysis;

import com.google.common.collect.TreeBasedTable;

import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

/**
 * Created by rsingh on 3/16/18.
 */
public interface MetricCollectionResponse {
  TreeBasedTable<String, Long, NewRelicMetricDataRecord> getMetricRecords(String transactionName, String metricName,
      String appId, String workflowId, String workflowExecutionId, String stateExecutionId, String serviceId,
      String host, String groupName, long collectionStartTime);
}
