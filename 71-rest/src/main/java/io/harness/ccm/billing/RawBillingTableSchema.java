package io.harness.ccm.billing;

import static io.harness.ccm.billing.preaggregated.PreAggregatedTableSchema.defaultTableName;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import lombok.Value;
import lombok.experimental.UtilityClass;

@Value
@UtilityClass
public class RawBillingTableSchema {
  public static final DbSpec spec;
  public static final DbSchema schema;
  public static final DbTable table;

  public static final DbColumn gcpBillingAccountId;
  public static final DbColumn gcpProject;
  public static final DbColumn gcpProjectId;
  public static final DbColumn gcpProduct;
  public static final DbColumn gcpSkuId;
  public static final DbColumn gcpSkuDescription;
  public static final DbColumn cost;
  public static final DbColumn zone;
  public static final DbColumn region;
  public static final DbColumn startTime;
  public static final DbColumn endTime;

  public static final DbColumn invoiceMonth;
  public static final DbColumn costType;
  public static final DbColumn serviceId;
  public static final DbColumn projectAncestryNumbers;
  public static final DbColumn projectLabelsKey;
  public static final DbColumn projectLabelsValue;
  public static final DbColumn labelsKey;
  public static final DbColumn labelsValue;
  public static final DbColumn systemLabelsKey;
  public static final DbColumn systemLabelsValue;
  public static final DbColumn locationLocation;
  public static final DbColumn locationCountry;
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
    table = schema.addTable("`" + defaultTableName + "`");

    cost = table.addColumn("cost");
    region = table.addColumn("location.region");
    zone = table.addColumn("location.zone");
    startTime = table.addColumn("usage_start_time", "timestamp", null);
    endTime = table.addColumn("usage_end_time", "timestamp", null);
    gcpBillingAccountId = table.addColumn("billing_account_id");
    gcpProjectId = table.addColumn("project.id");
    gcpProject = table.addColumn("project.name");
    gcpProduct = table.addColumn("service.description");
    gcpSkuId = table.addColumn("sku.id");
    gcpSkuDescription = table.addColumn("sku.description");
    labelsKey = table.addColumn("labels.key");
    labelsValue = table.addColumn("labels.value");

    projectLabelsKey = table.addColumn("project.labels.key");
    projectLabelsValue = table.addColumn("project.labels.value");
    systemLabelsKey = table.addColumn("system_labels.key");
    systemLabelsValue = table.addColumn("system_labels.value");

    invoiceMonth = table.addColumn("invoice.month");
    costType = table.addColumn("cost_type");
    serviceId = table.addColumn("service.id");
    projectAncestryNumbers = table.addColumn("project.ancestry_numbers");
    locationLocation = table.addColumn("location.location");
    locationCountry = table.addColumn("location.country");
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
