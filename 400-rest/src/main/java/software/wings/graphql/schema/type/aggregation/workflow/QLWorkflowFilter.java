package software.wings.graphql.schema.type.aggregation.workflow;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLWorkflowFilter implements EntityFilter {
  QLIdFilter application;
  QLIdFilter workflow;
  QLWorkflowTagFilter tag;
}
