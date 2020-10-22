package io.harness.states;

import static java.util.stream.Collectors.toList;
import static software.wings.common.CICommonPodConstants.CONTAINER_NAME;
import static software.wings.common.CICommonPodConstants.MOUNT_PATH;
import static software.wings.common.CICommonPodConstants.REL_STDERR_FILE_PATH;
import static software.wings.common.CICommonPodConstants.REL_STDOUT_FILE_PATH;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.script.ScriptInfo;
import io.harness.beans.steps.stepinfo.BuildStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ng.core.NGAccess;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.references.SweepingOutputRefObject;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.stateutils.buildstate.ConnectorUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.TaskType;
import software.wings.beans.ci.K8ExecCommandParams;
import software.wings.beans.ci.K8ExecuteCommandTaskParams;
import software.wings.beans.ci.ShellScriptType;
import software.wings.beans.ci.pod.ConnectorDetails;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.time.Duration;
import java.util.List;

/**
 * This state will execute build command on already setup pod. It will send customer defined commands.
 * Currently it assumes a timeout of 60 minutes
 */

@Slf4j
public class BuildStep implements Step, SyncExecutable<BuildStepInfo> {
  public static final StepType STEP_TYPE = BuildStepInfo.typeInfo.getStepType();
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private ConnectorUtils connectorUtils;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  // TODO Async can not be supported at this point. We have to build polling framework on CI manager.
  //     Async will be supported once we will have delegate microservice ready.

  @Override
  public StepResponse executeSync(
      Ambiance ambiance, BuildStepInfo buildStepInfo, StepInputPackage inputPackage, PassThroughData passThroughData) {
    try {
      K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
          ambiance, SweepingOutputRefObject.builder().name(ContextElement.podDetails).build());
      final String namespace = k8PodDetails.getNamespace();
      final String clusterName = k8PodDetails.getClusterName();

      List<String> commandList =
          buildStepInfo.getScriptInfos().stream().map(ScriptInfo::getScriptString).collect(toList());

      // TODO only k8 cluster is supported
      K8ExecCommandParams k8ExecCommandParams = K8ExecCommandParams.builder()
                                                    .containerName(CONTAINER_NAME)
                                                    .mountPath(MOUNT_PATH)
                                                    .relStdoutFilePath(REL_STDOUT_FILE_PATH)
                                                    .relStderrFilePath(REL_STDERR_FILE_PATH)
                                                    .commandTimeoutSecs(buildStepInfo.getTimeout())
                                                    .scriptType(ShellScriptType.DASH)
                                                    .commands(commandList)
                                                    .namespace(namespace)
                                                    .build();

      NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);
      ConnectorDetails connectorDetails = connectorUtils.getConnectorDetails(ngAccess, clusterName);
      K8ExecuteCommandTaskParams k8ExecuteCommandTaskParams = K8ExecuteCommandTaskParams.builder()
                                                                  .k8sConnector(connectorDetails)
                                                                  .k8ExecCommandParams(k8ExecCommandParams)
                                                                  .build();

      DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                    .accountId(ngAccess.getAccountIdentifier())
                                                    .taskSetupAbstractions(ambiance.getSetupAbstractions())
                                                    .executionTimeout(Duration.ofSeconds(buildStepInfo.getTimeout()))
                                                    .taskType(TaskType.EXECUTE_COMMAND.name())
                                                    .taskParameters(k8ExecuteCommandTaskParams)
                                                    .taskDescription("Execute command task")
                                                    .build();

      logger.info("Sending execute command task");
      K8sTaskExecutionResponse k8sTaskExecutionResponse =
          (K8sTaskExecutionResponse) delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
      if (k8sTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
        logger.info("Execute command task completed successfully");
        return StepResponse.builder().status(Status.SUCCEEDED).build();
      } else {
        logger.error(
            "Execute command task completed with status {}", k8sTaskExecutionResponse.getCommandExecutionStatus());
        return StepResponse.builder().status(Status.FAILED).build();
      }
    } catch (Exception e) {
      logger.error("Execute command task errored", e);
      return StepResponse.builder().status(Status.ERRORED).build();
    }
  }
}
