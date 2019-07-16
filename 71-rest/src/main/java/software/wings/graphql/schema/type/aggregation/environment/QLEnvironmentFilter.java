package software.wings.graphql.schema.type.aggregation.environment;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.QLEnvironmentType;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

@Value
@Builder
public class QLEnvironmentFilter implements EntityFilter {
  QLIdFilter environment;
  QLIdFilter application;
  QLEnvironmentType environmentType;
}
