package software.wings.graphql.schema.type.aggregation.service;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLServiceFilter implements EntityFilter {
  QLIdFilter application;
  QLIdFilter service;
  QLServiceTagFilter tag;
}
