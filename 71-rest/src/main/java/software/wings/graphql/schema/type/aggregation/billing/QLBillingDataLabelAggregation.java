package software.wings.graphql.schema.type.aggregation.billing;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.LabelAggregation;

@Value
@Builder
public class QLBillingDataLabelAggregation implements LabelAggregation {
  String name;
}
