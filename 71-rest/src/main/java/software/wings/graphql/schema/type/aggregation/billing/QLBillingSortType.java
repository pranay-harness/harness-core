package software.wings.graphql.schema.type.aggregation.billing;

import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;

public enum QLBillingSortType {
  Time(BillingDataMetaDataFields.STARTTIME),
  Amount(BillingDataMetaDataFields.SUM);

  private BillingDataMetaDataFields billingMetaData;

  QLBillingSortType(BillingDataMetaDataFields billingMetaData) {
    this.billingMetaData = billingMetaData;
  }

  public BillingDataMetaDataFields getBillingMetaData() {
    return billingMetaData;
  }
}