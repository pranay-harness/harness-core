package software.wings.delegate.service;

import static software.wings.managerclient.SafeHttpCall.execute;

import software.wings.delegatetasks.LogAnalysisStoreService;
import software.wings.managerclient.ManagerClient;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by rsingh on 06/20/17.
 */
@Singleton
public class LogAnalysisStoreServiceImpl implements LogAnalysisStoreService {
  @Inject private ManagerClient managerClient;

  @Override
  public boolean save(StateType stateType, String accountId, String appId, String stateExecutionId, String workflowId,
      String workflowExecutionId, String serviceId, String delegateTaskId, List<LogElement> logs) throws IOException {
    switch (stateType) {
      case SPLUNKV2:
        return execute(managerClient.saveSplunkLogs(accountId, appId, stateExecutionId, workflowId, workflowExecutionId,
                           serviceId, ClusterLevel.L1, delegateTaskId, logs))
            .getResource();
      case ELK:
        return execute(managerClient.saveElkLogs(accountId, appId, stateExecutionId, workflowId, workflowExecutionId,
                           serviceId, ClusterLevel.L0, delegateTaskId, logs))
            .getResource();
      case LOGZ:
        return execute(managerClient.saveLogzLogs(accountId, appId, stateExecutionId, workflowId, workflowExecutionId,
                           serviceId, ClusterLevel.L0, delegateTaskId, logs))
            .getResource();
      default:
        throw new IllegalStateException("Invalid state: " + stateType);
    }
  }
}
