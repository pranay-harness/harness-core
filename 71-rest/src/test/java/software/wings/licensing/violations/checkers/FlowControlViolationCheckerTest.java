package software.wings.licensing.violations.checkers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.DEPLOY_SERVICE;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse.PageResponseBuilder;
import io.harness.beans.WorkflowType;
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
import software.wings.beans.GraphNode;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.Collections;
import java.util.List;

public class FlowControlViolationCheckerTest extends WingsBaseTest {
  @Mock private WorkflowService workflowService;
  @InjectMocks @Inject private FlowControlViolationChecker flowControlViolationChecker;
  public static final String TEST_ACCOUNT_ID = "ACCOUNT_ID";

  @Test
  @Category(UnitTests.class)
  public void workflowWithFlowControl() {
    when(workflowService.listWorkflows(Mockito.any(PageRequest.class)))
        .thenReturn(PageResponseBuilder.aPageResponse()
                        .withResponse(Collections.singletonList(constructBasicWorkflow(true)))
                        .build());
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
        .thenReturn(PageResponseBuilder.aPageResponse()
                        .withResponse(Collections.singletonList(constructBasicWorkflow(false)))
                        .build());
    List<FeatureViolation> featureViolationList =
        flowControlViolationChecker.check(TEST_ACCOUNT_ID, AccountType.COMMUNITY);
    assertNotNull(featureViolationList);
    assertEquals(0, featureViolationList.size());
  }

  private Workflow constructBasicWorkflow(boolean addFlowControl) {
    return aWorkflow()
        .name(WORKFLOW_NAME)
        .uuid(generateUuid())
        .appId(APP_ID)
        .accountId(TEST_ACCOUNT_ID)
        .serviceId(SERVICE_ID)
        .infraMappingId(INFRA_MAPPING_ID)
        .workflowType(WorkflowType.ORCHESTRATION)
        .envId(ENV_ID)
        .orchestrationWorkflow(
            aBasicOrchestrationWorkflow()
                .withPreDeploymentSteps(
                    aPhaseStep(PRE_DEPLOYMENT)
                        .addStep(GraphNode.builder()
                                     .type(addFlowControl ? StateType.BARRIER.name() : StateType.SUB_WORKFLOW.name())
                                     .build())
                        .build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).build())
                .withWorkflowPhases(Collections.singletonList(
                    WorkflowPhaseBuilder.aWorkflowPhase()
                        .phaseSteps(Collections.singletonList(

                            aPhaseStep(DEPLOY_SERVICE)
                                .addStep(GraphNode.builder()
                                             .type(addFlowControl ? StateType.RESOURCE_CONSTRAINT.name()
                                                                  : StateType.SUB_WORKFLOW.name())
                                             .build()

                                        )
                                .build()

                                ))
                        .build()

                        ))
                .build())
        .build();
  }
}
