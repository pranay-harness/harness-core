package io.harness.executionplan.plancreator.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CDC)
public class PlanCreatorConstants {
  public final String INFRA_SECTION_NODE_IDENTIFIER = "infrastructure";
  public final String INFRA_SECTION_NODE_NAME = "Infrastructure Section";
  public final String STAGES_NODE_IDENTIFIER = "stages";
  public final String EXECUTION_NODE_IDENTIFIER = "execution";
  public final String INFRA_DEFINITION_NODE_IDENTIFIER = "infrastructureDefinition";
  public final String EXECUTION_ROLLBACK_NODE_IDENTIFIER = "executionRollback";
  public final String STEP_GROUPS_ROLLBACK_NODE_IDENTIFIER = "stepGroupsRollback";
  public final String PARALLEL_STEP_GROUPS_ROLLBACK_NODE_IDENTIFIER = "parallelStepGroupsRollback";
  public final String PARALLEL_STEP_GROUPS_ROLLBACK_NODE_NAME = "Parallel StepGroups (Rollback)";
  public final String SERVICE_NODE_NAME = "Service";
  public final String SERVICE_DEFINITION_NODE_NAME = "ServiceDefinition";
  public final String SERVICE_SPEC_NODE_NAME = "ServiceSpec";
  public final String ARTIFACTS_NODE_NAME = "Artifacts";
  public final String SIDECARS_NODE_NAME = "Sidecars";
  public final String ARTIFACT_NODE_NAME = "Artifact";
  public final String MANIFESTS_NODE_NAME = "Manifests";
  public final String MANIFEST_NODE_NAME = "Manifest";
  public final String INFRA_NODE_NAME = "Infrastructure";
  public final String EXECUTION_NODE_NAME = "Execution";
}
