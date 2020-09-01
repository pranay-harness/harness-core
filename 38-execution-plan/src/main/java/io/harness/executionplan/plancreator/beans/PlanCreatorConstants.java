package io.harness.executionplan.plancreator.beans;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PlanCreatorConstants {
  public final String INFRA_SECTION_NODE_IDENTIFIER = "infrastructure";
  public final String STAGES_NODE_IDENTIFIER = "stages";
  public final String EXECUTION_NODE_IDENTIFIER = "execution";
  public final String EXECUTION_ROLLBACK_NODE_IDENTIFIER = "executionRollback";
  public final String STEP_GROUPS_ROLLBACK_NODE_IDENTIFIER = "stepGroupsRollback";
  public final String PARALLEL_STEP_GROUPS_ROLLBACK_NODE_IDENTIFIER = "parallelStepGroupsRollback";
}
