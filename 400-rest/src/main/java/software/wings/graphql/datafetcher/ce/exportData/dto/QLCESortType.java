/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
