package software.wings.graphql.schema.type.aggregation.application;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLApplicationFilter implements EntityFilter {
  QLIdFilter application;
  QLApplicationTagFilter tag;
}
