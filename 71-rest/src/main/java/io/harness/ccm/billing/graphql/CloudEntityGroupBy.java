package io.harness.ccm.billing.graphql;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.harness.ccm.billing.preaggregated.PreAggregatedTableSchema;

public enum CloudEntityGroupBy {
  projectId(PreAggregatedTableSchema.gcpProjectId),
  billingAccountId(PreAggregatedTableSchema.gcpBillingAccountId),
  skuId(PreAggregatedTableSchema.gcpSkuId),
  product(PreAggregatedTableSchema.gcpProduct),
  sku(PreAggregatedTableSchema.gcpSkuDescription),
  projectNumber(PreAggregatedTableSchema.gcpProjectNumbers),

  region(PreAggregatedTableSchema.region),
  awsLinkedAccount(PreAggregatedTableSchema.awsUsageAccountId),
  awsUsageType(PreAggregatedTableSchema.awsUsageType),
  awsInstanceType(PreAggregatedTableSchema.awsInstanceType),
  awsService(PreAggregatedTableSchema.awsServiceCode),
  cloudProvider(PreAggregatedTableSchema.cloudProvider);

  private DbColumn dbColumn;
  CloudEntityGroupBy(DbColumn dbColumn) {
    this.dbColumn = dbColumn;
  }

  DbColumn getDbObject() {
    return dbColumn;
  }
}
