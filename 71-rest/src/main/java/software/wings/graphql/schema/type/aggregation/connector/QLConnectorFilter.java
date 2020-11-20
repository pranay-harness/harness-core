package software.wings.graphql.schema.type.aggregation.connector;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

@Value
@Builder
public class QLConnectorFilter implements EntityFilter {
  QLIdFilter connector;
  QLConnectorTypeFilter connectorType;
  QLTimeFilter createdAt;
}
