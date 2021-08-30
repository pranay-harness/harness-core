package software.wings.graphql.schema.type.permissions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CDC)
public enum QLWorkflowFilterType {
  PRODUCTION_WORKFLOWS,
  NON_PRODUCTION_WORKFLOWS,
  WORKFLOW_TEMPLATES,
  ALL_WORKFLOWS
}
