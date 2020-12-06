package software.wings.graphql.schema.type.aggregation.billing;

import software.wings.graphql.schema.type.aggregation.LabelAggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLBillingDataLabelAggregation implements LabelAggregation {
  String name;
}
