package io.harness.ccm.billing;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;

@Value
@UtilityClass
@FieldNameConstants(innerTypeName = "GCPBillingDataTableKeys")
public class GcpBillingTableSchema {
  public static final DbSpec spec;
  public static final DbSchema schema;
  public static final DbTable table;

  public static final DbColumn billingAccountId;
  public static final DbColumn invoiceMonth;
  public static final DbColumn costType;
  public static final DbColumn serviceId;
  public static final DbColumn serviceDescription;
  public static final DbColumn skuId;
  public static final DbColumn skuDescription;
  public static final DbColumn usageStartTime;
  public static final DbColumn usageEndTime;
  public static final DbColumn projectId;
  public static final DbColumn projectName;
  public static final DbColumn projectAncestryNumbers;
  public static final DbColumn projectLabelsKey;
  public static final DbColumn projectLabelsValue;
  public static final DbColumn labelsKey;
  public static final DbColumn labelsValue;
  public static final DbColumn systemLabelsKey;
  public static final DbColumn systemLabelsValue;
  public static final DbColumn locationLocation;
  public static final DbColumn locationCountry;
  public static final DbColumn locationRegion;
  public static final DbColumn locationZone;
  public static final DbColumn cost;
  public static final DbColumn currency;
  public static final DbColumn currencyConversionRate;
  public static final DbColumn usageAmount;
  public static final DbColumn usageUnit;
  public static final DbColumn usageAmountInPricingUnits;
  public static final DbColumn usagePricingUnit;
  public static final DbColumn creditsName;
  public static final DbColumn creditsAmount;
  public static final DbColumn exportTime;

  static {
    spec = new DbSpec();
    schema = spec.addDefaultSchema();
    table = schema.addTable(
        "`ccm-play.billing.gcp_billing_export_v1_0172EF_8FDFEB_21C220`"); // todo should specify the right table name

    billingAccountId = table.addColumn("billing_account_id");
    invoiceMonth = table.addColumn("invoice.month");
    costType = table.addColumn("cost_type");
    serviceId = table.addColumn("service.id");
    serviceDescription = table.addColumn("service.description");
    skuId = table.addColumn("sku.id");
    skuDescription = table.addColumn("sku.description");
    usageStartTime = table.addColumn("usage_start_time", "timestamp", null);
    usageEndTime = table.addColumn("usage_end_time", "timestamp", null);
    projectId = table.addColumn("project.id");
    projectName = table.addColumn("project.name");
    projectAncestryNumbers = table.addColumn("project.ancestry_numbers");
    projectLabelsKey = table.addColumn("project.labels.key");
    projectLabelsValue = table.addColumn("project.labels.value");
    labelsKey = table.addColumn("labels.key");
    labelsValue = table.addColumn("labels.value");
    systemLabelsKey = table.addColumn("system_labels.key");
    systemLabelsValue = table.addColumn("system_labels.value");
    locationLocation = table.addColumn("location.location");
    locationCountry = table.addColumn("location.country");
    locationRegion = table.addColumn("location.region");
    locationZone = table.addColumn("location.zone");
    cost = table.addColumn("cost");
    currency = table.addColumn("currency");
    currencyConversionRate = table.addColumn("currency_conversion_rate");
    usageAmount = table.addColumn("usage.amount");
    usageUnit = table.addColumn("usage.unit");
    usageAmountInPricingUnits = table.addColumn("usage.amount_in_pricing_units");
    usagePricingUnit = table.addColumn("usage.pricing_unit");
    creditsName = table.addColumn("credits.name");
    creditsAmount = table.addColumn("credits.amount");
    exportTime = table.addColumn("export_time");
  }
}
