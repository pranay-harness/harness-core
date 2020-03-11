package io.harness.ccm.billing;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;

public enum GcpBillingEntityGroupby {
  BILLING_ACCOUNT_ID(GcpBillingTableSchema.billingAccountId),
  PROJECT(GcpBillingTableSchema.projectName),
  SERVICE_ID(GcpBillingTableSchema.serviceId),
  SERVICE_DESCRIPTION(GcpBillingTableSchema.serviceDescription),
  SKU_ID(GcpBillingTableSchema.skuId);

  private DbColumn dbColumn;
  GcpBillingEntityGroupby(DbColumn dbColumn) {
    this.dbColumn = dbColumn;
  }

  DbColumn getDbObject() {
    return dbColumn;
  }
}
