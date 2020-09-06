package io.harness.cdng.infra.steps;

import io.harness.ambiance.Ambiance;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.steps.section.SectionStepParameters;
import io.harness.tasks.ResponseData;

import java.util.Map;

public class InfrastructureSectionStep implements Step, ChildExecutable<SectionStepParameters> {
  public static final StepType STEP_TYPE = StepType.builder().type("INFRASTRUCTURE_SECTION").build();

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
