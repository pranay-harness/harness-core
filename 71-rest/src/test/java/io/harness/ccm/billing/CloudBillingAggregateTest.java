package io.harness.ccm.billing;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import com.healthmarketscience.sqlbuilder.SqlObject;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.graphql.CloudBillingAggregate;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.graphql.datafetcher.billing.QLCCMAggregateOperation;

public class CloudBillingAggregateTest extends CategoryTest {
  private CloudBillingAggregate cloudBillingAggregate;
  private CloudBillingAggregate rawTableCloudBillingAggregate;

  @Before
  public void setUp() {
    cloudBillingAggregate =
        CloudBillingAggregate.builder().operationType(QLCCMAggregateOperation.SUM).columnName("cost").build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testToFunctionCall() {
    SqlObject functionCall = cloudBillingAggregate.toFunctionCall();
    assertThat(functionCall.toString()).isEqualTo("SUM(t0.cost) AS sum_cost");
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testToRawTableFunctionCall() {
    rawTableCloudBillingAggregate =
        CloudBillingAggregate.builder().operationType(QLCCMAggregateOperation.SUM).columnName("cost").build();
    SqlObject functionCall = rawTableCloudBillingAggregate.toRawTableFunctionCall();
    assertThat(functionCall.toString()).isEqualTo("SUM(t0.cost) AS sum_cost");

    rawTableCloudBillingAggregate =
        CloudBillingAggregate.builder().operationType(QLCCMAggregateOperation.AVG).columnName("cost").build();
    functionCall = rawTableCloudBillingAggregate.toRawTableFunctionCall();
    assertThat(functionCall.toString()).isEqualTo("AVG(t0.cost) AS avg_cost");

    rawTableCloudBillingAggregate =
        CloudBillingAggregate.builder().operationType(QLCCMAggregateOperation.MIN).columnName("startTime").build();
    functionCall = rawTableCloudBillingAggregate.toRawTableFunctionCall();
    assertThat(functionCall.toString()).isEqualTo("MIN(t0.usage_start_time) AS min_startTime");

    rawTableCloudBillingAggregate =
        CloudBillingAggregate.builder().operationType(QLCCMAggregateOperation.MAX).columnName("startTime").build();
    functionCall = rawTableCloudBillingAggregate.toRawTableFunctionCall();
    assertThat(functionCall.toString()).isEqualTo("MAX(t0.usage_start_time) AS max_startTime");
  }
}
