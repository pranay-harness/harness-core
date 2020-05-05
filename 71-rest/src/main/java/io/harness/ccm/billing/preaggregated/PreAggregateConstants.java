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
}
