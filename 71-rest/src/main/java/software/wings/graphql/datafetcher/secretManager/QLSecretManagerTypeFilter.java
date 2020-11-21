package software.wings.graphql.datafetcher.secretManager;

import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;
import software.wings.graphql.schema.type.secretManagers.QLSecretManagerType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLSecretManagerTypeFilter implements Filter {
  private QLEnumOperator operator;
  private QLSecretManagerType[] values;
}
