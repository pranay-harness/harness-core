package io.harness.ci.integrationstage;

import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.yaml.extended.ci.codebase.CodeBase;

/**
 * Modifies saved integration stage execution plan by appending pre and post execution steps or any custom steps
 */

public interface StageExecutionModifier {
  /**
   * Modifies saved integration stage execution plan by appending pre and post execution steps or any custom steps.
   * Additional steps may include cleanup or setup step and other operations that are needed for complete execution.
   * @param execution Execution object that holds current steps
   * @param stageType StageType object that holds info
   * @return modified execution
   */
  ExecutionElementConfig modifyExecutionPlan(ExecutionElementConfig execution, StageElementConfig stageType,
      PlanCreationContext context, String podName, CodeBase ciCodeBase);
}
