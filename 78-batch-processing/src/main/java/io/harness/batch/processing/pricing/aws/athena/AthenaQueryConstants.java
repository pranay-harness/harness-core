package io.harness.batch.processing.pricing.aws.athena;

import io.harness.ccm.CCMSettingServiceImpl;

public class AthenaQueryConstants {
  private AthenaQueryConstants() {}

  public static final String ATHENA_OUTPUT_BUCKET = "s3://" + CCMSettingServiceImpl.HARNESS_BASE_PATH + "/%s/%s";
  public static final String ATHENA_COMPUTE_INSTANCE_PRICE_QUERY =
      "SELECT line_item_blended_rate, line_item_blended_cost, "
      + " line_item_unblended_rate, line_item_unblended_cost, "
      + " line_item_availability_zone, product_instance_type, product_operating_system, product_region, "
      + " product_vcpu, product_memory"
      + " FROM default.ccm_wo_resource_daily_athena "
      + " WHERE line_item_line_item_type != 'Tax' "
      + " AND line_item_usage_account_id  = '%s' AND line_item_usage_start_date = TIMESTAMP '%s' "
      + " AND line_item_product_code = 'AmazonEC2' AND product_servicecode = 'AmazonEC2' AND product_product_family = 'Compute Instance' "
      + " ORDER BY line_item_usage_start_date DESC";
  public static final String ATHENA_ECS_FARGATE_PRICE_QUERY =
      " SELECT line_item_blended_rate, line_item_blended_cost,  line_item_unblended_rate, line_item_unblended_cost, "
      + " product_region, product_cputype, product_memorytype "
      + " FROM default.ccm_wo_resource_daily_athena "
      + " WHERE line_item_line_item_type != 'Tax' "
      + " AND line_item_usage_account_id = '%s' "
      + " AND line_item_usage_start_date = TIMESTAMP '%s' "
      + " AND line_item_product_code = 'AmazonECS' AND product_servicecode = 'AmazonECS' "
      + " AND line_item_operation = 'FargateTask' "
      + " ORDER BY line_item_usage_start_date DESC ";
  public static final long SLEEP_AMOUNT_IN_MS = 3000;
  public static final String ATHENA_DEFAULT_DATABASE = "default";
}
