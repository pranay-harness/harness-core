package software.wings.graphql.datafetcher.ce.exportData.dto;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;

@Value
@Builder
public class QLCESort {
  private QLCESortType sortType;
  private QLSortOrder order;
}
