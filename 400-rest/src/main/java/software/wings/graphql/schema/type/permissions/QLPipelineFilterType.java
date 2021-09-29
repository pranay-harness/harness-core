package software.wings.graphql.schema.type.permissions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CDC)
public enum QLPipelineFilterType {
  PRODUCTION_PIPELINES,
  NON_PRODUCTION_PIPELINES,
  ALL_PIPELINES
}
