package io.harness.states;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.pms.sdk.core.resolver.ResolverUtils.GLOBAL_GROUP_SCOPE;
import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CIPipelineSetupParameters;
import io.harness.ci.stdvars.BuildStandardVariables;
import io.harness.ci.utils.CIPipelineStandardVariablesUtils;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CI)
public class CIPipelineSetupStep
    implements ChildExecutable<CIPipelineSetupParameters>, SyncExecutable<CIPipelineSetupParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType("CI_PIPELINE_SETUP").setStepCategory(StepCategory.STEP).build();
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
    return ChildExecutableResponse.newBuilder().setChildNodeId(stagesNodeId).build();
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
