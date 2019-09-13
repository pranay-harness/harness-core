package software.wings.graphql.schema.type.aggregation.workflow;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

@Value
@Builder
public class QLWorkflowFilter implements EntityFilter {
  QLIdFilter application;
  QLIdFilter workflow;
  QLWorkflowTagFilter tag;
}
