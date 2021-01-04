package software.wings.graphql.schema.type.aggregation.trigger;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLTriggerFilter implements EntityFilter {
  QLIdFilter trigger;
  QLIdFilter application;
  QLTriggerTagFilter tag;
}
