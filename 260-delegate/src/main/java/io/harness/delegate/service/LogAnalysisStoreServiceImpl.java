package io.harness.delegate.service;

import static io.harness.network.SafeHttpCall.execute;

import io.harness.managerclient.VerificationServiceClient;

import software.wings.delegatetasks.LogAnalysisStoreService;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

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
        case STACK_DRIVER_LOG:
        case SCALYR:
          return execute(verificationServiceClient.saveLogs(accountId, appId, cvConfigId, stateExecutionId, workflowId,
                             workflowExecutionId, serviceId, ClusterLevel.L0, delegateTaskId, stateType, logs))
              .getResource();
        default:
          throw new IllegalStateException("Invalid state: " + stateType);
      }
    } catch (Exception ex) {
      log.error("Exception while saving log data for stateExecutionId: {}", stateExecutionId, ex);
      return false;
    }
  }
}
