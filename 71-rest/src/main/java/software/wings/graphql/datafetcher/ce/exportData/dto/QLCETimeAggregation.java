package software.wings.graphql.datafetcher.ce.exportData.dto;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.billing.QLTimeGroupType;

@Value
@Builder
public class QLCETimeAggregation {
  QLTimeGroupType timePeriod;
}
