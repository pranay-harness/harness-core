package software.wings.graphql.schema.type.aggregation.billing;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;

@Value
@Builder
public class QLBillingSortCriteria {
  private QLBillingSortType sortType;
  private QLSortOrder sortOrder;
}
