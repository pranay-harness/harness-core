/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
