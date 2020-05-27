package io.harness.cdng.infra.steps;

import io.harness.ambiance.Ambiance;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.core.section.SectionStepParameters;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;

import java.util.List;
import java.util.Map;

public class InfrastructureSectionStep implements Step, ChildExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("INFRASTRUCTURE_SECTION").build();

  @Override
  public StepType getType() {
    return STEP_TYPE;
  }

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs) {
    SectionStepParameters sectionStepParameters = (SectionStepParameters) stepParameters;
    return ChildExecutableResponse.builder().childNodeId(sectionStepParameters.getChildNodeId()).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    return null;
  }
}
