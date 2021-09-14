/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.billing.preaggregated;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.datafetcher.billing.QLEntityData;
import software.wings.graphql.schema.type.QLK8sLabel;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
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
  List<QLK8sLabel> awsTags;
}
