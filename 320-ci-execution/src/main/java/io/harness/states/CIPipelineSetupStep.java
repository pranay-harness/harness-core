package io.harness.states;

import static io.harness.ngpipeline.orchestration.StepUtils.createStepResponseFromChildResponse;
import static io.harness.resolvers.ResolverUtils.GLOBAL_GROUP_SCOPE;

import io.harness.ambiance.Ambiance;
import io.harness.beans.CIPipelineSetupParameters;
import io.harness.ci.stdvars.BuildStandardVariables;
import io.harness.ci.utils.CIPipelineStandardVariablesUtils;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.pms.execution.Status;
import io.harness.pms.steps.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CIPipelineSetupStep
    implements ChildExecutable<CIPipelineSetupParameters>, SyncExecutable<CIPipelineSetupParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType("CI_PIPELINE_SETUP").build();
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;

  @Override
  public Class<CIPipelineSetupParameters> getStepParametersClass() {
    return CIPipelineSetupParameters.class;
  }

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, CIPipelineSetupParameters ciPipelineSetupParameters, StepInputPackage inputPackage) {
    log.info("starting execution for ci pipeline [{}]", ciPipelineSetupParameters);

    BuildStandardVariables buildStandardVariables =
        CIPipelineStandardVariablesUtils.fetchBuildStandardVariables(ciPipelineSetupParameters.getCiExecutionArgs());

    executionSweepingOutputResolver.consume(
        ambiance, BuildStandardVariables.BUILD_VARIABLE, buildStandardVariables, GLOBAL_GROUP_SCOPE);

    final Map<String, String> fieldToExecutionNodeIdMap = ciPipelineSetupParameters.getFieldToExecutionNodeIdMap();
    final String stagesNodeId = fieldToExecutionNodeIdMap.get("stages");
    return ChildExecutableResponse.builder().childNodeId(stagesNodeId).build();
  }

  @Override
  public StepResponse handleChildResponse(Ambiance ambiance, CIPipelineSetupParameters ciPipelineSetupParameters,
      Map<String, ResponseData> responseDataMap) {
    log.info("executed pipeline =[{}]", ciPipelineSetupParameters);

    return createStepResponseFromChildResponse(responseDataMap);
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, CIPipelineSetupParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
