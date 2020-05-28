package io.harness.cdng.pipeline.steps;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.pipeline.beans.CDPipelineSetupParameters;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;

import java.util.List;

public class PipelineSetupStep implements Step, SyncExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("PIPELINE_SETUP").build();

  @Override
  public StepResponse executeSync(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs, PassThroughData passThroughData) {
    CDPipelineSetupParameters parameters = (CDPipelineSetupParameters) stepParameters;
    return StepResponse.builder()
        .status(NodeExecutionStatus.SUCCEEDED)
        .outcome("service", parameters.getCdPipeline().getStages().get(0).getService())
        .outcome(
            "infraDefinition", parameters.getCdPipeline().getStages().get(0).getInfrastructure().getInfraDefinition())
        .build();
  }
}
