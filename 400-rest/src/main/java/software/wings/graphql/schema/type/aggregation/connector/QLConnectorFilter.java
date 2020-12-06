package software.wings.graphql.schema.type.aggregation.connector;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLConnectorFilter implements EntityFilter {
  QLIdFilter connector;
  QLConnectorTypeFilter connectorType;
  QLTimeFilter createdAt;
}
