package software.wings.graphql.datafetcher.cloudefficiencyevents;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;

@Value
@Builder
public class QLEventsSortCriteria {
  private QLEventsSortType sortType;
  private QLSortOrder sortOrder;
}
