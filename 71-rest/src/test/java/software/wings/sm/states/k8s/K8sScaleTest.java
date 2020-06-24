package software.wings.sm.states.k8s;

import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.BOJANA;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.STATE_NAME;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.expression.VariableResolverTracker;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.InstanceElementListParam;
import software.wings.api.k8s.K8sElement;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.InstanceUnitType;
import software.wings.common.VariableProcessor;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sScaleResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.util.HashMap;
import java.util.Map;

public class K8sScaleTest extends WingsBaseTest {
  @Mock private VariableProcessor variableProcessor;
  @Mock private ManagerExpressionEvaluator evaluator;
  @Mock private ActivityService activityService;
  @Mock private K8sStateHelper k8sStateHelper;

  @InjectMocks private K8sScale k8sScale;

  private static final String ERROR_MESSAGE = "errorMessage";
  private WorkflowStandardParams workflowStandardParams = aWorkflowStandardParams().withAppId(APP_ID).build();
  private StateExecutionInstance stateExecutionInstance =
      aStateExecutionInstance()
          .displayName(STATE_NAME)
          .addContextElement(workflowStandardParams)
          .addStateExecutionData(K8sStateExecutionData.builder().build())
          .build();
  private ExecutionContextImpl context;

  @Before
  public void setup() {
    context = new ExecutionContextImpl(stateExecutionInstance);
    k8sScale.setStateTimeoutInMinutes(10);
    k8sScale.setInstances("5");
    k8sScale.setInstanceUnitType(InstanceUnitType.COUNT);

    when(variableProcessor.getVariables(any(), any())).thenReturn(emptyMap());
    when(evaluator.substitute(anyString(), anyMap(), any(VariableResolverTracker.class), anyString()))
        .thenAnswer(i -> i.getArguments()[0]);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseForFailure() {
    when(k8sStateHelper.getActivityId(context)).thenReturn(ACTIVITY_ID);
    K8sTaskExecutionResponse k8sTaskExecutionResponse = K8sTaskExecutionResponse.builder()
                                                            .errorMessage("errorMessage")
                                                            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                                            .k8sTaskResponse(K8sScaleResponse.builder().build())
                                                            .build();

    ExecutionResponse executionResponse =
        k8sScale.handleAsyncResponse(context, ImmutableMap.of(ACTIVITY_ID, k8sTaskExecutionResponse));
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(executionResponse.getStateExecutionData().getErrorMsg()).isEqualTo(ERROR_MESSAGE);
    verify(activityService, times(1)).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testTimeoutValue() {
    K8sScale state = new K8sScale("k8s-scale");
    assertThat(state.getTimeoutMillis()).isNull();

    state.setStateTimeoutInMinutes(5);
    assertThat(state.getTimeoutMillis()).isEqualTo(300000);

    state.setStateTimeoutInMinutes(Integer.MAX_VALUE);
    assertThat(state.getTimeoutMillis()).isNull();
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleAbortEvent() {
    K8sScale k8sScaleSpy = spy(k8sScale);
    k8sScaleSpy.handleAbortEvent(context);
    verify(k8sScaleSpy, times(1)).handleAbortEvent(any(ExecutionContext.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testExecuteSkipSteadyStateCheck() {
    ExecutionContextImpl executionContext = mock(ExecutionContextImpl.class);
    when(executionContext.renderExpression(anyString())).thenReturn("1");
    k8sScale.setSkipSteadyStateCheck(true);
    when(k8sStateHelper.getK8sElement(any(ExecutionContext.class))).thenReturn(K8sElement.builder().build());
    when(k8sStateHelper.createK8sActivity(
             any(ExecutionContext.class), anyString(), anyString(), any(ActivityService.class), anyList()))
        .thenReturn(new Activity());
    k8sScale.execute(executionContext);
    verify(k8sStateHelper, times(1)).queueK8sDelegateTask(any(ExecutionContext.class), any(K8sTaskParameters.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testExecute() {
    ExecutionContextImpl executionContext = mock(ExecutionContextImpl.class);
    when(executionContext.renderExpression(anyString())).thenReturn("1");
    when(k8sStateHelper.getK8sElement(any(ExecutionContext.class))).thenReturn(K8sElement.builder().build());
    when(k8sStateHelper.createK8sActivity(
             any(ExecutionContext.class), anyString(), anyString(), any(ActivityService.class), anyList()))
        .thenReturn(new Activity());
    k8sScale.execute(executionContext);
    verify(k8sStateHelper, times(1)).queueK8sDelegateTask(any(ExecutionContext.class), any(K8sTaskParameters.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    K8sTaskExecutionResponse k8sTaskExecutionResponse = K8sTaskExecutionResponse.builder()
                                                            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                            .k8sTaskResponse(K8sScaleResponse.builder().build())
                                                            .build();
    Map<String, ResponseData> response = new HashMap<>();
    response.put(ACTIVITY_ID, k8sTaskExecutionResponse);
    when(k8sStateHelper.getInstanceElementListParam(anyList())).thenReturn(InstanceElementListParam.builder().build());
    k8sScale.handleAsyncResponse(context, response);
    verify(activityService, times(1)).updateStatus(anyString(), anyString(), any(ExecutionStatus.class));
  }
}
