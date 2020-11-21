package software.wings.graphql.schema.type.aggregation.user;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLUserFilter implements EntityFilter {
  QLIdFilter user;
}
