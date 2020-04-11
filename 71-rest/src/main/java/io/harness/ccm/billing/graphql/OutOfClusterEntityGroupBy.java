package io.harness.ccm.billing.graphql;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.harness.ccm.billing.GcpBillingTableSchema;
import io.harness.ccm.billing.preaggregated.PreAggregatedTableSchema;

public enum OutOfClusterEntityGroupBy {
  project(GcpBillingTableSchema.projectName),
  projectId(GcpBillingTableSchema.projectId),
  projectNumber(GcpBillingTableSchema.projectAncestryNumbers),
  product(GcpBillingTableSchema.serviceDescription),
  sku(GcpBillingTableSchema.skuDescription),
  skuId(GcpBillingTableSchema.skuId),
  region(GcpBillingTableSchema.locationRegion),
  billingAccountId(GcpBillingTableSchema.billingAccountId),
  usageAmount(GcpBillingTableSchema.usageAmountInPricingUnits),
  usageUnit(GcpBillingTableSchema.usagePricingUnit),
  awsRegion(PreAggregatedTableSchema.region),
  likedAccount(PreAggregatedTableSchema.usageAccountId),
  usageType(PreAggregatedTableSchema.usageType),
  instanceType(PreAggregatedTableSchema.instanceType),
  service(PreAggregatedTableSchema.serviceCode);

  private DbColumn dbColumn;
  OutOfClusterEntityGroupBy(DbColumn dbColumn) {
    this.dbColumn = dbColumn;
  }

  DbColumn getDbObject() {
    return dbColumn;
  }
}
