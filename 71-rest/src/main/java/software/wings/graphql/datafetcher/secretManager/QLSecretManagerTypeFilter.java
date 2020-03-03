package software.wings.graphql.datafetcher.secretManager;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;
import software.wings.graphql.schema.type.secretManagers.QLSecretManagerType;

@Value
@Builder
public class QLSecretManagerTypeFilter implements Filter {
  private QLEnumOperator operator;
  private QLSecretManagerType[] values;
}
