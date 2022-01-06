/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ce.exportData.dto;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.datafetcher.ce.exportData.CEExportDataQueryMetadata.CEExportDataMetadataFields;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public enum QLCESortType {
  TIME(CEExportDataMetadataFields.STARTTIME),
  TOTALCOST(CEExportDataMetadataFields.SUM),
  UNALLOCATEDCOST(CEExportDataMetadataFields.UNALLOCATEDCOST),
  IDLECOST(CEExportDataMetadataFields.IDLECOST),
  WORKLOAD(CEExportDataMetadataFields.WORKLOADNAME),
  NAMESPACE(CEExportDataMetadataFields.NAMESPACE),
  CLUSTER(CEExportDataMetadataFields.CLUSTERID),
  APPLICATION(CEExportDataMetadataFields.APPID),
  SERVICE(CEExportDataMetadataFields.SERVICEID),
  ENVIRONMENT(CEExportDataMetadataFields.ENVID),
  REGION(CEExportDataMetadataFields.REGION),
  ECS_SERVICE(CEExportDataMetadataFields.CLOUDSERVICENAME),
  TASK(CEExportDataMetadataFields.TASKID),
  LAUNCHTYPE(CEExportDataMetadataFields.LAUNCHTYPE),
  WORKLOADTYPE(CEExportDataMetadataFields.WORKLOADTYPE),
  INSTANCE(CEExportDataMetadataFields.INSTANCEID);

  private CEExportDataMetadataFields billingMetaData;

  QLCESortType(CEExportDataMetadataFields billingMetaData) {
    this.billingMetaData = billingMetaData;
  }

  public CEExportDataMetadataFields getBillingMetaData() {
    return billingMetaData;
  }
}
