package io.harness.ccm.billing;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;

// TODO: Remove Old/New Set of GroupBy's (Adding new groupBy's in lower case to maintain consistency)
public enum GcpBillingEntityGroupby {
  BILLING_ACCOUNT_ID(GcpBillingTableSchema.billingAccountId),
  PROJECT(GcpBillingTableSchema.projectName),
  SERVICE_ID(GcpBillingTableSchema.serviceId),
  SERVICE_DESCRIPTION(GcpBillingTableSchema.serviceDescription),
  SKU_ID(GcpBillingTableSchema.skuId),
  project(GcpBillingTableSchema.projectName),
  projectId(GcpBillingTableSchema.projectId),
  projectNumber(GcpBillingTableSchema.projectAncestryNumbers),
  product(GcpBillingTableSchema.serviceDescription),
  sku(GcpBillingTableSchema.skuDescription),
  skuId(GcpBillingTableSchema.skuId),
  region(GcpBillingTableSchema.locationRegion),
  billingAccountId(GcpBillingTableSchema.billingAccountId),
  usageAmount(GcpBillingTableSchema.usageAmountInPricingUnits),
  usageUnit(GcpBillingTableSchema.usagePricingUnit);

  private DbColumn dbColumn;
  GcpBillingEntityGroupby(DbColumn dbColumn) {
    this.dbColumn = dbColumn;
  }

  DbColumn getDbObject() {
    return dbColumn;
  }
}
