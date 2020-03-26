package io.harness.ccm.billing;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.bigquery.TruncExpression;
import io.harness.ccm.billing.graphql.GcpBillingGroupby;
import io.harness.ccm.billing.graphql.TimeTruncGroupby;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GcpBillingGroupbyTest extends CategoryTest {
  private GcpBillingGroupby gcpBillingGroupby;

  @Before
  public void setUp() {
    gcpBillingGroupby = new GcpBillingGroupby();
    gcpBillingGroupby.setTimeTruncGroupby(TimeTruncGroupby.builder().resolution(TruncExpression.DatePart.DAY).build());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testToGroupbyObject() {
    Object groupbyObject = gcpBillingGroupby.toGroupbyObject();
    assertThat(groupbyObject.toString()).isEqualTo("TIMESTAMP_TRUNC(t0.usage_start_time,DAY) AS start_time_trunc");
  }
}
