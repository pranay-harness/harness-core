package software.wings.graphql.schema.type.aggregation.environment;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLEnvironmentFilter implements EntityFilter {
  QLIdFilter environment;
  QLIdFilter application;
  QLEnvironmentTypeFilter environmentType;
  QLEnvironmentTagFilter tag;
}
