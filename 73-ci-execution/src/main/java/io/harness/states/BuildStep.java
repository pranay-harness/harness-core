package io.harness.states;

import static java.util.stream.Collectors.toList;
import static software.wings.common.CICommonPodConstants.CLUSTER_EXPRESSION;
import static software.wings.common.CICommonPodConstants.CONTAINER_NAME;
import static software.wings.common.CICommonPodConstants.MOUNT_PATH;
import static software.wings.common.CICommonPodConstants.NAMESPACE_EXPRESSION;
import static software.wings.common.CICommonPodConstants.POD_NAME_EXPRESSION;
import static software.wings.common.CICommonPodConstants.REL_STDERR_FILE_PATH;
import static software.wings.common.CICommonPodConstants.REL_STDOUT_FILE_PATH;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Produces;
import io.harness.beans.script.ScriptInfo;
import io.harness.beans.steps.stepinfo.BuildStepInfo;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.managerclient.ManagerCIResource;
import io.harness.network.SafeHttpCall;
import io.harness.state.Step;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ci.K8ExecCommandParams;
import software.wings.beans.ci.ShellScriptType;

import java.util.List;

/**
 * This state will execute build command on already setup pod. It will send customer defined commands.
 * Currently it assumes a timeout of 60 minutes
 */

@Produces(Step.class)
@Slf4j
public class BuildStep implements Step, SyncExecutable {
  @Inject private ManagerCIResource managerCIResource;
  @Inject EngineExpressionService engineExpressionService;
  // TODO Async can not be supported at this point. We have to build polling framework on CI manager.
  //     Async will be supported once we will have delegate microservice ready.

  @Override
  public StepResponse executeSync(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs, PassThroughData passThroughData) {
    try {
      final String clusterName = engineExpressionService.renderExpression(ambiance, CLUSTER_EXPRESSION);
      final String namespace = engineExpressionService.renderExpression(ambiance, NAMESPACE_EXPRESSION);
      final String podName = engineExpressionService.renderExpression(ambiance, POD_NAME_EXPRESSION);

      BuildStepInfo buildStepInfo = (BuildStepInfo) stepParameters;

      List<String> commandList =
          buildStepInfo.getScriptInfos().stream().map(ScriptInfo::getScriptString).collect(toList());

      // TODO only k8 cluster is supported
      K8ExecCommandParams k8ExecCommandParams = K8ExecCommandParams.builder()
                                                    .podName(podName)
                                                    .containerName(CONTAINER_NAME)
                                                    .mountPath(MOUNT_PATH)
                                                    .relStdoutFilePath(REL_STDOUT_FILE_PATH)
                                                    .relStderrFilePath(REL_STDERR_FILE_PATH)
                                                    .commandTimeoutSecs(buildStepInfo.getTimeout())
                                                    .scriptType(ShellScriptType.DASH)
                                                    .commands(commandList)
                                                    .namespace(namespace)
                                                    .build();

      // TODO Use k8 connector from element input and, handle response

      SafeHttpCall.execute(managerCIResource.podCommandExecutionTask(clusterName, k8ExecCommandParams));

      return StepResponse.builder().status(Status.SUCCEEDED).build();
    } catch (Exception e) {
      logger.error("state execution failed", e);
    }

    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
