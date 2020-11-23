package io.harness.integrationstage;

import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.intfc.StageType;

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
  ExecutionElement modifyExecutionPlan(
      ExecutionElement execution, StageType stageType, ExecutionPlanCreationContext context, String podName);
}
