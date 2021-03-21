package software.wings.graphql.schema.type.aggregation.billing;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
