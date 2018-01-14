package software.wings.sm.states;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.EnvStateExecutionData;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.ErrorCode;
import software.wings.beans.Workflow;
import software.wings.exception.WingsException;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.WorkflowStandardParams;

/**
 * Created by anubhaw on 11/2/16.
 */

public class EnvStateTest extends WingsBaseTest {
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private PipelineService pipelineService;
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
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.isAsync()).isTrue();
    EnvStateExecutionData stateExecutionData = (EnvStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(stateExecutionData.getWorkflowId()).isEqualTo(WORKFLOW_ID);
    assertThat(stateExecutionData.getWorkflowExecutionId()).isEqualTo(WORKFLOW_EXECUTION_ID);
    assertThat(stateExecutionData.getEnvId()).isEqualTo(ENV_ID);
  }

  @Test
  public void shouldExecuteWhenNoWorkflow() {
    ExecutionResponse executionResponse = envState.execute(context);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(executionResponse.getErrorMessage()).isNotEmpty();
  }

  @Test
  public void shouldExecuteOnError() {
    when(workflow.getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);
    when(workflowExecutionService.triggerOrchestrationExecution(
             eq(APP_ID), eq(ENV_ID), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any()))
        .thenThrow(new WingsException(ErrorCode.INVALID_REQUEST)
                       .addParam("message", "Workflow variable [test] is mandatory for execution"));
    ExecutionResponse executionResponse = envState.execute(context);
    verify(workflowExecutionService)
        .triggerOrchestrationExecution(
            eq(APP_ID), eq(ENV_ID), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any());
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(executionResponse.getErrorMessage())
        .isNotEmpty()
        .isEqualTo("INVALID_REQUEST - Workflow variable [test] is mandatory for execution. ");
  }
}
