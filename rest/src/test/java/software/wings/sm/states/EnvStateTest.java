package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.EnvStateExecutionData;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.EnvState.EnvExecutionResponseData;

import java.util.Arrays;

/**
 * Created by anubhaw on 11/2/16.
 */

public class EnvStateTest extends WingsBaseTest {
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private PipelineService pipelineService;
  @Mock private ExecutionContextImpl context;
  private static final WorkflowStandardParams WORKFLOW_STANDARD_PARAMS =
      aWorkflowStandardParams().withArtifactIds(Arrays.asList(ARTIFACT_ID)).build();

  @InjectMocks private EnvState envState = new EnvState("ENV_STATE");

  @Before
  public void setUp() throws Exception {
    envState.setEnvId(ENV_ID);
    envState.setWorkflowId(WORKFLOW_ID);
    when(context.getApp()).thenReturn(anApplication().withUuid(APP_ID).build());
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(WORKFLOW_STANDARD_PARAMS);
    when(context.getWorkflowExecutionId()).thenReturn(PIPELINE_WORKFLOW_EXECUTION_ID);
    when(workflowExecutionService.triggerEnvExecution(eq(APP_ID), eq(ENV_ID), any()))
        .thenReturn(aWorkflowExecution().withUuid(WORKFLOW_EXECUTION_ID).build());
  }

  @Test
  public void shouldExecute() {
    ExecutionResponse executionResponse = envState.execute(context);
    verify(workflowExecutionService).triggerEnvExecution(eq(APP_ID), eq(ENV_ID), any());
    verify(pipelineService).refreshPipelineExecutionAsync(APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID);
    assertThat(executionResponse.getCorrelationIds()).hasSameElementsAs(Arrays.asList(WORKFLOW_EXECUTION_ID));
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.isAsync()).isTrue();
    EnvStateExecutionData stateExecutionData = (EnvStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(stateExecutionData.getWorkflowId()).isEqualTo(WORKFLOW_ID);
    assertThat(stateExecutionData.getWorkflowExecutionId()).isEqualTo(WORKFLOW_EXECUTION_ID);
    assertThat(stateExecutionData.getEnvId()).isEqualTo(ENV_ID);
  }

  @Test
  public void shouldHandleAsyncResponse() {
    ExecutionResponse executionResponse = envState.handleAsyncResponse(context,
        ImmutableMap.of(
            ACTIVITY_ID, new EnvExecutionResponseData(PIPELINE_WORKFLOW_EXECUTION_ID, ExecutionStatus.SUCCESS)));
    assertThat(executionResponse.isAsync()).isFalse();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    verify(pipelineService).refreshPipelineExecutionAsync(APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID);
  }
}
