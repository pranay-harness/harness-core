/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.metrics.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.metrics.AutoMetricContext;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MetricContextBuilderTest extends CvNextGenTestBase {
  @Inject private MetricContextBuilder metricContextBuilder;
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetContext() {
    String accountId = generateUuid();
    CVNGStepTask cvngStepTask = CVNGStepTask.builder()
                                    .accountId(accountId)
                                    .activityId(generateUuid())
                                    .status(CVNGStepTask.Status.IN_PROGRESS)
                                    .build();
    try (AutoMetricContext autoMetricContext = metricContextBuilder.getContext(cvngStepTask, CVNGStepTask.class)) {
      assertThat(autoMetricContext).isNotNull();
    }
  }
}
