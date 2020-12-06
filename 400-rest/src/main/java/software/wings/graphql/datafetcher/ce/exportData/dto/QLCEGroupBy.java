package software.wings.graphql.datafetcher.ce.exportData.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCEGroupBy {
  QLCEEntityGroupBy entity;
  QLCETimeAggregation time;
  QLCETagAggregation tagAggregation;
  QLCELabelAggregation labelAggregation;
}
