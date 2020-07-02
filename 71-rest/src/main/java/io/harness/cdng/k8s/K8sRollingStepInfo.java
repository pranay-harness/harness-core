package io.harness.cdng.k8s;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.executionplan.CDStepDependencyKey;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.cdng.stepsdependency.utils.CDStepDependencyUtils;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.executionplan.stepsdependency.bean.KeyAwareStepDependencySpec;
import io.harness.executionplan.stepsdependency.instructors.OutcomeRefStepDependencyInstructor;
import io.harness.executionplan.utils.ParentPathInfoUtils;
import io.harness.facilitator.FacilitatorType;
import io.harness.state.StepType;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonTypeName("k8sRolling")
public class K8sRollingStepInfo implements CDStepInfo {
  private String displayName;
  private String identifier;
  private K8sRollingStepParameters k8sRolling;

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public StepType getStepType() {
    return K8sRollingStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return FacilitatorType.TASK_CHAIN;
  }

  @Override
  public Map<String, StepDependencySpec> getInputStepDependencyList(CreateExecutionPlanContext context) {
    KeyAwareStepDependencySpec serviceSpec =
        KeyAwareStepDependencySpec.builder().key(CDStepDependencyUtils.getServiceKey(context)).build();
    KeyAwareStepDependencySpec infraSpec =
        KeyAwareStepDependencySpec.builder().key(CDStepDependencyUtils.getInfraKey(context)).build();
    k8sRolling.setStepDependencySpecs(new HashMap<>());
    k8sRolling.getStepDependencySpecs().put(CDStepDependencyKey.SERVICE.name(), serviceSpec);
    k8sRolling.getStepDependencySpecs().put(CDStepDependencyKey.INFRASTRUCTURE.name(), infraSpec);
    return k8sRolling.getStepDependencySpecs();
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

  @NotNull
  @Override
  public String getIdentifier() {
    return identifier;
  }
}
