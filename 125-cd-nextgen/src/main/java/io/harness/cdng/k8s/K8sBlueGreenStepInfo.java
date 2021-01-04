package io.harness.cdng.k8s;

import io.harness.cdng.executionplan.CDStepDependencyKey;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.cdng.stepsdependency.utils.CDStepDependencyUtils;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.cdstepinfo.K8sBlueGreenStepInfoVisitorHelper;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.executionplan.stepsdependency.bean.KeyAwareStepDependencySpec;
import io.harness.executionplan.stepsdependency.instructors.OutcomeRefStepDependencyInstructor;
import io.harness.executionplan.utils.ParentPathInfoUtils;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.K8S_BLUE_GREEN_DEPLOY)
@SimpleVisitorHelper(helperClass = K8sBlueGreenStepInfoVisitorHelper.class)
@TypeAlias("k8sBlueGreenStepInfo")
public class K8sBlueGreenStepInfo extends K8sBlueGreenStepParameters implements CDStepInfo, Visitable {
  @JsonIgnore private String name;
  @JsonIgnore private String identifier;

  // For Visitor Framework Impl
  String metadata;

  @Builder(builderMethodName = "infoBuilder")
  public K8sBlueGreenStepInfo(ParameterField<String> timeout, ParameterField<Boolean> skipDryRun,
      Map<String, StepDependencySpec> stepDependencySpecs, String name, String identifier) {
    super(timeout, skipDryRun, stepDependencySpecs);
    this.name = name;
    this.identifier = identifier;
  }

  public K8sBlueGreenStepInfo(String name, String identifier) {
    this.name = name;
    this.identifier = identifier;
  }

  @Override
  public String getDisplayName() {
    return name;
  }

  @Override
  public StepType getStepType() {
    return K8sBlueGreenStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK_CHAIN;
  }

  @Override
  public Map<String, StepDependencySpec> getInputStepDependencyList(ExecutionPlanCreationContext context) {
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
      StepDependencyService stepDependencyService, ExecutionPlanCreationContext context, String nodeId) {
    OutcomeRefStepDependencyInstructor instructor =
        OutcomeRefStepDependencyInstructor.builder()
            .key(ParentPathInfoUtils.getParentPath(context) + "." + CDStepDependencyKey.K8S_BLUE_GREEN.name())
            .providerPlanNodeId(nodeId)
            .outcomeExpression(OutcomeExpressionConstants.K8S_BLUE_GREEN_OUTCOME)
            .build();
    stepDependencyService.registerStepDependencyInstructor(instructor, context);
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.K8S_BLUE_GREEN_DEPLOY).build();
  }
}
