package io.harness.ccm.billing.graphql;

import io.harness.ccm.billing.RawBillingTableSchema;
import io.harness.ccm.billing.preaggregated.PreAggregatedTableSchema;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;

public enum CloudEntityGroupBy {
  projectId(PreAggregatedTableSchema.gcpProjectId, RawBillingTableSchema.gcpProjectId, null),
  billingAccountId(PreAggregatedTableSchema.gcpBillingAccountId, RawBillingTableSchema.gcpBillingAccountId, null),
  skuId(PreAggregatedTableSchema.gcpSkuId, RawBillingTableSchema.gcpSkuId, null),
  product(PreAggregatedTableSchema.gcpProduct, RawBillingTableSchema.gcpProduct, null),
  sku(PreAggregatedTableSchema.gcpSkuDescription, RawBillingTableSchema.gcpSkuDescription, null),
  region(PreAggregatedTableSchema.region, RawBillingTableSchema.region, RawBillingTableSchema.awsRegion),
  labelsKey(null, RawBillingTableSchema.labelsKey, null),
  labelsValue(null, RawBillingTableSchema.labelsValue, null),

  tagsKey(null, null, RawBillingTableSchema.tagsKey),
  tagsValue(null, null, RawBillingTableSchema.tagsValue),
  awsLinkedAccount(PreAggregatedTableSchema.awsUsageAccountId, null, RawBillingTableSchema.awsUsageAccountId),
  awsUsageType(PreAggregatedTableSchema.awsUsageType, null, RawBillingTableSchema.awsUsageType),
  awsInstanceType(PreAggregatedTableSchema.awsInstanceType, null, RawBillingTableSchema.awsInstanceType),
  awsService(PreAggregatedTableSchema.awsServiceCode, null, RawBillingTableSchema.awsServiceCode),
  projectNumber(PreAggregatedTableSchema.gcpProjectNumbers, null, null),
  cloudProvider(PreAggregatedTableSchema.cloudProvider, null, null);

  private DbColumn dbColumn;
  private DbColumn rawDbColumn;
  private DbColumn awsRawDbColumn;

  CloudEntityGroupBy(DbColumn dbColumn, DbColumn rawDbColumn, DbColumn awsRawDbColumn) {
    this.dbColumn = dbColumn;
    this.rawDbColumn = rawDbColumn;
    this.awsRawDbColumn = awsRawDbColumn;
  }

  DbColumn getDbObject() {
    return dbColumn;
  }

  DbColumn getRawDbObject() {
    return rawDbColumn;
  }

  DbColumn getAwsRawDbObject() {
    return awsRawDbColumn;
  }
}
