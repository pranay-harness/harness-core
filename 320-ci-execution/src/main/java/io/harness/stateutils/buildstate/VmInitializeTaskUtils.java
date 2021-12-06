package io.harness.stateutils.buildstate;

import static io.harness.beans.sweepingoutputs.StageInfraDetails.STAGE_INFRA_DETAILS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.stepinfo.InitializeStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.StageDetails;
import io.harness.beans.sweepingoutputs.VmStageInfraDetails;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.VmInfraYaml;
import io.harness.delegate.beans.ci.vm.CIVmInitializeTaskParams;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class VmInitializeTaskUtils {
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;

  public CIVmInitializeTaskParams getInitializeTaskParams(
      InitializeStepInfo initializeStepInfo, Ambiance ambiance, String logPrefix) {
    Infrastructure infrastructure = initializeStepInfo.getInfrastructure();

    if (infrastructure == null || ((VmInfraYaml) infrastructure).getSpec() == null) {
      throw new CIStageExecutionException("Input infrastructure can not be empty");
    }

    VmInfraYaml awsVmInfraYaml = (VmInfraYaml) infrastructure;
    executionSweepingOutputResolver.consume(ambiance, STAGE_INFRA_DETAILS,
        VmStageInfraDetails.builder().poolId(awsVmInfraYaml.getSpec().getPoolId()).build(),
        StepOutcomeGroup.STAGE.name());

    OptionalSweepingOutput optionalSweepingOutput = executionSweepingOutputResolver.resolveOptional(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(ContextElement.stageDetails));
    if (!optionalSweepingOutput.isFound()) {
      throw new CIStageExecutionException("Stage details sweeping output cannot be empty");
    }

    StageDetails stageDetails = (StageDetails) optionalSweepingOutput.getOutput();
    return CIVmInitializeTaskParams.builder().stageRuntimeId(stageDetails.getStageRuntimeID()).build();
  }
}
