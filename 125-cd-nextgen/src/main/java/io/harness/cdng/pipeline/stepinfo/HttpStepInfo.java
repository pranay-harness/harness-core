package io.harness.cdng.pipeline.stepinfo;

import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.pipeline.steps.HttpStep;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.cdstepinfo.HttpStepInfoVisitorHelper;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.steps.StepType;
import io.harness.redesign.states.http.BasicHttpStepParameters;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.HTTP)
@SimpleVisitorHelper(helperClass = HttpStepInfoVisitorHelper.class)
@TypeAlias("httpStepInfo")
public class HttpStepInfo extends BasicHttpStepParameters implements CDStepInfo, Visitable {
  @JsonIgnore String name;
  @JsonIgnore String identifier;

  // For Visitor Framework Impl
  String metadata;

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
    return HttpStep.STEP_TYPE;
  }

  @Override
  @JsonIgnore
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK_V3;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    return VisitableChildren.builder().build();
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.HTTP_STEP).build();
  }
}
