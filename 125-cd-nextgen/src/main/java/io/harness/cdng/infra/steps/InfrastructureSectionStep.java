package io.harness.cdng.infra.steps;

import io.harness.ambiance.Ambiance;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.pms.execution.Status;
import io.harness.pms.steps.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
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
    return ChildExecutableResponse.builder().childNodeId(sectionStepParameters.getChildNodeId()).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, SectionStepParameters sectionStepParameters, Map<String, ResponseData> responseDataMap) {
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
