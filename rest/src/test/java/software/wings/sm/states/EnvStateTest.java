package software.wings.sm.states;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.EnvStateExecutionData.Builder.anEnvStateExecutionData;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.common.Constants.ENV_STATE_TIMEOUT_MILLIS;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.SKIPPED;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import io.harness.exception.InvalidRequestException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.EnvStateExecutionData;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;

import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 11/2/16.
 */

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
    when(context.getApp()).thenReturn(anApplication().withUuid(APP_ID).build());
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(WORKFLOW_STANDARD_PARAMS);
    when(context.getWorkflowExecutionId()).thenReturn(PIPELINE_WORKFLOW_EXECUTION_ID);
    when(workflowExecutionService.triggerOrchestrationExecution(
             eq(APP_ID), eq(ENV_ID), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any()))
        .thenReturn(aWorkflowExecution().withUuid(WORKFLOW_EXECUTION_ID).build());
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
  }

  @Test
  public void shouldExecute() {
    when(workflow.getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);
    ExecutionResponse executionResponse = envState.execute(context);
    verify(workflowExecutionService)
        .triggerOrchestrationExecution(
            eq(APP_ID), eq(ENV_ID), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any());
    assertThat(executionResponse.getCorrelationIds()).hasSameElementsAs(asList(WORKFLOW_EXECUTION_ID));
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(executionResponse.isAsync()).isTrue();
    EnvStateExecutionData stateExecutionData = (EnvStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(stateExecutionData.getWorkflowId()).isEqualTo(WORKFLOW_ID);
    assertThat(stateExecutionData.getWorkflowExecutionId()).isEqualTo(WORKFLOW_EXECUTION_ID);
    assertThat(stateExecutionData.getEnvId()).isEqualTo(ENV_ID);
  }

  @Test
  public void shouldSkipDisabledStep() {
    when(workflow.getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);
    envState.setDisable(true);
    ExecutionResponse executionResponse = envState.execute(context);
    verify(workflowExecutionService, times(0))
        .triggerOrchestrationExecution(
            eq(APP_ID), eq(ENV_ID), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any());
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SKIPPED);
    assertThat(executionResponse.getErrorMessage()).isNotEmpty();
  }
  @Test
  public void shouldExecuteWhenNoWorkflow() {
    ExecutionResponse executionResponse = envState.execute(context);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(FAILED);
    assertThat(executionResponse.getErrorMessage()).isNotEmpty();
  }

  @Ignore
  @Test
  public void shouldExecuteOnError() {
    when(workflow.getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);
    when(workflowExecutionService.triggerOrchestrationExecution(
             eq(APP_ID), eq(ENV_ID), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any()))
        .thenThrow(new InvalidRequestException("Workflow variable [test] is mandatory for execution"));
    ExecutionResponse executionResponse = envState.execute(context);
    verify(workflowExecutionService)
        .triggerOrchestrationExecution(
            eq(APP_ID), eq(ENV_ID), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any());
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(FAILED);
    assertThat(executionResponse.getErrorMessage())
        .isNotEmpty()
        .isEqualTo("Invalid request: Workflow variable [test] is mandatory for execution");
  }

  @Test
  public void shouldGetTimeout() {
    Integer timeoutMillis = envState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo(ENV_STATE_TIMEOUT_MILLIS);
  }

  @Test
  public void shouldGetSetTimeout() {
    envState.setTimeoutMillis((int) TimeUnit.HOURS.toMillis(1));
    Integer timeoutMillis = envState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo((int) TimeUnit.HOURS.toMillis(1));
  }

  @Test
  public void shouldHandleAbort() {
    envState.setTimeoutMillis((int) (0.6 * TimeUnit.HOURS.toMillis(1)));
    when(context.getStateExecutionData())
        .thenReturn(anEnvStateExecutionData().withWorkflowId(WORKFLOW_ID).withEnvId(ENV_ID).build());
    envState.handleAbortEvent(context);
    assertThat(context.getStateExecutionData()).isNotNull();
    assertThat(context.getStateExecutionData().getErrorMsg()).contains("Workflow not completed within 36m");
  }
}
