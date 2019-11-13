package software.wings.graphql.schema.type.aggregation.billing;

import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLDataType;

public enum QLBillingDataFilterType {
  Application(BillingDataMetaDataFields.APPID),
  EndTime(BillingDataMetaDataFields.STARTTIME),
  StartTime(BillingDataMetaDataFields.STARTTIME),
  Service(BillingDataMetaDataFields.SERVICEID),
  Environment(BillingDataMetaDataFields.ENVID),
  Cluster(BillingDataMetaDataFields.CLUSTERID);

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
