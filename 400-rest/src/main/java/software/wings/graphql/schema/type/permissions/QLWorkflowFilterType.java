package software.wings.graphql.schema.type.permissions;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._380_CG_GRAPHQL)
public enum QLWorkflowFilterType {
  PRODUCTION_WORKFLOWS,
  NON_PRODUCTION_WORKFLOWS,
  WORKFLOW_TEMPLATES
}
