package io.harness.ccm.billing.preaggregated;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.datafetcher.billing.QLEntityData;
import software.wings.graphql.schema.type.QLK8sLabel;

import java.util.List;
import java.util.Set;

@Value
@Builder
public class PreAggregatedFilterValuesDataPoint {
  Set<QLEntityData> region;
  Set<QLEntityData> awsService;
  Set<QLEntityData> awsUsageType;
  Set<QLEntityData> awsInstanceType;
  Set<QLEntityData> awsLinkedAccount;
  Set<QLEntityData> gcpProjectId;
  Set<QLEntityData> gcpProduct;
  Set<QLEntityData> gcpSku;
  Set<QLEntityData> gcpBillingAccount;
  List<QLK8sLabel> gcpLabels;
}
