/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.config.k8s.recommendation.estimators;

import static io.harness.rule.OwnerRule.AVMOHAN;

import static software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement.CPU;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement.MEMORY;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OmitResourceEstimatorTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testOmitResource() throws Exception {
    ResourceEstimator estimator = ConstEstimator.of(ResourceAmountUtils.makeResourceMap(1000, 200)).omitResources(CPU);
    Map<String, Long> resourceEstimation = estimator.getResourceEstimation(null);
    assertThat(resourceEstimation.get(CPU)).isNull();
    assertThat(resourceEstimation.get(MEMORY)).isEqualTo(200);
  }
}
