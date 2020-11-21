package software.wings.sm.states.k8s;

import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.BOJANA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.model.IstioDestinationWeight;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.tasks.ResponseData;

import software.wings.WingsBaseTest;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Activity;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class K8sTrafficSplitStateTest extends WingsBaseTest {
  @Mock private K8sStateHelper k8sStateHelper;
  @Mock private ActivityService activityService;
  @InjectMocks K8sTrafficSplitState k8sTrafficSplitState;

  @Mock private ExecutionContextImpl context;

  @Before
  public void setup() {
    WorkflowStandardParams workflowStandardParams = new WorkflowStandardParams();
    when(context.getContextElement(any(ContextElementType.class))).thenReturn(workflowStandardParams);
    when(context.getStateExecutionData()).thenReturn(new K8sStateExecutionData());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleAbortEvent() {
    K8sTrafficSplitState k8sTrafficSplitStateSpy = spy(k8sTrafficSplitState);
    k8sTrafficSplitStateSpy.handleAbortEvent(context);
    verify(k8sTrafficSplitStateSpy, times(1)).handleAbortEvent(any(ExecutionContext.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testValidateFields() {
    k8sTrafficSplitState.setIstioDestinationWeights(Arrays.asList(IstioDestinationWeight.builder().build()));
    Map<String, String> invalidFields = k8sTrafficSplitState.validateFields();
    assertThat(invalidFields).isNotEmpty();
    assertThat(invalidFields.size()).isEqualTo(3);
    assertThat(invalidFields).containsKeys("VirtualService name", "Weight", "Destination");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testExecute() {
    k8sTrafficSplitState.setVirtualServiceName("virtualServiceName");
    k8sTrafficSplitState.setIstioDestinationWeights(
        Arrays.asList(IstioDestinationWeight.builder().destination("destination").weight("weight").build()));
    when(context.renderExpression(anyString())).thenReturn("virtualServiceName");
    when(k8sStateHelper.createK8sActivity(
             any(ExecutionContext.class), anyString(), anyString(), any(ActivityService.class), anyList()))
        .thenReturn(new Activity());
    k8sTrafficSplitState.execute(context);
    verify(k8sStateHelper, times(1)).getContainerInfrastructureMapping(any(ExecutionContext.class));
    verify(k8sStateHelper, times(1)).queueK8sDelegateTask(any(ExecutionContext.class), any(K8sTaskParameters.class));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteWingsException() {
    InvalidArgumentsException exceptionToBeThrown = new InvalidArgumentsException(Pair.of("arg", "missing"));

    doThrow(exceptionToBeThrown).when(k8sStateHelper).getContainerInfrastructureMapping(context);

    assertThatThrownBy(() -> k8sTrafficSplitState.execute(context)).isSameAs(exceptionToBeThrown);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteAnyException() {
    IllegalStateException exceptionToBeThrown = new IllegalStateException();

    doThrow(exceptionToBeThrown).when(k8sStateHelper).getContainerInfrastructureMapping(context);

    assertThatThrownBy(() -> k8sTrafficSplitState.execute(context)).isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    K8sTaskExecutionResponse k8sTaskExecutionResponse =
        K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
    Map<String, ResponseData> response = new HashMap<>();
    response.put("k8sTaskExecutionResponse", k8sTaskExecutionResponse);

    ExecutionResponse executionResponse = k8sTrafficSplitState.handleAsyncResponse(context, response);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    verify(activityService, times(1)).updateStatus(anyString(), anyString(), any(ExecutionStatus.class));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseWingsException() {
    K8sTaskExecutionResponse k8sTaskExecutionResponse =
        K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
    Map<String, ResponseData> response = new HashMap<>();
    response.put("k8sTaskExecutionResponse", k8sTaskExecutionResponse);
    InvalidArgumentsException exceptionToBeThrown = new InvalidArgumentsException(Pair.of("arg", "missing"));

    doThrow(exceptionToBeThrown)
        .when(activityService)
        .updateStatus(anyString(), anyString(), any(ExecutionStatus.class));

    assertThatThrownBy(() -> k8sTrafficSplitState.handleAsyncResponse(context, response)).isSameAs(exceptionToBeThrown);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseAnyException() {
    K8sTaskExecutionResponse k8sTaskExecutionResponse =
        K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
    Map<String, ResponseData> response = new HashMap<>();
    response.put("k8sTaskExecutionResponse", k8sTaskExecutionResponse);
    IllegalStateException exceptionToBeThrown = new IllegalStateException();

    doThrow(exceptionToBeThrown)
        .when(activityService)
        .updateStatus(anyString(), anyString(), any(ExecutionStatus.class));

    assertThatThrownBy(() -> k8sTrafficSplitState.handleAsyncResponse(context, response))
        .isInstanceOf(InvalidRequestException.class);
  }
}
