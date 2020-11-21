package software.wings.graphql.schema.type.aggregation.pipeline;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLPipelineFilter implements EntityFilter {
  QLIdFilter application;
  QLIdFilter pipeline;
  QLPipelineTagFilter tag;
}
