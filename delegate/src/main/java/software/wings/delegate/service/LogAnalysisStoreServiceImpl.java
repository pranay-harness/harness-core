package software.wings.delegate.service;

import static software.wings.managerclient.SafeHttpCall.execute;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.delegatetasks.LogAnalysisStoreService;
import software.wings.managerclient.ManagerClient;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.List;

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
        return execute(managerClient.saveLogs(accountId, appId, stateExecutionId, workflowId, workflowExecutionId,
                           serviceId, ClusterLevel.L2, delegateTaskId, StateType.SPLUNKV2, logs))
            .getResource();
      case SUMO:
        return execute(managerClient.saveLogs(accountId, appId, stateExecutionId, workflowId, workflowExecutionId,
                           serviceId, ClusterLevel.L0, delegateTaskId, StateType.SUMO, logs))
            .getResource();
      case ELK:
        return execute(managerClient.saveLogs(accountId, appId, stateExecutionId, workflowId, workflowExecutionId,
                           serviceId, ClusterLevel.L0, delegateTaskId, StateType.ELK, logs))
            .getResource();
      case LOGZ:
        return execute(managerClient.saveLogs(accountId, appId, stateExecutionId, workflowId, workflowExecutionId,
                           serviceId, ClusterLevel.L0, delegateTaskId, StateType.LOGZ, logs))
            .getResource();
      default:
        throw new IllegalStateException("Invalid state: " + stateType);
    }
  }
}
