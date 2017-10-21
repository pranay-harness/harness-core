package software.wings.delegatetasks;

import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

import java.util.List;

/**
 * Created by rsingh on 5/18/17.
 */
public interface MetricDataStoreService {
  boolean saveAppDynamicsMetrics(String accountId, String applicationId, String stateExecutionId, long appId,
      long tierId, List<AppdynamicsMetricData> metricData);

  boolean saveNewRelicMetrics(String accountId, String applicationId, String stateExecutionId, String delegateTaskID,
      List<NewRelicMetricDataRecord> metricData);
}
