package io.harness.ccm.billing;

import static io.harness.ccm.billing.GcpBillingFilter.BILLING_GCP_STARTTIME;
import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import com.healthmarketscience.sqlbuilder.Condition;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;

public class GcpBillingFilterTest extends CategoryTest {
  private Number value = 0;
  private GcpBillingFilter gcpBillingFilter;

  @Before
  public void setUp() {
    gcpBillingFilter = new GcpBillingFilter();
    gcpBillingFilter.setStartTime(BillingTimeFilter.builder()
                                      .operator(QLTimeOperator.AFTER)
                                      .variable(BILLING_GCP_STARTTIME)
                                      .value(value)
                                      .build());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testToCondition() {
    Condition condition = gcpBillingFilter.toCondition();
    assertThat(condition.toString()).isEqualTo("(t0.usage_start_time >= '1970-01-01T00:00:00Z')");
  }
}
