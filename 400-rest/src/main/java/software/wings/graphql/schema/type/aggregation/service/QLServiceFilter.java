package software.wings.graphql.schema.type.aggregation.service;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLServiceFilter implements EntityFilter {
  QLIdFilter application;
  QLIdFilter service;
  QLServiceTagFilter tag;
}
