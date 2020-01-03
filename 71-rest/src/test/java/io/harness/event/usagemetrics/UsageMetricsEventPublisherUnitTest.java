package io.harness.event.usagemetrics;

import static io.harness.rule.OwnerRule.RUSHABH;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.event.usagemetrics.UsageMetricsTestUtils.UsageMetricsTestKeys;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentTimeSeriesEvent;
import software.wings.beans.WorkflowExecution;

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
