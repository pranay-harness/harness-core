package software.wings.graphql.datafetcher.cloudefficiencyevents;

import software.wings.graphql.schema.type.aggregation.QLSortOrder;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLEventsSortCriteria {
  private QLEventsSortType sortType;
  private QLSortOrder sortOrder;
}
