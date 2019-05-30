package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.EnvStateExecutionData.Builder.anEnvStateExecutionData;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.common.Constants.ENV_STATE_TIMEOUT_MILLIS;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.EnvStateExecutionData;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;

import java.util.concurrent.TimeUnit;

public class EnvStateTest extends WingsBaseTest {
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private ExecutionContextImpl context;
  @Mock private WorkflowService workflowService;
  @Mock private Workflow workflow;
  @Mock private CanaryOrchestrationWorkflow canaryOrchestrationWorkflow;
  private static final WorkflowStandardParams WORKFLOW_STANDARD_PARAMS =
      aWorkflowStandardParams().withAppId(APP_ID).withArtifactIds(asList(ARTIFACT_ID)).build();

  @InjectMocks private EnvState envState = new EnvState("ENV_STATE");

  @Before
  public void setUp() throws Exception {
    envState.setEnvId(ENV_ID);
    envState.setWorkflowId(WORKFLOW_ID);
    when(context.getApp()).thenReturn(anApplication().uuid(APP_ID).build());
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(WORKFLOW_STANDARD_PARAMS);
    when(context.getWorkflowExecutionId()).thenReturn(PIPELINE_WORKFLOW_EXECUTION_ID);
    when(workflowExecutionService.triggerOrchestrationExecution(
             eq(APP_ID), eq(ENV_ID), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any(), any()))
        .thenReturn(WorkflowExecution.builder().uuid(WORKFLOW_EXECUTION_ID).status(ExecutionStatus.NEW).build());
    when(workflowService.readWorkflowWithoutServices(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldExecute() {
    when(workflow.getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);
    ExecutionResponse executionResponse = envState.execute(context);
    verify(workflowExecutionService)
        .triggerOrchestrationExecution(
            eq(APP_ID), eq(ENV_ID), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any(), any());
    assertThat(executionResponse.getCorrelationIds()).hasSameElementsAs(asList(WORKFLOW_EXECUTION_ID));
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(executionResponse.isAsync()).isTrue();
    EnvStateExecutionData stateExecutionData = (EnvStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(stateExecutionData.getWorkflowId()).isEqualTo(WORKFLOW_ID);
    assertThat(stateExecutionData.getWorkflowExecutionId()).isEqualTo(WORKFLOW_EXECUTION_ID);
    assertThat(stateExecutionData.getEnvId()).isEqualTo(ENV_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSkipDisabledStep() {
    when(workflow.getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);
    envState.setDisable(true);
    ExecutionResponse executionResponse = envState.execute(context);
    verify(workflowExecutionService, times(0))
        .triggerOrchestrationExecution(
            eq(APP_ID), eq(ENV_ID), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any(), any());
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SKIPPED);
    assertThat(executionResponse.getErrorMessage()).isNotEmpty();
  }
  @Test
  @Category(UnitTests.class)
  public void shouldExecuteWhenNoWorkflow() {
    ExecutionResponse executionResponse = envState.execute(context);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(FAILED);
    assertThat(executionResponse.getErrorMessage()).isNotEmpty();
  }

  @Test
  @Owner(emails = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldExecuteOnError() {
    when(workflow.getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);
    when(workflowExecutionService.triggerOrchestrationExecution(eq(APP_ID), eq(ENV_ID), eq(WORKFLOW_ID),
             eq(PIPELINE_WORKFLOW_EXECUTION_ID), any(ExecutionArgs.class), any()))
        .thenThrow(new InvalidRequestException("Workflow variable [test] is mandatory for execution"));
    ExecutionResponse executionResponse = envState.execute(context);

    verify(workflowExecutionService)
        .triggerOrchestrationExecution(eq(APP_ID), eq(ENV_ID), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID),
            any(ExecutionArgs.class), any());
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(FAILED);
    assertThat(executionResponse.getErrorMessage()).isNotEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetTimeout() {
    Integer timeoutMillis = envState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo(ENV_STATE_TIMEOUT_MILLIS);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetSetTimeout() {
    envState.setTimeoutMillis((int) TimeUnit.HOURS.toMillis(1));
    Integer timeoutMillis = envState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo((int) TimeUnit.HOURS.toMillis(1));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldHandleAbort() {
    envState.setTimeoutMillis((int) (0.6 * TimeUnit.HOURS.toMillis(1)));
    when(context.getStateExecutionData())
        .thenReturn(anEnvStateExecutionData().withWorkflowId(WORKFLOW_ID).withEnvId(ENV_ID).build());
    envState.handleAbortEvent(context);
    assertThat(context.getStateExecutionData()).isNotNull();
    assertThat(context.getStateExecutionData().getErrorMsg()).contains("Workflow not completed within 36m");
  }
}
