package software.wings.graphql.datafetcher.ce.exportData.dto;

import software.wings.graphql.schema.type.aggregation.billing.QLTimeGroupType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCETimeAggregation {
  QLTimeGroupType timePeriod;
}
