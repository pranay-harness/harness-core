package io.harness.ccm.billing.preaggregated;

import software.wings.graphql.schema.type.aggregation.QLData;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PreAggregateCloudOverviewDataDTO implements QLData {
  Double totalCost;
  List<PreAggregateCloudOverviewDataPoint> data;
}
