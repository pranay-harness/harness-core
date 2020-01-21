package io.harness.ccm.budget;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.billing.QLCCMAggregateOperation;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLTimeGroupType;

@UtilityClass
@Slf4j
public class BudgetUtils {
  public static QLCCMAggregationFunction makeBillingAmtAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName("billingamount")
        .build();
  }

  // Todo: add month aggregation
  public static QLCCMTimeSeriesAggregation makeStartTimeEntityGroupBy() {
    return QLCCMTimeSeriesAggregation.builder().timeGroupType(QLTimeGroupType.MONTH).build();
  }
}
