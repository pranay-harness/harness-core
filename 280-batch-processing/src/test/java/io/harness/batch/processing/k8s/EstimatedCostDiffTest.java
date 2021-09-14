/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.k8s;

import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.math.BigDecimal;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EstimatedCostDiffTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testZeroOldCost() throws Exception {
    assertThat(new EstimatedCostDiff(BigDecimal.ZERO, BigDecimal.TEN).getDiffAmountPercent()).isNull();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testDiffAmountPercent() throws Exception {
    assertThat(new EstimatedCostDiff(BigDecimal.valueOf(1000), BigDecimal.valueOf(1200)).getDiffAmountPercent())
        .isEqualTo(BigDecimal.valueOf(2000, 2));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testDiffAmountPercentRound() throws Exception {
    EstimatedCostDiff estimatedCostDiff = new EstimatedCostDiff(BigDecimal.valueOf(900), BigDecimal.valueOf(1200));
    assertThat(estimatedCostDiff.getDiffAmount()).isEqualTo(BigDecimal.valueOf(300));
    assertThat(estimatedCostDiff.getDiffAmountPercent()).isEqualTo(BigDecimal.valueOf(3333, 2));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testDiffAmountPercentRoundNegative() throws Exception {
    EstimatedCostDiff estimatedCostDiff = new EstimatedCostDiff(BigDecimal.valueOf(900), BigDecimal.valueOf(600));
    assertThat(estimatedCostDiff.getDiffAmount()).isEqualTo(BigDecimal.valueOf(-300));
    assertThat(estimatedCostDiff.getDiffAmountPercent()).isEqualTo(BigDecimal.valueOf(-3333, 2));
  }
}
