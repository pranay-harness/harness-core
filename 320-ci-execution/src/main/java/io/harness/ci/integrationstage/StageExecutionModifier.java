package io.harness.ci.integrationstage;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.yaml.extended.ci.codebase.CodeBase;

/**
 * Modifies saved integration stage execution plan by appending pre and post execution steps or any custom steps
 */
@OwnedBy(CI)
public interface StageExecutionModifier {
  /**
   * Modifies saved integration stage execution plan by appending pre and post execution steps or any custom steps.
   * Additional steps may include cleanup or setup step and other operations that are needed for complete execution.
   * @param execution Execution object that holds current steps
   * @param stageConfig StageElementConfig object that holds info
   * @return modified execution
   */
  ExecutionElementConfig modifyExecutionPlan(ExecutionElementConfig execution, StageElementConfig stageConfig,
      PlanCreationContext context, String podName, CodeBase ciCodeBase, Infrastructure infrastructure,
      ExecutionSource executionSource);
}
