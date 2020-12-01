package io.harness.cdng.pipeline.steps;

import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.cdng.pipeline.beans.CDPipelineSetupParameters;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.Status;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.steps.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.tasks.ResponseData;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PipelineSetupStep
    implements SyncExecutable<CDPipelineSetupParameters>, ChildExecutable<CDPipelineSetupParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.PIPELINE_SETUP.getName()).build();

  @Override
  public Class<CDPipelineSetupParameters> getStepParametersClass() {
    return CDPipelineSetupParameters.class;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, CDPipelineSetupParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
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
