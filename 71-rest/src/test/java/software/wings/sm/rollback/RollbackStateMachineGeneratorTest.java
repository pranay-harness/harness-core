package software.wings.sm.rollback;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.service.impl.workflow.WorkflowServiceTestHelper.constructBasicWorkflowWithRollbackForAMI;
import static software.wings.sm.StateType.AWS_AMI_ROLLBACK_SWITCH_ROUTES;
import static software.wings.sm.StateType.AWS_AMI_SERVICE_DEPLOY;
import static software.wings.sm.StateType.AWS_AMI_SERVICE_ROLLBACK;
import static software.wings.sm.StateType.AWS_AMI_SERVICE_SETUP;
import static software.wings.sm.StateType.AWS_AMI_SWITCH_ROUTES;
import static software.wings.sm.StateType.PHASE;
import static software.wings.sm.StateType.PHASE_STEP;
import static software.wings.sm.StateType.STAGING_ORIGINAL_EXECUTION;
import static software.wings.sm.StateType.SUB_WORKFLOW;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.CustomOrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.exception.InvalidRollbackException;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateMachine;
import software.wings.sm.StateTypeDescriptor;

import java.util.Map;

public class RollbackStateMachineGeneratorTest extends WingsBaseTest {
  @Mock WorkflowService workflowService;
  @Mock WorkflowExecutionService workflowExecutionService;
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
          .put("STAGING_ORIGINAL_EXECUTION", STAGING_ORIGINAL_EXECUTION)
          .build();

  private final WorkflowExecution execution = WorkflowExecution.builder()
                                                  .appId(APP_ID)
                                                  .envType(NON_PROD)
                                                  .status(ExecutionStatus.SUCCESS)
                                                  .workflowType(WorkflowType.ORCHESTRATION)
                                                  .uuid(generateUuid())
                                                  .workflowId(WORKFLOW_ID)
                                                  .build();

  private final WorkflowExecution runningExecution = WorkflowExecution.builder()
                                                         .appId(APP_ID)
                                                         .envType(NON_PROD)
                                                         .status(ExecutionStatus.RUNNING)
                                                         .workflowType(WorkflowType.ORCHESTRATION)
                                                         .uuid(generateUuid())
                                                         .workflowId(WORKFLOW_ID)
                                                         .build();

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldGenerateForRollbackExecution() {
    Workflow workflow = constructBasicWorkflowWithRollbackForAMI();
    when(workflowExecutionService.getWorkflowExecution(APP_ID, WORKFLOW_EXECUTION_ID)).thenReturn(execution);
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    when(workflowService.stencilMap(any())).thenReturn(stencilMap);
    StateMachine originalStateMachine = new StateMachine(workflow, workflow.getDefaultVersion(),
        ((CustomOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getGraph(), stencilMap, true, false);
    assertThat(originalStateMachine).isNotNull();
    assertThat(originalStateMachine.getStates()).hasSize(4);
    assertThat(originalStateMachine.getChildStateMachines()).hasSize(5);

    StateMachine sm = stateMachineGenerator.generateForRollbackExecution(APP_ID, WORKFLOW_EXECUTION_ID, true);
    assertThat(sm).isNotNull();
    assertThat(sm.getStates()).hasSize(4);
    assertThat(sm.getChildStateMachines()).hasSize(4);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldNotGenerateForRollback() {
    Workflow workflow = constructBasicWorkflowWithRollbackForAMI();
    when(workflowExecutionService.getWorkflowExecution(APP_ID, WORKFLOW_EXECUTION_ID)).thenReturn(runningExecution);
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    when(workflowService.stencilMap(any())).thenReturn(stencilMap);
    assertThatThrownBy(() -> stateMachineGenerator.generateForRollbackExecution(APP_ID, WORKFLOW_EXECUTION_ID, true))
        .isInstanceOf(InvalidRollbackException.class);
  }
}