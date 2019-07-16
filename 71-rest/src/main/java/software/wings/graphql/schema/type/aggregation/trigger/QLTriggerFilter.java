package software.wings.graphql.schema.type.aggregation.trigger;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

@Value
@Builder
public class QLTriggerFilter implements EntityFilter {
  QLIdFilter trigger;
  QLIdFilter application;
}
