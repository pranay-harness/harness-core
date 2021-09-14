/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.config.k8s.recommendation.estimators;

import static io.harness.batch.processing.config.k8s.recommendation.estimators.ResourceAmountUtils.makeResourceMap;
import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MinResourceEstimatorTest extends CategoryTest {
  private static final long MIN_CPU_AMOUNT = 25;
  private static final long MIN_MEMORY_AMOUNT = 250 * 1024 * 1024;

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testMinResourceEstimatorMemory() throws Exception {
    long baseCpuAmount = 100;
    long baseMemoryAmount = 100 * 1024 * 1024;
    ResourceEstimator estimator = ConstEstimator.of(makeResourceMap(baseCpuAmount, baseMemoryAmount))
                                      .withMinResources(makeResourceMap(MIN_CPU_AMOUNT, MIN_MEMORY_AMOUNT));
    assertThat(estimator.getResourceEstimation(null)).isEqualTo(makeResourceMap(baseCpuAmount, MIN_MEMORY_AMOUNT));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testMinResourceEstimatorCpu() throws Exception {
    long baseCpuAmount = 10;
    long baseMemoryAmount = 300 * 1024 * 1024;
    ResourceEstimator estimator = ConstEstimator.of(makeResourceMap(baseCpuAmount, baseMemoryAmount))
                                      .withMinResources(makeResourceMap(MIN_CPU_AMOUNT, MIN_MEMORY_AMOUNT));
    assertThat(estimator.getResourceEstimation(null)).isEqualTo(makeResourceMap(MIN_CPU_AMOUNT, baseMemoryAmount));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testMinResourceEstimatorBoth() throws Exception {
    long baseCpuAmount = 750;
    long baseMemoryAmount = 300 * 1024 * 1024;
    ResourceEstimator estimator = ConstEstimator.of(makeResourceMap(baseCpuAmount, baseMemoryAmount))
                                      .withMinResources(makeResourceMap(MIN_CPU_AMOUNT, MIN_MEMORY_AMOUNT));
    assertThat(estimator.getResourceEstimation(null)).isEqualTo(makeResourceMap(baseCpuAmount, baseMemoryAmount));
  }
}
