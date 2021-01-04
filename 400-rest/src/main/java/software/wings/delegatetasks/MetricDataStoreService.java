package software.wings.delegatetasks;

import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

import java.util.List;

/**
 * Created by rsingh on 5/18/17.
 */
public interface MetricDataStoreService {
  boolean saveNewRelicMetrics(String accountId, String applicationId, String stateExecutionId, String delegateTaskID,
      List<NewRelicMetricDataRecord> metricData);
}
