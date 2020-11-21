package software.wings.graphql.schema.type.aggregation.billing;

import software.wings.graphql.schema.type.aggregation.QLSortOrder;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLBillingSortCriteria {
  private QLBillingSortType sortType;
  private QLSortOrder sortOrder;
}
