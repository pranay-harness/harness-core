package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.delegate.beans.TaskData.asyncTaskData;
import static io.harness.rule.OwnerRule.VGLIJIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.TaskType.GCB;
import static software.wings.sm.states.GcbState.GcbDelegateResponse.gcbDelegateResponseOf;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.api.GcbExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.GcpConfig;
import software.wings.beans.command.GcbTaskParams;
import software.wings.helpers.ext.gcb.models.GcbBuildDetails;
import software.wings.helpers.ext.gcb.models.GcbBuildStatus;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.GcbState.GcbDelegateResponse;

import java.util.Collections;
import java.util.Map;

public class GcbStateTest extends CategoryTest {
  private static final Activity ACTIVITY_WITH_ID = Activity.builder().uuid(ACTIVITY_ID).build();
  private static final ExecutionResponse EXECUTION_RESPONSE = ExecutionResponse.builder().build();

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private ActivityService activityService;
  @Mock private ExecutionContextImpl execution;
  @Mock private DelegateService delegateService;
  @Mock private SecretManager secretManager;

  @InjectMocks private GcbState state = spy(new GcbState("gcb"));

  @Before
  public void setUp() throws Exception {
    doReturn(anApplication().accountId(ACCOUNT_ID).uuid(APP_ID).build()).when(execution).fetchRequiredApp();
    doReturn(APP_ID).when(execution).getAppId();
    doReturn(anEnvironment().uuid(ENV_ID).appId(APP_ID).build()).when(execution).getEnv();

    WorkflowStandardParams workflowStandardParams = WorkflowStandardParams.Builder.aWorkflowStandardParams().build();
    workflowStandardParams.setCurrentUser(EmbeddedUser.builder().name("test").email("test@harness.io").build());
    doReturn(workflowStandardParams).when(execution).getContextElement(ContextElementType.STANDARD);

    doReturn(anEnvironment().uuid(ENV_ID).appId(APP_ID).build()).when(execution).fetchRequiredEnvironment();
    doReturn(ACTIVITY_WITH_ID).when(activityService).save(any(Activity.class));
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldExecute() {
    doReturn(EXECUTION_RESPONSE).when(state).executeInternal(execution, ACTIVITY_ID);
    state.execute(execution);
    verify(state).createActivity(execution);
    verify(state).executeInternal(execution, ACTIVITY_ID);
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldDelegateTask() {
    Application application = mock(Application.class);
    doReturn(application).when(execution).fetchRequiredApp();
    when(application.getAppId()).thenReturn("appId");

    GcpConfig gcpConfig = mock(GcpConfig.class);
    doReturn(gcpConfig).when(execution).getGlobalSettingValue(any(), any());
    doReturn("workflowExecutionId").when(execution).getWorkflowExecutionId();

    secretManager.getEncryptionDetails(gcpConfig, "appId", "workflowExecutionId");

    doReturn(Collections.emptyList())
        .when(secretManager)
        .getEncryptionDetails(gcpConfig, "appId", "workflowExecutionId");

    state.executeInternal(execution, ACTIVITY_ID);
    verify(delegateService).queueTask(any(DelegateTask.class));
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldMarkActivityAsSuccessTask() {
    GcbDelegateResponse gcbDelegateResponse = gcbDelegateResponseOf(
        GcbTaskParams.builder().build(), GcbBuildDetails.builder().status(GcbBuildStatus.SUCCESS).build());
    GcbExecutionData gcbExecutionData = mock(GcbExecutionData.class);
    when(execution.getStateExecutionData()).thenReturn(gcbExecutionData);
    when(execution.getAppId()).thenReturn("appId");
    when(gcbExecutionData.getStatus()).thenReturn(ExecutionStatus.SUCCESS);
    doNothing().when(state).handleSweepingOutput(any(SweepingOutputService.class), eq(execution), eq(gcbExecutionData));

    Map<String, ResponseData> response = ImmutableMap.of("", gcbDelegateResponse);

    state.handleAsyncResponse(execution, response);
    verify(activityService)
        .updateStatus(
            gcbDelegateResponse.getParams().getActivityId(), execution.getAppId(), gcbExecutionData.getStatus());
    verify(state).handleSweepingOutput(any(SweepingOutputService.class), eq(execution), eq(gcbExecutionData));
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldSaveCreatedActivityTask() {
    Activity mockActivity = mock(Activity.class);
    String mockUuid = "mockUuid";
    when(activityService.save(any(Activity.class))).thenReturn(mockActivity);
    when(mockActivity.getUuid()).thenReturn(mockUuid);
    String activityUuid = state.createActivity(execution);
    verify(activityService).save(any(Activity.class));
    assertThat(activityUuid).isEqualTo(mockUuid);
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void handleAbortEventShouldSetErrorMessage() {
    ExecutionContext executionContext = mock(ExecutionContext.class);
    GcbExecutionData gcbExecutionData = mock(GcbExecutionData.class);

    when(executionContext.getStateExecutionData()).thenReturn(gcbExecutionData);
    state.handleAbortEvent(executionContext);
    verify(gcbExecutionData).setErrorMsg(anyString());
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void handleAsyncResponseShouldReturnWIthFailStatus() {
    ExecutionContext executionContext = mock(ExecutionContext.class);
    ErrorNotifyResponseData error = ErrorNotifyResponseData.builder().errorMessage("error").build();

    ExecutionResponse expected = ExecutionResponse.builder().executionStatus(FAILED).errorMessage("error").build();

    ExecutionResponse actual = state.handleAsyncResponse(executionContext, ImmutableMap.of("activityId", error));

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldStartPollTask() {
    ExecutionContext executionContext = mock(ExecutionContext.class);
    GcbDelegateResponse delegateResponse = gcbDelegateResponseOf(
        GcbTaskParams.builder().build(), GcbBuildDetails.builder().status(GcbBuildStatus.WORKING).build());
    ExecutionResponse executionResponse = ExecutionResponse.builder().build();

    doReturn(executionResponse).when(state).startPollTask(executionContext, delegateResponse);

    ExecutionResponse actual =
        state.handleAsyncResponse(executionContext, ImmutableMap.of("activityId", delegateResponse));
    verify(state).startPollTask(executionContext, delegateResponse);
    assertThat(actual).isEqualTo(executionResponse);
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldQueuePollDelegateTask() {
    ExecutionContext executionContext = mock(ExecutionContext.class);
    GcbTaskParams gcbTaskParams = GcbTaskParams.builder().build();
    GcbDelegateResponse delegateResponse =
        gcbDelegateResponseOf(gcbTaskParams, GcbBuildDetails.builder().status(GcbBuildStatus.WORKING).build());
    GcbExecutionData gcbExecutionData = mock(GcbExecutionData.class);

    doReturn(gcbExecutionData).when(executionContext).getStateExecutionData();
    when(executionContext.fetchRequiredApp()).thenReturn(mock(Application.class));
    when(executionContext.fetchRequiredApp().getAccountId()).thenReturn("accountId");
    when(executionContext.fetchRequiredApp().getAppId()).thenReturn("appId");
    when(executionContext.fetchInfraMappingId()).thenReturn("infrastructureId");

    ExecutionResponse actual = state.startPollTask(executionContext, delegateResponse);
    ArgumentCaptor<DelegateTask> delegateTaskCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(delegateTaskCaptor.capture());

    assertThat(actual.isAsync()).isTrue();
    assertThat(actual.getStateExecutionData()).isEqualTo(gcbExecutionData);
    assertThat(actual.getExecutionStatus()).isEqualTo(RUNNING);

    DelegateTask delegateTask = delegateTaskCaptor.getValue();

    assertThat(delegateTask.getAccountId()).isEqualTo("accountId");
    assertThat(delegateTask.getAppId()).isEqualTo("appId");
    assertThat(delegateTask.getInfrastructureMappingId()).isEqualTo("infrastructureId");
    assertThat(delegateTask.getData()).isEqualTo(asyncTaskData(GCB.name(), gcbTaskParams));
  }
}
