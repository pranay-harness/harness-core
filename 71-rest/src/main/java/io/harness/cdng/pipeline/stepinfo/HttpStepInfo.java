package io.harness.cdng.pipeline.stepinfo;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.executionplan.utils.PlanCreatorFacilitatorUtils;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.redesign.states.http.BasicHttpStep;
import io.harness.redesign.states.http.BasicHttpStepParameters;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("http")
public class HttpStepInfo implements CDStepInfo {
  String displayName;
  String type;
  String identifier;
  BasicHttpStepParameters http;

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public StepType getStepType() {
    return BasicHttpStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return PlanCreatorFacilitatorUtils.decideTaskFacilitatorType();
  }

  @Override
  public StepParameters getStepParameters() {
    return http;
  }
}
