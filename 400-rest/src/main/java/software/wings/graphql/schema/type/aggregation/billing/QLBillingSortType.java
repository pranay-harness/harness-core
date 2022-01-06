/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.type.aggregation.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public enum QLBillingSortType {
  Time(BillingDataMetaDataFields.STARTTIME),
  Amount(BillingDataMetaDataFields.SUM),
  storageCost(BillingDataMetaDataFields.STORAGECOST),
  IdleCost(BillingDataMetaDataFields.IDLECOST),
  Application(BillingDataMetaDataFields.APPID),
  Service(BillingDataMetaDataFields.SERVICEID),
  Environment(BillingDataMetaDataFields.ENVID),
  CloudProvider(BillingDataMetaDataFields.CLOUDPROVIDERID),
  Cluster(BillingDataMetaDataFields.CLUSTERID),
  Namespace(BillingDataMetaDataFields.NAMESPACE),
  Workload(BillingDataMetaDataFields.WORKLOADNAME),
  Node(BillingDataMetaDataFields.INSTANCEID),
  Pod(BillingDataMetaDataFields.INSTANCEID),
  CloudServiceName(BillingDataMetaDataFields.CLOUDSERVICENAME),
  LaunchType(BillingDataMetaDataFields.LAUNCHTYPE),
  TaskId(BillingDataMetaDataFields.TASKID);

  private BillingDataMetaDataFields billingMetaData;

  QLBillingSortType(BillingDataMetaDataFields billingMetaData) {
    this.billingMetaData = billingMetaData;
  }

  public BillingDataMetaDataFields getBillingMetaData() {
    return billingMetaData;
  }
}
