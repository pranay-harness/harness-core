package io.harness.cdng.k8s;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.executionplan.CDStepDependencyKey;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.stepsdependency.utils.CDStepDependencyUtils;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.executionplan.stepsdependency.bean.KeyAwareStepDependencySpec;
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
@JsonTypeName("k8sRollingRollback")
public class K8sRollingRollbackStepInfo implements CDStepInfo {
  private String displayName;
  private String identifier;
  private K8sRollingRollbackStepParameters k8sRollingRollback;

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public StepType getStepType() {
    return K8sRollingRollbackStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return FacilitatorType.TASK;
  }

  @Override
  public Map<String, StepDependencySpec> getInputStepDependencyList(CreateExecutionPlanContext context) {
    KeyAwareStepDependencySpec infraSpec =
        KeyAwareStepDependencySpec.builder().key(CDStepDependencyUtils.getInfraKey(context)).build();
    KeyAwareStepDependencySpec k8sRollingSpec =
        KeyAwareStepDependencySpec.builder()
            .key(ParentPathInfoUtils.getParentPath(context) + "." + CDStepDependencyKey.K8S_ROLL_OUT.name())
            .build();
    k8sRollingRollback.setStepDependencySpecs(new HashMap<>());
    k8sRollingRollback.getStepDependencySpecs().put(CDStepDependencyKey.INFRASTRUCTURE.name(), infraSpec);
    k8sRollingRollback.getStepDependencySpecs().put(CDStepDependencyKey.K8S_ROLL_OUT.name(), k8sRollingSpec);
    return k8sRollingRollback.getStepDependencySpecs();
  }

  @NotNull
  @Override
  public String getIdentifier() {
    return identifier;
  }
}
