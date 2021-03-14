package io.harness.ccm.billing.preaggregated;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLData;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(Module._490_CE_COMMONS)
public class PreAggregateCloudOverviewDataDTO implements QLData {
  Double totalCost;
  List<PreAggregateCloudOverviewDataPoint> data;
}
