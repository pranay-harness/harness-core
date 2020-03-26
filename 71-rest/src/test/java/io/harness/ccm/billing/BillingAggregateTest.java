package io.harness.ccm.billing;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import com.healthmarketscience.sqlbuilder.SqlObject;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.graphql.BillingAggregate;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.graphql.datafetcher.billing.QLCCMAggregateOperation;

public class BillingAggregateTest extends CategoryTest {
  private BillingAggregate billingAggregate;

  @Before
  public void setUp() {
    billingAggregate = BillingAggregate.builder().operationType(QLCCMAggregateOperation.SUM).columnName("cost").build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testToFunctionCall() {
    SqlObject functionCall = billingAggregate.toFunctionCall();
    assertThat(functionCall.toString()).isEqualTo("SUM(t0.cost) AS sum_cost");
  }
}
