package software.wings.graphql.datafetcher.ce.exportData.dto;

import software.wings.graphql.schema.type.aggregation.QLSortOrder;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCESort {
  private QLCESortType sortType;
  private QLSortOrder order;
}
