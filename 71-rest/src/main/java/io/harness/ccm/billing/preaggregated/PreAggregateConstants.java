package io.harness.ccm.billing.preaggregated;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PreAggregateConstants {
  public static final String nullStringValueConstant = "Others";
  public static final String entityConstantAwsRegion = "region";
  public static final String entityConstantAwsLinkedAccount = "usageaccountid";
  public static final String entityConstantAwsUsageType = "usagetype";
  public static final String entityConstantAwsInstanceType = "instancetype";
  public static final String entityConstantAwsService = "servicecode";
  public static final String entityConstantAwsBlendedCost = "sum_blendedCost";
  public static final String entityConstantAwsUnBlendedCost = "sum_unblendedCost";
  public static final String minPreAggStartTimeConstant = "min_startTime";
  public static final String maxPreAggStartTimeConstant = "max_startTime";
}
