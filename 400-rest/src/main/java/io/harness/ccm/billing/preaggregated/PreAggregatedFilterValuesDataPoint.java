/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
