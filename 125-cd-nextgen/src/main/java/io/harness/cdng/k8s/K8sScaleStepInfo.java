package io.harness.cdng.k8s;

import io.harness.cdng.executionplan.CDStepDependencyKey;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.stepsdependency.utils.CDStepDependencyUtils;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.cdstepinfo.K8sScaleStepInfoVisitorHelper;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.executionplan.stepsdependency.bean.KeyAwareStepDependencySpec;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.K8S_SCALE)
@SimpleVisitorHelper(helperClass = K8sScaleStepInfoVisitorHelper.class)
@TypeAlias("k8sScale")
public class K8sScaleStepInfo extends K8sScaleStepParameter implements CDStepInfo, Visitable {
  @JsonIgnore private String name;
  @JsonIgnore private String identifier;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Builder(builderMethodName = "infoBuilder")
  public K8sScaleStepInfo(ParameterField<String> timeout, ParameterField<Boolean> skipDryRun,
      ParameterField<Boolean> skipSteadyStateCheck, InstanceSelectionWrapper instanceSelection,
      ParameterField<String> workload, Map<String, StepDependencySpec> stepDependencySpecs, String name,
      String identifier) {
    super(instanceSelection, workload, timeout, skipDryRun, skipSteadyStateCheck, stepDependencySpecs);
    this.name = name;
    this.identifier = identifier;
  }

  public K8sScaleStepInfo(String name, String identifier) {
    this.name = name;
    this.identifier = identifier;
  }

  @Override
  public String getDisplayName() {
    return name;
  }

  @Override
  public StepType getStepType() {
    return K8sScaleStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public Map<String, StepDependencySpec> getInputStepDependencyList(ExecutionPlanCreationContext context) {
    KeyAwareStepDependencySpec infraSpec =
        KeyAwareStepDependencySpec.builder().key(CDStepDependencyUtils.getInfraKey(context)).build();
    setStepDependencySpecs(new HashMap<>());
    getStepDependencySpecs().put(CDStepDependencyKey.INFRASTRUCTURE.name(), infraSpec);
    return getStepDependencySpecs();
  }

  @NotNull
  @Override
  public String getIdentifier() {
    return identifier;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.K8S_SCALE).build();
  }
}
