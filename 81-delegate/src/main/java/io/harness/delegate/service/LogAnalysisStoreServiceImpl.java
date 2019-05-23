package io.harness.delegate.service;

import static io.harness.network.SafeHttpCall.execute;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.verification.VerificationServiceClient;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.LogAnalysisStoreService;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;

import java.util.List;

/**
 * Created by rsingh on 06/20/17.
 */
@Singleton
@Slf4j
public class LogAnalysisStoreServiceImpl implements LogAnalysisStoreService {
  @Inject private VerificationServiceClient verificationServiceClient;

  @Override
  public boolean save(StateType stateType, String accountId, String appId, String cvConfigId, String stateExecutionId,
      String workflowId, String workflowExecutionId, String serviceId, String delegateTaskId, List<LogElement> logs) {
    try {
      switch (stateType) {
        case SPLUNKV2:
          return execute(verificationServiceClient.saveLogs(accountId, appId, cvConfigId, stateExecutionId, workflowId,
                             workflowExecutionId, serviceId, ClusterLevel.L2, delegateTaskId, StateType.SPLUNKV2, logs))
              .getResource();
        case SUMO:
        case ELK:
        case LOGZ:
        case LOG_VERIFICATION:
        case BUG_SNAG:
        case DATA_DOG_LOG:
          return execute(verificationServiceClient.saveLogs(accountId, appId, cvConfigId, stateExecutionId, workflowId,
                             workflowExecutionId, serviceId, ClusterLevel.L0, delegateTaskId, stateType, logs))
              .getResource();
        default:
          throw new IllegalStateException("Invalid state: " + stateType);
      }
    } catch (Exception ex) {
      logger.error("Exception while saving log data for stateExecutionId: {}", stateExecutionId, ex);
      return false;
    }
  }
}
