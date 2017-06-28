package software.wings.delegate.service;

import static software.wings.managerclient.SafeHttpCall.execute;

import software.wings.delegatetasks.SplunkMetricStoreService;
import software.wings.managerclient.ManagerClient;
import software.wings.service.impl.splunk.SplunkLogElement;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by rsingh on 06/20/17.
 */
@Singleton
public class SplunkMetricStoreServiceImpl implements SplunkMetricStoreService {
  @Inject private ManagerClient managerClient;

  @Override
  public void save(String accountId, String appId, String stateExecutionId, List<SplunkLogElement> splunkLogs)
      throws IOException {
    execute(managerClient.saveSplunkLogs(accountId, appId, stateExecutionId, splunkLogs));
  }
}
