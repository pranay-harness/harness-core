/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.cloudefficiencyevents;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.datafetcher.cloudefficiencyevents.CEEventsQueryMetaData.CEEventsMetaDataFields;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public enum QLEventsDataFilterType {
  Application(CEEventsMetaDataFields.APPID),
  StartTime(CEEventsMetaDataFields.STARTTIME),
  EndTime(CEEventsMetaDataFields.STARTTIME),
  Service(CEEventsMetaDataFields.SERVICEID),
  Environment(CEEventsMetaDataFields.ENVID),
  Cluster(CEEventsMetaDataFields.CLUSTERID),
  CloudServiceName(CEEventsMetaDataFields.CLOUDSERVICENAME),
  TaskId(CEEventsMetaDataFields.TASKID),
  WorkloadName(CEEventsMetaDataFields.WORKLOADNAME),
  WorkloadType(CEEventsMetaDataFields.WORKLOADTYPE),
  Namespace(CEEventsMetaDataFields.NAMESPACE),
  BillingAmount(CEEventsMetaDataFields.BILLINGAMOUNT);

  private CEEventsMetaDataFields metaDataFields;
  QLEventsDataFilterType() {}

  QLEventsDataFilterType(CEEventsMetaDataFields metaDataFields) {
    this.metaDataFields = metaDataFields;
  }

  public CEEventsMetaDataFields getMetaDataFields() {
    return metaDataFields;
  }
}
