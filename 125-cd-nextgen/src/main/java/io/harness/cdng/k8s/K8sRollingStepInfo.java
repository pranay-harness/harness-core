package io.harness.cdng.k8s;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.executionplan.CDStepDependencyKey;
import io.harness.cdng.executionplan.utils.PlanCreatorFacilitatorUtils;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.pipeline.stepinfo.StepSpecType;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.cdng.stepsdependency.utils.CDStepDependencyUtils;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.executionplan.stepsdependency.bean.KeyAwareStepDependencySpec;
import io.harness.executionplan.stepsdependency.instructors.OutcomeRefStepDependencyInstructor;
import io.harness.executionplan.utils.ParentPathInfoUtils;
import io.harness.state.StepType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecType.K8S_ROLLOUT_DEPLOY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class K8sRollingStepInfo extends K8sRollingStepParameters implements CDStepInfo {
  @JsonIgnore private String name;
  @JsonIgnore private String identifier;

  @Builder(builderMethodName = "infoBuilder")
  public K8sRollingStepInfo(int timeout, boolean skipDryRun, Map<String, StepDependencySpec> stepDependencySpecs,
      String name, String identifier) {
    super(timeout, skipDryRun, stepDependencySpecs);
    this.name = name;
    this.identifier = identifier;
  }

  @Override
  public String getDisplayName() {
    return name;
  }

  @Override
  public StepType getStepType() {
    return K8sRollingStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return PlanCreatorFacilitatorUtils.decideTaskChainFacilitatorType();
  }

  @Override
  public Map<String, StepDependencySpec> getInputStepDependencyList(CreateExecutionPlanContext context) {
    KeyAwareStepDependencySpec serviceSpec =
        KeyAwareStepDependencySpec.builder().key(CDStepDependencyUtils.getServiceKey(context)).build();
    KeyAwareStepDependencySpec infraSpec =
        KeyAwareStepDependencySpec.builder().key(CDStepDependencyUtils.getInfraKey(context)).build();
    setStepDependencySpecs(new HashMap<>());
    getStepDependencySpecs().put(CDStepDependencyKey.SERVICE.name(), serviceSpec);
    getStepDependencySpecs().put(CDStepDependencyKey.INFRASTRUCTURE.name(), infraSpec);
    return getStepDependencySpecs();
  }

  @Override
  public void registerStepDependencyInstructors(
      StepDependencyService stepDependencyService, CreateExecutionPlanContext context, String nodeId) {
    OutcomeRefStepDependencyInstructor instructor =
        OutcomeRefStepDependencyInstructor.builder()
            .key(ParentPathInfoUtils.getParentPath(context) + "." + CDStepDependencyKey.K8S_ROLL_OUT.name())
            .providerPlanNodeId(nodeId)
            .outcomeExpression(OutcomeExpressionConstants.K8S_ROLL_OUT.getName())
            .build();
    stepDependencyService.registerStepDependencyInstructor(instructor, context);
  }
}
