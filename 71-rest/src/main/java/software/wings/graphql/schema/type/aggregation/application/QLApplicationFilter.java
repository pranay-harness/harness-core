package software.wings.graphql.schema.type.aggregation.application;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

@Value
@Builder
public class QLApplicationFilter implements EntityFilter {
  QLIdFilter application;
  QLApplicationTagFilter tag;
}
