package software.wings.graphql.schema.type.permissions;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLDeploymentFilterType {
  PRODUCTION_ENVIRONMENTS,
  NON_PRODUCTION_ENVIRONMENTS
}
