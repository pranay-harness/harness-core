package software.wings.graphql.datafetcher.ce.exportData.dto;

import software.wings.graphql.datafetcher.ce.exportData.CEExportDataQueryMetadata.CEExportDataMetadataFields;
import software.wings.graphql.schema.type.aggregation.QLDataType;

public enum QLCEFilterType {
  Application(CEExportDataMetadataFields.APPID),
  EndTime(CEExportDataMetadataFields.STARTTIME),
  StartTime(CEExportDataMetadataFields.STARTTIME),
  Service(CEExportDataMetadataFields.SERVICEID),
  Environment(CEExportDataMetadataFields.ENVID),
  Cluster(CEExportDataMetadataFields.CLUSTERID),
  EcsService(CEExportDataMetadataFields.CLOUDSERVICENAME),
  LaunchType(CEExportDataMetadataFields.LAUNCHTYPE),
  Task(CEExportDataMetadataFields.TASKID),
  InstanceType(CEExportDataMetadataFields.INSTANCETYPE),
  Workload(CEExportDataMetadataFields.WORKLOADNAME),
  Namespace(CEExportDataMetadataFields.NAMESPACE),
  Node(CEExportDataMetadataFields.INSTANCEID),
  Pod(CEExportDataMetadataFields.INSTANCEID),
  Tag(null),
  Label(null);

  private QLDataType dataType;
  private CEExportDataMetadataFields metaDataFields;
  QLCEFilterType() {}

  QLCEFilterType(CEExportDataMetadataFields metaDataFields) {
    this.metaDataFields = metaDataFields;
  }

  public CEExportDataMetadataFields getMetaDataFields() {
    return metaDataFields;
  }
}
