package software.wings.graphql.datafetcher.ce.exportData.dto;

import software.wings.graphql.datafetcher.ce.exportData.CEExportDataQueryMetadata.CEExportDataMetadataFields;

public enum QLCESortType {
  Time(CEExportDataMetadataFields.STARTTIME),
  TotalCost(CEExportDataMetadataFields.SUM),
  UnallocatedCost(CEExportDataMetadataFields.UNALLOCATEDCOST),
  IdleCost(CEExportDataMetadataFields.IDLECOST);

  private CEExportDataMetadataFields billingMetaData;

  QLCESortType(CEExportDataMetadataFields billingMetaData) {
    this.billingMetaData = billingMetaData;
  }

  public CEExportDataMetadataFields getBillingMetaData() {
    return billingMetaData;
  }
}
