package software.wings.graphql.schema.type.aggregation.billing;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.TagAggregation;

@Value
@Builder
public class QLBillingDataTagAggregation implements TagAggregation {
  private QLBillingDataTagType entityType;
  private String tagName;
}
