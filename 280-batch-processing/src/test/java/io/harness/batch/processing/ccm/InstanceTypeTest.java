package io.harness.batch.processing.ccm;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InstanceTypeTest extends CategoryTest {
  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testInstanceType() {
    InstanceType instanceType = InstanceType.EC2_INSTANCE;
    PricingGroup pricingGroup = instanceType.getPricingGroup();
    CostAttribution costAttribution = instanceType.getCostAttribution();
    double minChargeableSeconds = instanceType.getMinChargeableSeconds();
    assertThat(pricingGroup).isEqualTo(PricingGroup.COMPUTE);
    assertThat(costAttribution).isEqualTo(CostAttribution.COMPLETE);
    assertThat(minChargeableSeconds).isEqualTo(3600);
  }
}
