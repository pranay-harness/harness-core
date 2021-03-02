package software.wings.graphql.datafetcher.ce.exportData.dto;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.datafetcher.ce.exportData.CEExportDataQueryMetadata.CEExportDataMetadataFields;

@TargetModule(Module._375_CE_GRAPHQL)
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
