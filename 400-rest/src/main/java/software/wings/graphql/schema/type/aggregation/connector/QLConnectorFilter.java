package software.wings.graphql.schema.type.aggregation.connector;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._380_CG_GRAPHQL)
public class QLConnectorFilter implements EntityFilter {
  QLIdFilter connector;
  QLConnectorTypeFilter connectorType;
  QLTimeFilter createdAt;
}
