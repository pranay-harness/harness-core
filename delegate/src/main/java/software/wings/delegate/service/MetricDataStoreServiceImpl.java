package software.wings.delegate.service;

import static software.wings.managerclient.SafeHttpCall.execute;

import software.wings.delegatetasks.MetricDataStoreService;
import software.wings.managerclient.ManagerClient;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by raghu on 5/19/17.
 */
@Singleton
public class MetricDataStoreServiceImpl implements MetricDataStoreService {
  @Inject private ManagerClient managerClient;

  @Override
  public void saveAppDynamicsMetrics(String accountId, String applicationId, String stateExecutionId, long appId,
      long tierId, List<AppdynamicsMetricData> metricData) {
    try {
      execute(
          managerClient.saveAppdynamicsMetrics(accountId, applicationId, stateExecutionId, appId, tierId, metricData));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void saveNewRelicMetrics(String accountId, String applicationId, List<NewRelicMetricDataRecord> metricData) {
    try {
      execute(managerClient.saveNewRelicMetrics(accountId, applicationId, metricData));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
