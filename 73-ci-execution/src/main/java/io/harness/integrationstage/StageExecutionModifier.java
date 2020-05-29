package io.harness.integrationstage;

import io.harness.yaml.core.Execution;
import io.harness.yaml.core.intfc.Stage;

/**
 * Modifies saved integration stage execution plan by appending pre and post execution steps or any custom steps
 */

public interface StageExecutionModifier {
  /**
   * Modifies saved integration stage execution plan by appending pre and post execution steps or any custom steps.
   * Additional steps may include cleanup or setup step and other operations that are needed for complete execution.
   * @param execution Execution object that holds current steps
   * @param stage Stage object that holds info
   * @return modified execution
   */
  Execution modifyExecutionPlan(Execution execution, Stage stage);
}
