package io.harness.ccm.billing.preaggregated;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.datafetcher.billing.QLEntityData;

import java.util.Set;

@Value
@Builder
public class PreAggregatedFilterValuesDataPoint {
  Set<QLEntityData> awsRegion;
  Set<QLEntityData> awsService;
  Set<QLEntityData> awsUsageType;
  Set<QLEntityData> awsInstanceType;
  Set<QLEntityData> awsLinkedAccount;
}
