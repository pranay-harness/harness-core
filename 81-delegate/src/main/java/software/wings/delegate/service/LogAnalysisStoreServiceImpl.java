package software.wings.delegate.service;

import static io.harness.network.SafeHttpCall.execute;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.delegatetasks.LogAnalysisStoreService;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;
import software.wings.verification.VerificationServiceClient;

import java.io.IOException;
import java.util.List;

/**
 * Created by rsingh on 06/20/17.
 */
@Singleton
public class LogAnalysisStoreServiceImpl implements LogAnalysisStoreService {
  @Inject private VerificationServiceClient verificationServiceClient;

  @Override
  public boolean save(StateType stateType, String accountId, String appId, String stateExecutionId, String workflowId,
      String workflowExecutionId, String serviceId, String delegateTaskId, List<LogElement> logs) throws IOException {
    switch (stateType) {
      case SPLUNKV2:
        return execute(verificationServiceClient.saveLogs(accountId, appId, stateExecutionId, workflowId,
                           workflowExecutionId, serviceId, ClusterLevel.L2, delegateTaskId, StateType.SPLUNKV2, logs))
            .getResource();
      case SUMO:
      case ELK:
      case LOGZ:
      case LOG_VERIFICATION:
      case BUG_SNAG:
        return execute(verificationServiceClient.saveLogs(accountId, appId, stateExecutionId, workflowId,
                           workflowExecutionId, serviceId, ClusterLevel.L0, delegateTaskId, stateType, logs))
            .getResource();
      default:
        throw new IllegalStateException("Invalid state: " + stateType);
    }
  }
}
