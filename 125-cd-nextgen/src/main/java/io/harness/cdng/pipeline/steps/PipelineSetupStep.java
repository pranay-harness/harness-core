package io.harness.cdng.pipeline.steps;

import static io.harness.ngpipeline.orchestration.StepUtils.createStepResponseFromChildResponse;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.pipeline.beans.CDPipelineSetupParameters;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.pms.execution.Status;
import io.harness.pms.steps.StepType;
import io.harness.state.Step;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.tasks.ResponseData;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class PipelineSetupStep implements Step, SyncExecutable, ChildExecutable<CDPipelineSetupParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.PIPELINE_SETUP.getName()).build();

  @Override
  public StepResponse executeSync(Ambiance ambiance, StepParameters stepParameters, StepInputPackage inputPackage,
      PassThroughData passThroughData) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, CDPipelineSetupParameters cdPipelineSetupParameters, StepInputPackage inputPackage) {
    log.info("starting execution for pipeline [{}]", cdPipelineSetupParameters);

    final Map<String, String> fieldToExecutionNodeIdMap = cdPipelineSetupParameters.getFieldToExecutionNodeIdMap();
    final String stagesNodeId = fieldToExecutionNodeIdMap.get("stages");
    return ChildExecutableResponse.builder().childNodeId(stagesNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(Ambiance ambiance, CDPipelineSetupParameters cdPipelineSetupParameters,
      Map<String, ResponseData> responseDataMap) {
    log.info("executed pipeline =[{}]", cdPipelineSetupParameters);

    return createStepResponseFromChildResponse(responseDataMap);
  }
}
