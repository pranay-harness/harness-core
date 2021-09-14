/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.ccm;

import static io.harness.rule.OwnerRule.HITESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.beans.CostAttribution;
import io.harness.ccm.commons.beans.InstanceType;
import io.harness.ccm.commons.beans.PricingGroup;
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
