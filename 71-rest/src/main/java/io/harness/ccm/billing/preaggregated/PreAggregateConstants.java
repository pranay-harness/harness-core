package io.harness.ccm.billing.preaggregated;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PreAggregateConstants {
  public static final String nullStringValueConstant = "Others";
  public static final String entityConstantRegion = "region";
  public static final String entityConstantAwsLinkedAccount = "awsUsageaccountid";
  public static final String entityConstantAwsUsageType = "awsUsagetype";
  public static final String entityConstantAwsInstanceType = "awsInstancetype";
  public static final String entityConstantAwsService = "awsServicecode";
  public static final String entityConstantAwsBlendedCost = "sum_blendedCost";
  public static final String entityConstantAwsUnBlendedCost = "sum_unblendedCost";

  public static final String minPreAggStartTimeConstant = "min_startTime";
  public static final String maxPreAggStartTimeConstant = "max_startTime";
  public static final String startTimeTruncatedConstant = "start_time_trunc";

  public static final String entityConstantGcpBillingAccount = "gcpBillingAccountId";
  public static final String entityConstantGcpProjectId = "gcpProjectId";
  public static final String entityConstantGcpProduct = "gcpProduct";
  public static final String entityConstantGcpSkuId = "gcpSkuId";
  public static final String entityConstantGcpSku = "gcpSkuDescription";
  public static final String entityConstantGcpCost = "sum_cost";
  public static final String entityCloudProviderConst = "cloudProvider";

  public static final String entityConstantNoRegion = "No Region";
  public static final String entityConstantAwsNoLinkedAccount = "No Linked Account";
  public static final String entityConstantAwsNoUsageType = "No Usage Type";
  public static final String entityConstantAwsNoInstanceType = "No Instance Type";
  public static final String entityConstantAwsNoService = "No Service";
  public static final String entityConstantGcpNoProjectId = "No Project";
  public static final String entityConstantGcpNoProduct = "No Product";
  public static final String entityConstantGcpNoSkuId = "No SkuId";
  public static final String entityConstantGcpNoSku = "No Sku";
  public static final String entityNoCloudProviderConst = "No Cloud Provider";
}
