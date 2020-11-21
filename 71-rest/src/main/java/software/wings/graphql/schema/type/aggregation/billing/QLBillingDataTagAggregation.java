package software.wings.graphql.schema.type.aggregation.billing;

import software.wings.graphql.schema.type.aggregation.TagAggregation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLBillingDataTagAggregation implements TagAggregation {
  private QLBillingDataTagType entityType;
  private String tagName;
}
