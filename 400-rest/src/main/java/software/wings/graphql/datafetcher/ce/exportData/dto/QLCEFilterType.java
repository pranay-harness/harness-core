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
import software.wings.graphql.schema.type.aggregation.QLDataType;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
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
