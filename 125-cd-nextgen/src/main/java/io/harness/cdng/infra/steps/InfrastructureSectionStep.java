package io.harness.cdng.infra.steps;

import io.harness.executions.steps.ExecutionNodeType;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ChildExecutableResponse;
import io.harness.pms.execution.Status;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.steps.StepType;
import io.harness.steps.section.SectionStepParameters;
import io.harness.tasks.ResponseData;

import java.util.Map;

public class InfrastructureSectionStep implements ChildExecutable<SectionStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.INFRASTRUCTURE_SECTION.getName()).build();

  @Override
  public Class<SectionStepParameters> getStepParametersClass() {
    return SectionStepParameters.class;
  }

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, SectionStepParameters sectionStepParameters, StepInputPackage inputPackage) {
    return ChildExecutableResponse.newBuilder().setChildNodeId(sectionStepParameters.getChildNodeId()).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, SectionStepParameters sectionStepParameters, Map<String, ResponseData> responseDataMap) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
