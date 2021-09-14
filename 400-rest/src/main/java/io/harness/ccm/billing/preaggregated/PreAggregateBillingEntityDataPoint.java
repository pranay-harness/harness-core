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

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class PreAggregateBillingEntityDataPoint {
  String id;
  String region;
  String awsLinkedAccount;
  String awsUsageType;
  String awsInstanceType;
  String awsService;
  String awsTag;
  double costTrend;
  double awsBlendedCost;
  double awsUnblendedCost;

  String gcpProjectId;
  String gcpProduct;
  String gcpSkuDescription;
  String gcpSkuId;
  String gcpLabel;
  double gcpDiscount;
  double gcpTotalCost;
  double gcpSubTotalCost;
}
