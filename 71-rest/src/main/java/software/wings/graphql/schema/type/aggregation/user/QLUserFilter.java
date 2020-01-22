package software.wings.graphql.schema.type.aggregation.user;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

@Value
@Builder
public class QLUserFilter implements EntityFilter {
  QLIdFilter user;
}
