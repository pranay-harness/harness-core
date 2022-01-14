package io.harness.plancreator.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CDC)
public class OrchestrationConstants {
  public final String EXECUTION_NODE_IDENTIFIER = "execution";
  public final String EXECUTION_NODE_NAME = "Execution";
  public final String ROLLBACK_NODE_NAME = "(Rollback)";
  public final String INFRA_ROLLBACK_NODE_NAME = "Infrastructure (Rollback)";
  public final String INFRA_ROLLBACK_NODE_IDENTIFIER = "infraRollbackSteps";
  public final String STEP_GROUPS_ROLLBACK_NODE_ID_SUFFIX = "_stepGroupsRollback";
  public final String STEP_GROUPS_ROLLBACK_NODE_NAME = "Step Groups (Rollback)";
  public final String ROLLBACK_STEPS_NODE_ID_SUFFIX = "_rollbackSteps";
  public final String ROLLBACK_EXECUTION_NODE_ID_SUFFIX = "_combinedRollback";
  public final String COMBINED_ROLLBACK_ID_SUFFIX = "_combinedRollback";
}
