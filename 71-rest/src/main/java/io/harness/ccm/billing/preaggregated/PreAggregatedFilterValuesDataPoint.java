package io.harness.ccm.billing.preaggregated;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.datafetcher.billing.QLEntityData;

import java.util.Set;

@Value
@Builder
public class PreAggregatedFilterValuesDataPoint {
  Set<QLEntityData> region;
  Set<QLEntityData> awsService;
  Set<QLEntityData> awsUsageType;
  Set<QLEntityData> awsInstanceType;
  Set<QLEntityData> awsLinkedAccount;
  Set<QLEntityData> gcpProject;
  Set<QLEntityData> gcpProduct;
  Set<QLEntityData> gcpSku;
  Set<QLEntityData> gcpBillingAccount;
}
