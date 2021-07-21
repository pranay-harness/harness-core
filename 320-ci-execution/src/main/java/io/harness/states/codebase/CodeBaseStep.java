package io.harness.states.codebase;

import static io.harness.beans.sweepingoutputs.CISweepingOutputNames.CODEBASE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.beans.TaskType.SCM_GIT_REF_TASK;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.execution.ManualExecutionSource;
import io.harness.beans.sweepingoutputs.CodebaseSweepingOutput;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.task.scm.GitRefType;
import io.harness.delegate.task.scm.ScmGitRefTaskParams;
import io.harness.delegate.task.scm.ScmGitRefTaskResponseData;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.product.ci.scm.proto.FindPRResponse;
import io.harness.product.ci.scm.proto.PullRequest;
import io.harness.serializer.KryoSerializer;
import io.harness.stateutils.buildstate.ConnectorUtils;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CodeBaseStep implements TaskExecutable<CodeBaseStepParameters, ScmGitRefTaskResponseData> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType("CICODEBASE").setStepCategory(StepCategory.STEP).build();

  @Inject private KryoSerializer kryoSerializer;
  @Inject private ConnectorUtils connectorUtils;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;

  @Override
  public Class<CodeBaseStepParameters> getStepParametersClass() {
    return CodeBaseStepParameters.class;
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, CodeBaseStepParameters stepParameters, StepInputPackage inputPackage) {
    ExecutionSource executionSource = stepParameters.getExecutionSource();
    String prNumber = null;
    if (executionSource.getType() == ExecutionSource.Type.MANUAL) {
      ManualExecutionSource manualExecutionSource = (ManualExecutionSource) executionSource;
      prNumber = manualExecutionSource.getPrNumber();
    }

    if (isEmpty(prNumber)) {
      throw new CIStageExecutionException("PR number should not be empty");
    }

    ConnectorDetails connectorDetails =
        connectorUtils.getConnectorDetails(AmbianceUtils.getNgAccess(ambiance), stepParameters.getConnectorRef());

    ScmGitRefTaskParams scmGitRefTaskParams = ScmGitRefTaskParams.builder()
                                                  .prNumber(Long.parseLong(prNumber))
                                                  .gitRefType(GitRefType.PULL_REQUEST)
                                                  .encryptedDataDetails(connectorDetails.getEncryptedDataDetails())
                                                  .scmConnector((ScmConnector) connectorDetails.getConnectorConfig())
                                                  .build();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(30 * 1000L)
                                  .taskType(SCM_GIT_REF_TASK.name())
                                  .parameters(new Object[] {scmGitRefTaskParams})
                                  .build();

    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer);
  }

  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, CodeBaseStepParameters stepParameters,
      ThrowingSupplier<ScmGitRefTaskResponseData> responseDataSupplier) throws Exception {
    ScmGitRefTaskResponseData scmGitRefTaskResponseData = responseDataSupplier.get();
    FindPRResponse findPRResponse = FindPRResponse.parseFrom(scmGitRefTaskResponseData.getFindPRResponse());
    PullRequest pr = findPRResponse.getPr();

    CodebaseSweepingOutput pullReqSweepingOutput = CodebaseSweepingOutput.builder()
                                                       .commitBranch(pr.getTarget())
                                                       .commitBefore(pr.getBase().getSha())
                                                       .commitRef(pr.getRef())
                                                       .commitSha(pr.getSha())
                                                       .build();
    OptionalSweepingOutput optionalSweepingOutput =
        executionSweepingOutputResolver.resolveOptional(ambiance, RefObjectUtils.getOutcomeRefObject(CODEBASE));
    if (!optionalSweepingOutput.isFound()) {
      try {
        executionSweepingOutputResolver.consume(
            ambiance, CODEBASE, pullReqSweepingOutput, StepOutcomeGroup.PIPELINE.name());
      } catch (Exception e) {
        log.error("Error while consuming codebase sweeping output", e);
      }
    }
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
