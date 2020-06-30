package software.wings.graphql.datafetcher.ce.recommendation.dto;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;

@Value
@Builder
public class QLWorkloadTypeFilter implements Filter {
  QLEnumOperator operator;
  QLWorkloadType[] values;
}
