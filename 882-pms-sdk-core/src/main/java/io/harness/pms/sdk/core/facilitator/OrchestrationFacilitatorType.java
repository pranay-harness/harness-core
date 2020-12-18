package io.harness.pms.sdk.core.facilitator;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class OrchestrationFacilitatorType {
  // Provided From the orchestration layer system facilitators
  public static final String SYNC = "SYNC";
  public static final String ASYNC = "ASYNC";
  public static final String CHILD = "CHILD";
  public static final String CHILDREN = "CHILDREN";
  public static final String TASK = "TASK";
  public static final String TASK_CHAIN = "TASK_CHAIN";
  public static final String CHILD_CHAIN = "CHILD_CHAIN";
  public static final String BARRIER = "BARRIER";
  public static final String RESOURCE_RESTRAINT = "RESOURCE_RESTRAINT";
}
