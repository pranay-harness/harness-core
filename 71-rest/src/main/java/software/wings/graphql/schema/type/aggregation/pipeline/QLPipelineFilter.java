package software.wings.graphql.schema.type.aggregation.pipeline;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

@Value
@Builder
public class QLPipelineFilter implements EntityFilter {
  QLIdFilter application;
  QLIdFilter pipeline;
  QLPipelineTagFilter tag;
}
