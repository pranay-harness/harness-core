package io.harness.states;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.steps.stepinfo.CleanupStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.common.AmbianceHelper;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ng.core.NGAccess;
import io.harness.references.SweepingOutputRefObject;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.stateutils.buildstate.ConnectorUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.TaskType;
import software.wings.beans.ci.CIK8CleanupTaskParams;
import software.wings.beans.ci.pod.ConnectorDetails;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.time.Duration;
import java.util.ArrayList;

/**
 * State sends cleanup task to finish CI build job. It has to be executed in the end once all steps are complete
 */

@Slf4j
// TODO Cleanup Support for other types (Non K8)
public class CleanupStep implements Step, SyncExecutable<CleanupStepInfo> {
  public static final StepType STEP_TYPE = CleanupStepInfo.typeInfo.getStepType();
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private ConnectorUtils connectorUtils;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  // TODO Async can not be supported at this point. We have to build polling framework on CI manager.
  //     Async will be supported once we will have delegate microservice ready.

  @Override
  public StepResponse executeSync(Ambiance ambiance, CleanupStepInfo cleanupStepInfo, StepInputPackage inputPackage,
      PassThroughData passThroughData) {
    try {
      K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
          ambiance, SweepingOutputRefObject.builder().name(ContextElement.podDetails).build());

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
                                                    .taskSetupAbstractions(ambiance.getSetupAbstractions())
                                                    .executionTimeout(Duration.ofSeconds(cleanupStepInfo.getTimeout()))
                                                    .taskType(TaskType.EXECUTE_COMMAND.name())
                                                    .taskParameters(cik8CleanupTaskParams)
                                                    .taskDescription("Execute command task")
                                                    .build();

      logger.info("Sending cleanup task");
      K8sTaskExecutionResponse k8sTaskExecutionResponse =
          (K8sTaskExecutionResponse) delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
      if (k8sTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
        logger.info("Cleanup task completed successfully");
        return StepResponse.builder().status(Status.SUCCEEDED).build();
      } else {
        logger.error("Cleanup task completed with status {}", k8sTaskExecutionResponse.getCommandExecutionStatus());
        return StepResponse.builder().status(Status.FAILED).build();
      }
    } catch (Exception e) {
      logger.error("Cleanup task errored", e);
      return StepResponse.builder().status(Status.ERRORED).build();
    }
  }
}
