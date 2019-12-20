package io.harness.ccm.budget;

import com.google.inject.Singleton;

import lombok.experimental.UtilityClass;
import software.wings.graphql.datafetcher.billing.QLCCMAggregateOperation;
import software.wings.graphql.datafetcher.billing.QLCCMAggregationFunction;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeAggregationType;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;

import java.time.LocalDate;
import java.time.ZoneId;

@Singleton
@UtilityClass
public class BudgetUtils {
  private static final long DAY_IN_MILLI_SECONDS = 86400000L;

  public static QLCCMAggregationFunction makeBillingAmtAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName("billingamount")
        .build();
  }

  // Todo: add month aggregation
  public static QLTimeSeriesAggregation makeStartTimeEntityGroupBy() {
    return QLTimeSeriesAggregation.builder()
        .timeAggregationType(QLTimeAggregationType.DAY)
        .timeAggregationValue(1)
        .build();
  }

  public static QLBillingDataFilter makeClusterFilter(String[] values) {
    QLIdFilter clusterFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLBillingDataFilter.builder().cluster(clusterFilter).build();
  }

  public static QLBillingDataFilter makeApplicationFilter(String[] values) {
    QLIdFilter applicationFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLBillingDataFilter.builder().application(applicationFilter).build();
  }

  // Todo: check round off is correct
  public static long roundOffBudgetCreationTime(long createdAt) {
    LocalDate date = LocalDate.ofEpochDay(createdAt / DAY_IN_MILLI_SECONDS);
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
  }
}
