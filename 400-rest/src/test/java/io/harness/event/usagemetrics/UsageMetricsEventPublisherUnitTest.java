/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
