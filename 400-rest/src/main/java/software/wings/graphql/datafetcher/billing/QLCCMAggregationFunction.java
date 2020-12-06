package software.wings.graphql.datafetcher.billing;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCCMAggregationFunction {
  QLCCMAggregateOperation operationType;
  String columnName;
}
