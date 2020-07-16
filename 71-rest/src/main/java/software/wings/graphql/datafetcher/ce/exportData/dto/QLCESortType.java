package software.wings.graphql.datafetcher.ce.exportData.dto;

import software.wings.graphql.datafetcher.ce.exportData.CEExportDataQueryMetadata.CEExportDataMetadataFields;

public enum QLCESortType {
  TIME(CEExportDataMetadataFields.STARTTIME),
  TOTALCOST(CEExportDataMetadataFields.SUM),
  UNALLOCATEDCOST(CEExportDataMetadataFields.UNALLOCATEDCOST),
  IDLECOST(CEExportDataMetadataFields.IDLECOST);

  private CEExportDataMetadataFields billingMetaData;

  QLCESortType(CEExportDataMetadataFields billingMetaData) {
    this.billingMetaData = billingMetaData;
  }

  public CEExportDataMetadataFields getBillingMetaData() {
    return billingMetaData;
  }
}
