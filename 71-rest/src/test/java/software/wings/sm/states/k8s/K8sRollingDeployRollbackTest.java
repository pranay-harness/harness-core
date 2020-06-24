package software.wings.sm.states.k8s;

import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.rule.OwnerRule.BOJANA;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.utils.WingsTestConstants.STATE_NAME;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.k8s.K8sContextElement;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Activity;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class K8sRollingDeployRollbackTest extends WingsBaseTest {
  @Mock private K8sStateHelper k8sStateHelper;
  @Mock private ActivityService activityService;
  @InjectMocks K8sRollingDeployRollback k8sRollingDeployRollback;

  private StateExecutionInstance stateExecutionInstance = aStateExecutionInstance().displayName(STATE_NAME).build();
  private ExecutionContextImpl context;

  @Before
  public void setup() {
    context = new ExecutionContextImpl(stateExecutionInstance);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testTimeoutValue() {
    K8sRollingDeployRollback state = new K8sRollingDeployRollback("k8s-rollback");
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
    K8sRollingDeployRollback k8sRollingDeployRollbackSpy = spy(k8sRollingDeployRollback);
    k8sRollingDeployRollbackSpy.handleAbortEvent(context);
    verify(k8sRollingDeployRollbackSpy, times(1)).handleAbortEvent(any(ExecutionContext.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testExecuteSkipped() {
    ExecutionResponse response = k8sRollingDeployRollback.execute(context);
    assertThat(response.getExecutionStatus()).isEqualTo(SKIPPED);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testExecute() {
    when(k8sStateHelper.createK8sActivity(
             any(ExecutionContext.class), anyString(), anyString(), any(ActivityService.class), anyList()))
        .thenReturn(new Activity());
    stateExecutionInstance.setContextElements(new LinkedList<>(Arrays.asList(K8sContextElement.builder().build())));
    ExecutionResponse response = k8sRollingDeployRollback.execute(context);
    verify(k8sStateHelper, times(1))
        .createK8sActivity(
            any(ExecutionContext.class), anyString(), anyString(), any(ActivityService.class), anyList());
    verify(k8sStateHelper, times(1)).queueK8sDelegateTask(any(ExecutionContext.class), any(K8sTaskParameters.class));
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testExecuteInvalidRequestException() {
    stateExecutionInstance.setContextElements(new LinkedList<>(Arrays.asList(K8sContextElement.builder().build())));
    k8sRollingDeployRollback.execute(context);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    K8sTaskExecutionResponse k8sTaskExecutionResponse =
        K8sTaskExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionResult.CommandExecutionStatus.SUCCESS)
            .build();
    Map<String, ResponseData> response = new HashMap<>();
    response.put("k8sTaskExecutionResponse", k8sTaskExecutionResponse);

    WorkflowStandardParams workflowStandardParams = new WorkflowStandardParams();
    stateExecutionInstance.setContextElements(new LinkedList<>(Arrays.asList(workflowStandardParams)));

    Map<String, StateExecutionData> stateExecutionMap = new HashMap<>();
    stateExecutionMap.put(STATE_NAME, new K8sStateExecutionData());
    stateExecutionInstance.setStateExecutionMap(stateExecutionMap);

    ExecutionResponse executionResponse = k8sRollingDeployRollback.handleAsyncResponse(context, response);

    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    verify(activityService, times(1)).updateStatus(anyString(), anyString(), any(ExecutionStatus.class));
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseInvalidRequestException() {
    K8sTaskExecutionResponse k8sTaskExecutionResponse =
        K8sTaskExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionResult.CommandExecutionStatus.SUCCESS)
            .build();
    Map<String, ResponseData> response = new HashMap<>();
    response.put("k8sTaskExecutionResponse", k8sTaskExecutionResponse);
    WorkflowStandardParams workflowStandardParams = new WorkflowStandardParams();
    stateExecutionInstance.setContextElements(new LinkedList<>(Arrays.asList(workflowStandardParams)));
    k8sRollingDeployRollback.handleAsyncResponse(context, response);
  }
}