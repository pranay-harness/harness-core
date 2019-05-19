package software.wings.licensing.violations.checkers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static software.wings.service.impl.PreDeploymentCheckerTest.getWorkflow;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse.PageResponseBuilder;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureUsageViolation;
import software.wings.beans.FeatureViolation;
import software.wings.service.intfc.WorkflowService;

import java.util.Collections;
import java.util.List;

public class FlowControlViolationCheckerTest extends WingsBaseTest {
  private static final String TEST_ACCOUNT_ID = "ACCOUNT_ID";

  @Mock private WorkflowService workflowService;
  @InjectMocks @Inject private FlowControlViolationChecker flowControlViolationChecker;

  @Test
  @Category(UnitTests.class)
  public void workflowWithFlowControl() {
    when(workflowService.listWorkflows(Mockito.any(PageRequest.class)))
        .thenReturn(
            PageResponseBuilder.aPageResponse().withResponse(Collections.singletonList(getWorkflow(true))).build());
    List<FeatureViolation> featureViolationList =
        flowControlViolationChecker.check(TEST_ACCOUNT_ID, AccountType.COMMUNITY);
    assertNotNull(featureViolationList);
    assertEquals(1, featureViolationList.size());
    assertEquals(((FeatureUsageViolation) featureViolationList.get(0)).getUsageCount(), 1);
    assertEquals(
        ((FeatureUsageViolation) featureViolationList.get(0)).getUsages().get(0).getEntityName(), WORKFLOW_NAME);
  }

  //@Test
  @Category(UnitTests.class)
  public void workflowWithoutFlowControl() {
    when(workflowService.listWorkflows(Mockito.any(PageRequest.class)))
        .thenReturn(
            PageResponseBuilder.aPageResponse().withResponse(Collections.singletonList(getWorkflow(false))).build());
    List<FeatureViolation> featureViolationList =
        flowControlViolationChecker.check(TEST_ACCOUNT_ID, AccountType.COMMUNITY);
    assertNotNull(featureViolationList);
    assertEquals(0, featureViolationList.size());
  }
}
