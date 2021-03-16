package software.wings.graphql.datafetcher.ce.exportData.dto;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.datafetcher.ce.exportData.CEExportDataQueryMetadata.CEExportDataMetadataFields;

@TargetModule(Module._380_CG_GRAPHQL)
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
