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
import software.wings.graphql.schema.type.aggregation.QLDataType;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public enum QLBillingDataFilterType {
  Application(BillingDataMetaDataFields.APPID),
  EndTime(BillingDataMetaDataFields.STARTTIME),
  StartTime(BillingDataMetaDataFields.STARTTIME),
  Service(BillingDataMetaDataFields.SERVICEID),
  Environment(BillingDataMetaDataFields.ENVID),
  Cluster(BillingDataMetaDataFields.CLUSTERID),
  CloudServiceName(BillingDataMetaDataFields.CLOUDSERVICENAME),
  LaunchType(BillingDataMetaDataFields.LAUNCHTYPE),
  TaskId(BillingDataMetaDataFields.TASKID),
  InstanceType(BillingDataMetaDataFields.INSTANCETYPE),
  InstanceName(BillingDataMetaDataFields.INSTANCENAME),
  WorkloadName(BillingDataMetaDataFields.WORKLOADNAME),
  WorkloadType(BillingDataMetaDataFields.WORKLOADTYPE),
  Namespace(BillingDataMetaDataFields.NAMESPACE),
  CloudProvider(BillingDataMetaDataFields.CLOUDPROVIDERID),
  NodeInstanceId(BillingDataMetaDataFields.INSTANCEID),
  PodInstanceId(BillingDataMetaDataFields.INSTANCEID),
  ParentInstanceId(BillingDataMetaDataFields.PARENTINSTANCEID),
  StorageUtilizationValue(BillingDataMetaDataFields.STORAGEUTILIZATIONVALUE),
  LabelSearch(null),
  TagSearch(null),
  Tag(null),
  Label(null),
  EnvironmentType(null),
  AlertTime(null),
  View(null);

  private QLDataType dataType;
  private BillingDataMetaDataFields metaDataFields;
  QLBillingDataFilterType() {}

  QLBillingDataFilterType(BillingDataMetaDataFields metaDataFields) {
    this.metaDataFields = metaDataFields;
  }

  public BillingDataMetaDataFields getMetaDataFields() {
    return metaDataFields;
  }
}
