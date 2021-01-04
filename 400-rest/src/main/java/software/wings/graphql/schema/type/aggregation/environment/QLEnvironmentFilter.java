package software.wings.graphql.schema.type.aggregation.environment;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLEnvironmentFilter implements EntityFilter {
  QLIdFilter environment;
  QLIdFilter application;
  QLEnvironmentTypeFilter environmentType;
  QLEnvironmentTagFilter tag;
}
