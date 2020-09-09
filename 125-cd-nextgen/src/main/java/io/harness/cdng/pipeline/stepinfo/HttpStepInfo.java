package io.harness.cdng.pipeline.stepinfo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.executionplan.utils.PlanCreatorFacilitatorUtils;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.visitor.helpers.cdstepinfo.HttpStepInfoVisitorHelper;
import io.harness.redesign.states.http.BasicHttpStep;
import io.harness.redesign.states.http.BasicHttpStepParameters;
import io.harness.state.StepType;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecType.HTTP)
@SimpleVisitorHelper(helperClass = HttpStepInfoVisitorHelper.class)
public class HttpStepInfo extends BasicHttpStepParameters implements CDStepInfo {
  @JsonIgnore String name;
  @JsonIgnore String identifier;

  @Builder(builderMethodName = "infoBuilder")
  public HttpStepInfo(String url, String method, String header, String body, String assertion, int socketTimeoutMillis,
      String name, String identifier) {
    super(url, method, header, body, assertion, socketTimeoutMillis);
    this.name = name;
    this.identifier = identifier;
  }

  @Override
  public String getDisplayName() {
    return name;
  }

  @Override
  @JsonIgnore
  public StepType getStepType() {
    return BasicHttpStep.STEP_TYPE;
  }

  @Override
  @JsonIgnore
  public String getFacilitatorType() {
    return PlanCreatorFacilitatorUtils.decideTaskFacilitatorType();
  }
}
