package io.harness.cdng.k8s;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.ParameterField;
import io.harness.cdng.executionplan.CDStepDependencyKey;
import io.harness.cdng.executionplan.utils.PlanCreatorFacilitatorUtils;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.pipeline.stepinfo.StepSpecTypeConstants;
import io.harness.cdng.stepsdependency.utils.CDStepDependencyUtils;
import io.harness.cdng.visitor.LevelNodeQualifierName;
import io.harness.cdng.visitor.helpers.cdstepinfo.K8sRollingRollbackStepInfoVisitorHelper;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.executionplan.stepsdependency.bean.KeyAwareStepDependencySpec;
import io.harness.executionplan.utils.ParentPathInfoUtils;
import io.harness.state.StepType;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.K8S_ROLLING_ROLLBACK)
@SimpleVisitorHelper(helperClass = K8sRollingRollbackStepInfoVisitorHelper.class)
public class K8sRollingRollbackStepInfo extends K8sRollingRollbackStepParameters implements CDStepInfo, Visitable {
  @JsonIgnore private String name;
  @JsonIgnore private String identifier;

  // For Visitor Framework Impl
  String metadata;

  @Builder(builderMethodName = "infoBuilder")
  public K8sRollingRollbackStepInfo(ParameterField<String> timeout, Map<String, StepDependencySpec> stepDependencySpecs,
      String name, String identifier) {
    super(timeout, stepDependencySpecs);
    this.name = name;
    this.identifier = identifier;
  }

  @Override
  public String getDisplayName() {
    return name;
  }

  @Override
  public StepType getStepType() {
    return K8sRollingRollbackStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return PlanCreatorFacilitatorUtils.decideTaskFacilitatorType();
  }

  @Override
  public Map<String, StepDependencySpec> getInputStepDependencyList(ExecutionPlanCreationContext context) {
    KeyAwareStepDependencySpec infraSpec =
        KeyAwareStepDependencySpec.builder().key(CDStepDependencyUtils.getInfraKey(context)).build();
    KeyAwareStepDependencySpec k8sRollingSpec =
        KeyAwareStepDependencySpec.builder()
            .key(ParentPathInfoUtils.getParentPath(context) + "." + CDStepDependencyKey.K8S_ROLL_OUT.name())
            .build();
    setStepDependencySpecs(new HashMap<>());
    getStepDependencySpecs().put(CDStepDependencyKey.INFRASTRUCTURE.name(), infraSpec);
    getStepDependencySpecs().put(CDStepDependencyKey.K8S_ROLL_OUT.name(), k8sRollingSpec);
    return getStepDependencySpecs();
  }

  @NotNull
  @Override
  public String getIdentifier() {
    return identifier;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(LevelNodeQualifierName.K8S_ROLLING_ROLLBACK).build();
  }
}
