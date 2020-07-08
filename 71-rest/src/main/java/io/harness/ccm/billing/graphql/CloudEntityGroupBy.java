package io.harness.ccm.billing.graphql;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.harness.ccm.billing.RawBillingTableSchema;
import io.harness.ccm.billing.preaggregated.PreAggregatedTableSchema;

public enum CloudEntityGroupBy {
  projectId(PreAggregatedTableSchema.gcpProjectId, RawBillingTableSchema.gcpProjectId),
  billingAccountId(PreAggregatedTableSchema.gcpBillingAccountId, RawBillingTableSchema.gcpBillingAccountId),
  skuId(PreAggregatedTableSchema.gcpSkuId, RawBillingTableSchema.gcpSkuId),
  product(PreAggregatedTableSchema.gcpProduct, RawBillingTableSchema.gcpProduct),
  sku(PreAggregatedTableSchema.gcpSkuDescription, RawBillingTableSchema.gcpSkuDescription),
  region(PreAggregatedTableSchema.region, RawBillingTableSchema.region),
  labelsKey(null, RawBillingTableSchema.labelsKey),
  labelsValue(null, RawBillingTableSchema.labelsValue),

  projectNumber(PreAggregatedTableSchema.gcpProjectNumbers, null),
  awsLinkedAccount(PreAggregatedTableSchema.awsUsageAccountId, null),
  awsUsageType(PreAggregatedTableSchema.awsUsageType, null),
  awsInstanceType(PreAggregatedTableSchema.awsInstanceType, null),
  awsService(PreAggregatedTableSchema.awsServiceCode, null),
  cloudProvider(PreAggregatedTableSchema.cloudProvider, null);

  private DbColumn dbColumn;
  private DbColumn rawDbColumn;
  CloudEntityGroupBy(DbColumn dbColumn, DbColumn rawDbColumn) {
    this.dbColumn = dbColumn;
    this.rawDbColumn = rawDbColumn;
  }

  DbColumn getDbObject() {
    return dbColumn;
  }

  DbColumn getRawDbObject() {
    return rawDbColumn;
  }
}
