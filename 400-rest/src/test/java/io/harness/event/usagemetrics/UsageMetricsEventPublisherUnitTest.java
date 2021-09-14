/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.usagemetrics;

import static io.harness.rule.OwnerRule.RUSHABH;

import io.harness.category.element.UnitTests;
import io.harness.event.usagemetrics.UsageMetricsTestUtils.UsageMetricsTestKeys;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.api.DeploymentTimeSeriesEvent;
import software.wings.beans.WorkflowExecution;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class UsageMetricsEventPublisherUnitTest extends WingsBaseTest {
  @Inject UsageMetricsEventPublisher usageMetricsEventPublisher;

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testConstructDeploymentEvent() {
    WorkflowExecution workflowExecution = UsageMetricsTestUtils.generateWorkflowExecution(0);
    DeploymentTimeSeriesEvent timeSeriesEvent = usageMetricsEventPublisher.constructDeploymentTimeSeriesEvent(
        UsageMetricsTestKeys.ACCOUNTID, workflowExecution);
    UsageMetricsTestUtils.validateTimeSeriesEventInfo(timeSeriesEvent, 0);
  }
}
