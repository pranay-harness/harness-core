package software.wings.delegate.service;

import static software.wings.managerclient.SafeHttpCall.execute;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.delegatetasks.MetricDataStoreService;
import software.wings.managerclient.ManagerClient;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;

import java.util.List;

/**
 * Created by raghu on 5/19/17.
 */
@Singleton
public class MetricDataStoreServiceImpl implements MetricDataStoreService {
  private static final Logger logger = LoggerFactory.getLogger(MetricDataStoreServiceImpl.class);

  @Inject private ManagerClient managerClient;

  @Override
  public boolean saveNewRelicMetrics(String accountId, String applicationId, String stateExecutionId,
      String delegateTaskId, List<NewRelicMetricDataRecord> metricData) {
    if (metricData.isEmpty()) {
      return true;
    }
    try {
      return execute(
          managerClient.saveNewRelicMetrics(accountId, applicationId, stateExecutionId, delegateTaskId, metricData))
          .getResource();
    } catch (Exception e) {
      logger.error("error saving new relic metrics", e);
      return false;
    }
  }
}
