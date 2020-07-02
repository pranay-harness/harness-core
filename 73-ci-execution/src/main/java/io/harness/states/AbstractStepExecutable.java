package io.harness.states;

import static software.wings.common.CICommonPodConstants.CONTAINER_NAME;
import static software.wings.common.CICommonPodConstants.MOUNT_PATH;
import static software.wings.common.CICommonPodConstants.REL_STDERR_FILE_PATH;
import static software.wings.common.CICommonPodConstants.REL_STDOUT_FILE_PATH;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.managerclient.ManagerCIResource;
import io.harness.network.SafeHttpCall;
import io.harness.references.SweepingOutputRefObject;
import io.harness.state.Step;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ci.K8ExecCommandParams;
import software.wings.beans.ci.ShellScriptType;

import java.util.List;

@Slf4j
public abstract class AbstractStepExecutable implements Step, SyncExecutable {
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private ManagerCIResource managerCIResource;

  @Override
  public StepResponse executeSync(Ambiance ambiance, StepParameters stepParameters, StepInputPackage inputPackage,
      PassThroughData passThroughData) {
    try {
      K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
          ambiance, SweepingOutputRefObject.builder().name(ContextElement.podDetails).build());
      final String namespace = k8PodDetails.getNamespace();
      final String clusterName = k8PodDetails.getClusterName();
      final String podName = k8PodDetails.getPodName();

      CIStepInfo ciStepInfo = (CIStepInfo) stepParameters;
      K8ExecCommandParams k8ExecCommandParams =
          K8ExecCommandParams.builder()
              .podName(podName)
              .containerName(CONTAINER_NAME)
              .mountPath(MOUNT_PATH)
              .relStdoutFilePath(REL_STDOUT_FILE_PATH + "-" + ciStepInfo.getIdentifier())
              .relStderrFilePath(REL_STDERR_FILE_PATH + "-" + ciStepInfo.getIdentifier())
              .commandTimeoutSecs(ciStepInfo.getTimeout())
              .scriptType(ShellScriptType.DASH)
              .commands(getExecCommand(ciStepInfo))
              .namespace(namespace)
              .build();

      SafeHttpCall.execute(managerCIResource.podCommandExecutionTask(clusterName, k8ExecCommandParams));

      return StepResponse.builder().status(Status.SUCCEEDED).build();
    } catch (Exception e) {
      logger.error("state execution failed", e);
      return StepResponse.builder().status(Status.FAILED).build();
    }
  }

  protected abstract List<String> getExecCommand(CIStepInfo ciStepInfo);
}
