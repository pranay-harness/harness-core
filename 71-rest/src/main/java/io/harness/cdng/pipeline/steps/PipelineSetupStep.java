package io.harness.cdng.pipeline.steps;

import static io.harness.cdng.orchestration.StepUtils.createStepResponseFromChildResponse;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.pipeline.beans.CDPipelineSetupParameters;
import io.harness.delegate.beans.ResponseData;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.state.io.StepTransput;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class PipelineSetupStep implements Step, SyncExecutable, ChildExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("PIPELINE_SETUP").build();

  @Override
  public StepResponse executeSync(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs, PassThroughData passThroughData) {
    CDPipelineSetupParameters parameters = (CDPipelineSetupParameters) stepParameters;
    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(StepOutcome.builder()
                         .name("service")
                         .outcome(((DeploymentStage) parameters.getCdPipeline().getStages().get(0)).getService())
                         .build())
        .stepOutcome(StepOutcome.builder()
                         .name("infraDefinition")
                         .outcome(((DeploymentStage) parameters.getCdPipeline().getStages().get(0))
                                      .getInfrastructure()
                                      .getInfraDefinition())
                         .build())
        .build();
  }

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs) {
    CDPipelineSetupParameters parameters = (CDPipelineSetupParameters) stepParameters;
    logger.info("starting execution for pipeline [{}]", parameters);

    final Map<String, String> fieldToExecutionNodeIdMap = parameters.getFieldToExecutionNodeIdMap();
    final String stagesNodeId = fieldToExecutionNodeIdMap.get("stages");
    return ChildExecutableResponse.builder().childNodeId(stagesNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    final CDPipelineSetupParameters parameters = (CDPipelineSetupParameters) stepParameters;

    logger.info("executed pipeline =[{}]", parameters);

    return createStepResponseFromChildResponse(responseDataMap);
  }
}
