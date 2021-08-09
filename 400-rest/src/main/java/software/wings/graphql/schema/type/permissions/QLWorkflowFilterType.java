package software.wings.graphql.schema.type.permissions;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLWorkflowFilterType {
  PRODUCTION_WORKFLOWS,
  NON_PRODUCTION_WORKFLOWS,
  WORKFLOW_TEMPLATES
}
