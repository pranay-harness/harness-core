package io.harness.states;

import static io.harness.common.CICommonPodConstants.CONTAINER_NAME;
import static io.harness.common.CICommonPodConstants.MOUNT_PATH;
import static io.harness.common.CICommonPodConstants.REL_STDERR_FILE_PATH;
import static io.harness.common.CICommonPodConstants.REL_STDOUT_FILE_PATH;

import static java.util.stream.Collectors.toList;

import io.harness.ambiance.Ambiance;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.script.ScriptInfo;
import io.harness.beans.steps.stepinfo.BuildStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.delegate.beans.ci.K8ExecCommandParams;
import io.harness.delegate.beans.ci.K8ExecuteCommandTaskParams;
import io.harness.delegate.beans.ci.ShellScriptType;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ng.core.NGAccess;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.execution.Status;
import io.harness.pms.steps.StepType;
import io.harness.refObjects.RefObjectUtil;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.stateutils.buildstate.ConnectorUtils;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * This state will execute build command on already setup pod. It will send customer defined commands.
 * Currently it assumes a timeout of 60 minutes
 */

@Slf4j
public class BuildStep implements SyncExecutable<BuildStepInfo> {
  public static final StepType STEP_TYPE = BuildStepInfo.typeInfo.getStepType();
  public static final String TASK_TYPE = "EXECUTE_COMMAND";
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private ConnectorUtils connectorUtils;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  @Override
  public Class<BuildStepInfo> getStepParametersClass() {
    return BuildStepInfo.class;
  }

  // TODO Async can not be supported at this point. We have to build polling framework on CI manager.
  //     Async will be supported once we will have delegate microservice ready.

  @Override
  public StepResponse executeSync(
      Ambiance ambiance, BuildStepInfo buildStepInfo, StepInputPackage inputPackage, PassThroughData passThroughData) {
    try {
      K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
          ambiance, RefObjectUtil.getSweepingOutputRefObject(ContextElement.podDetails));
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
                                                    .taskType(TASK_TYPE)
                                                    .taskParameters(k8ExecuteCommandTaskParams)
                                                    .taskDescription("Execute command task")
                                                    .build();

      log.info("Sending execute command task");
      K8sTaskExecutionResponse k8sTaskExecutionResponse =
          (K8sTaskExecutionResponse) delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
      if (k8sTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
        log.info("Execute command task completed successfully");
        return StepResponse.builder().status(Status.SUCCEEDED).build();
      } else {
        log.error(
            "Execute command task completed with status {}", k8sTaskExecutionResponse.getCommandExecutionStatus());
        return StepResponse.builder().status(Status.FAILED).build();
      }
    } catch (Exception e) {
      log.error("Execute command task errored", e);
      return StepResponse.builder().status(Status.ERRORED).build();
    }
  }
}
