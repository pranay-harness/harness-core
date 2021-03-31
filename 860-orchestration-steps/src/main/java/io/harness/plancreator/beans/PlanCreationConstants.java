package io.harness.plancreator.beans;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PlanCreationConstants {
  public final String STAGES_NODE_IDENTIFIER = "stages";
  public final String PIPELINE_NODE_IDENTIFIER = "pipeline";
  public final String EXECUTION_NODE_IDENTIFIER = "execution";
  public final String EXECUTION_NODE_NAME = "Execution";
  public final String ROLLBACK_NODE_NAME = "Rollback";
  public final String INFRA_ROLLBACK_NODE_NAME = "Infrastructure Rollback";
  public final String INFRA_ROLLBACK_NODE_IDENTIFIER = "infrastructureRollback";
  public static final String STEP_GROUPS_ROLLBACK_NODE_ID_PREFIX = "_stepGrouprollback";
  public static final String ROLLBACK_STEPS_NODE_ID_PREFIX = "_rollbackSteps";
}
