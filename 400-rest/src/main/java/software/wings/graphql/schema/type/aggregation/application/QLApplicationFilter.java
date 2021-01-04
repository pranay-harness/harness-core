package software.wings.graphql.schema.type.aggregation.application;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLApplicationFilter implements EntityFilter {
  QLIdFilter application;
  QLApplicationTagFilter tag;
}
