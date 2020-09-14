package io.harness.batch.processing.billing.timeseries.service.support;

import static io.harness.batch.processing.ccm.BatchJobType.ACTUAL_IDLE_COST_BILLING_HOURLY;
import static io.harness.batch.processing.ccm.BatchJobType.INSTANCE_BILLING_HOURLY;
import static io.harness.batch.processing.ccm.BatchJobType.UNALLOCATED_BILLING_HOURLY;

import com.google.common.collect.ImmutableSet;

import io.harness.batch.processing.ccm.BatchJobType;
import org.springframework.stereotype.Component;

@Component
public class BillingDataTableNameProvider {
  private static final String DAILY_BILLING_DATA_TABLE = "BILLING_DATA";
  private static final String HOURLY_BILLING_DATA_TABLE = "BILLING_DATA_HOURLY";

  public static String replaceTableName(String statement, BatchJobType batchJobType) {
    return String.format(statement, getTableName(batchJobType));
  }

  private BillingDataTableNameProvider() {}

  public static String getTableName(BatchJobType batchJobType) {
    String tableName = DAILY_BILLING_DATA_TABLE;
    if (ImmutableSet.of(INSTANCE_BILLING_HOURLY, ACTUAL_IDLE_COST_BILLING_HOURLY, UNALLOCATED_BILLING_HOURLY)
            .contains(batchJobType)) {
      tableName = HOURLY_BILLING_DATA_TABLE;
    }
    return tableName;
  }
}
