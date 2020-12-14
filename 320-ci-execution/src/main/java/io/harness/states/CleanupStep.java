package io.harness.states;

import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.steps.stepinfo.CleanupStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.delegate.beans.ci.CIK8CleanupTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ng.core.NGAccess;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.resolver.RefObjectUtil;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.stateutils.buildstate.ConnectorUtils;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;

/**
 * State sends cleanup task to finish CI build job. It has to be executed in the end once all steps are complete
 */

@Slf4j
// TODO Cleanup Support for other types (Non K8)
public class CleanupStep implements SyncExecutable<CleanupStepInfo> {
  public static final StepType STEP_TYPE = CleanupStepInfo.typeInfo.getStepType();
  public static final String TASK_TYPE = "EXECUTE_COMMAND";
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private ConnectorUtils connectorUtils;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  @Override
  public Class<CleanupStepInfo> getStepParametersClass() {
    return CleanupStepInfo.class;
  }

  // TODO Async can not be supported at this point. We have to build polling framework on CI manager.
  //     Async will be supported once we will have delegate microservice ready.

  @Override
  public StepResponse executeSync(Ambiance ambiance, CleanupStepInfo cleanupStepInfo, StepInputPackage inputPackage,
      PassThroughData passThroughData) {
    try {
      K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
          ambiance, RefObjectUtil.getSweepingOutputRefObject(ContextElement.podDetails));

      final String namespace = k8PodDetails.getNamespace();
      final String clusterName = k8PodDetails.getClusterName();
      NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);

      ConnectorDetails connectorDetails = connectorUtils.getConnectorDetails(ngAccess, clusterName);

      CIK8CleanupTaskParams cik8CleanupTaskParams = CIK8CleanupTaskParams.builder()
                                                        .k8sConnector(connectorDetails)
                                                        .namespace(namespace)
                                                        .podNameList(new ArrayList<>())
                                                        .serviceNameList(new ArrayList<>())
                                                        .build();

      DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                    .accountId(ngAccess.getAccountIdentifier())
                                                    .taskSetupAbstractions(ambiance.getSetupAbstractionsMap())
                                                    .executionTimeout(Duration.ofSeconds(cleanupStepInfo.getTimeout()))
                                                    .taskType(TASK_TYPE)
                                                    .taskParameters(cik8CleanupTaskParams)
                                                    .taskDescription("Execute command task")
                                                    .build();

      log.info("Sending cleanup task");
      K8sTaskExecutionResponse k8sTaskExecutionResponse =
          (K8sTaskExecutionResponse) delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
      if (k8sTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
        log.info("Cleanup task completed successfully");
        return StepResponse.builder().status(Status.SUCCEEDED).build();
      } else {
        log.error("Cleanup task completed with status {}", k8sTaskExecutionResponse.getCommandExecutionStatus());
        return StepResponse.builder().status(Status.FAILED).build();
      }
    } catch (Exception e) {
      log.error("Cleanup task errored", e);
      return StepResponse.builder().status(Status.ERRORED).build();
    }
  }
}
