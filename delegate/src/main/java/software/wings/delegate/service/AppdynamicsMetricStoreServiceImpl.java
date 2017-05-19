package software.wings.delegate.service;

import static software.wings.managerclient.SafeHttpCall.execute;

import software.wings.delegatetasks.AppdynamicsMetricStoreService;
import software.wings.managerclient.ManagerClient;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by peeyushaggarwal on 1/9/17.
 */
@Singleton
public class AppdynamicsMetricStoreServiceImpl implements AppdynamicsMetricStoreService {
  @Inject private ManagerClient managerClient;

  @Override
  public void save(String accountId, List<AppdynamicsMetricData> metricData) {
    try {
      execute(managerClient.saveAppdynamicsMetrics(accountId, metricData));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
