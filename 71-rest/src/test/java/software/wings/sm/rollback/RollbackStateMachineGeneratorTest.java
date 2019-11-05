package software.wings.sm.rollback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.service.impl.workflow.WorkflowServiceTestHelper.constructBasicWorkflowWithRollbackForAMI;
import static software.wings.sm.StateType.AWS_AMI_ROLLBACK_SWITCH_ROUTES;
import static software.wings.sm.StateType.AWS_AMI_SERVICE_DEPLOY;
import static software.wings.sm.StateType.AWS_AMI_SERVICE_ROLLBACK;
import static software.wings.sm.StateType.AWS_AMI_SERVICE_SETUP;
import static software.wings.sm.StateType.AWS_AMI_SWITCH_ROUTES;
import static software.wings.sm.StateType.PHASE;
import static software.wings.sm.StateType.PHASE_STEP;
import static software.wings.sm.StateType.SUB_WORKFLOW;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.CustomOrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateMachine;
import software.wings.sm.StateTypeDescriptor;

import java.util.Map;

public class RollbackStateMachineGeneratorTest extends WingsBaseTest {
  @Mock WorkflowService workflowService;
  @InjectMocks @Inject RollbackStateMachineGenerator stateMachineGenerator;

  private final Map<String, StateTypeDescriptor> stencilMap =
      ImmutableMap.<String, StateTypeDescriptor>builder()
          .put("PHASE", PHASE)
          .put("PHASE_STEP", PHASE_STEP)
          .put("SUB_WORK_FLOW", SUB_WORKFLOW)
          .put("AWS_AMI_SERVICE_SETUP", AWS_AMI_SERVICE_SETUP)
          .put("AWS_AMI_SERVICE_DEPLOY", AWS_AMI_SERVICE_DEPLOY)
          .put("AWS_AMI_SWITCH_ROUTES", AWS_AMI_SWITCH_ROUTES)
          .put("AWS_AMI_ROLLBACK_SWITCH_ROUTES", AWS_AMI_ROLLBACK_SWITCH_ROUTES)
          .put("AWS_AMI_SERVICE_ROLLBACK", AWS_AMI_SERVICE_ROLLBACK)
          .build();

  @Test
  @Category(UnitTests.class)
  public void generateForRollback() {
    Workflow workflow = constructBasicWorkflowWithRollbackForAMI();
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    when(workflowService.stencilMap(any())).thenReturn(stencilMap);
    StateMachine originalStateMachine = new StateMachine(workflow, workflow.getDefaultVersion(),
        ((CustomOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getGraph(), stencilMap, true);
    assertThat(originalStateMachine).isNotNull();
    assertThat(originalStateMachine.getStates()).hasSize(4);
    assertThat(originalStateMachine.getChildStateMachines()).hasSize(5);

    StateMachine sm = stateMachineGenerator.generateForRollback(APP_ID, WORKFLOW_ID, true);
    assertThat(sm).isNotNull();
    assertThat(sm.getStates()).hasSize(4);
    assertThat(sm.getChildStateMachines()).hasSize(2);
  }
}