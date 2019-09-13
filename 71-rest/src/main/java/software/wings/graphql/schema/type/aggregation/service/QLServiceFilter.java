package software.wings.graphql.schema.type.aggregation.service;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

@Value
@Builder
public class QLServiceFilter implements EntityFilter {
  QLIdFilter application;
  QLIdFilter service;
  QLServiceTagFilter tag;
}
