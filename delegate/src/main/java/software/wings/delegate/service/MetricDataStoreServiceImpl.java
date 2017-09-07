package software.wings.delegate.service;

import static software.wings.managerclient.SafeHttpCall.execute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger logger = LoggerFactory.getLogger(MetricDataStoreServiceImpl.class);

  @Inject private ManagerClient managerClient;

  @Override
  public boolean saveAppDynamicsMetrics(String accountId, String applicationId, String stateExecutionId, long appId,
      long tierId, List<AppdynamicsMetricData> metricData) {
    try {
      return execute(
          managerClient.saveAppdynamicsMetrics(accountId, applicationId, stateExecutionId, appId, tierId, metricData))
          .getResource();
    } catch (IOException e) {
      logger.error("error saving appdynamics metrics", e);
      return false;
    }
  }

  @Override
  public boolean saveNewRelicMetrics(
      String accountId, String applicationId, List<NewRelicMetricDataRecord> metricData) {
    try {
      return execute(managerClient.saveNewRelicMetrics(accountId, applicationId, metricData)).getResource();
    } catch (IOException e) {
      logger.error("error saving new relic metrics", e);
      return false;
    }
  }
}
