package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.delegate.beans.TaskData.asyncTaskData;
import static io.harness.rule.OwnerRule.AGORODETKI;
import static io.harness.rule.OwnerRule.VGLIJIN;
import static java.util.Collections.EMPTY_MAP;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.command.GcbTaskParams;
import software.wings.beans.template.TemplateUtils;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.helpers.ext.gcb.models.GcbBuildDetails;
import software.wings.helpers.ext.gcb.models.GcbBuildStatus;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.GcbState.GcbDelegateResponse;
import software.wings.sm.states.gcbconfigs.GcbOptions;
import software.wings.sm.states.gcbconfigs.GcbRemoteBuildSpec;

import java.util.List;
import java.util.Map;

public class GcbStateTest extends CategoryTest {
  private static final Activity ACTIVITY_WITH_ID = Activity.builder().uuid(ACTIVITY_ID).build();
  private static final ExecutionResponse EXECUTION_RESPONSE = ExecutionResponse.builder().build();

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private ActivityService activityService;
  @Mock private ExecutionContextImpl context;
  @Mock private DelegateService delegateService;
  @Mock private SecretManager secretManager;
  @Mock private SweepingOutputService sweepingOutputService;
  @Mock private TemplateUtils templateUtils;
  @Mock private TemplateExpressionProcessor templateExpressionProcessor;
  @Mock private SettingsService settingService;

  @InjectMocks private GcbState state = spy(new GcbState("gcb"));

  @Before
  public void setUp() throws Exception {
    doReturn(anApplication().accountId(ACCOUNT_ID).uuid(APP_ID).build()).when(context).fetchRequiredApp();
    doReturn(APP_ID).when(context).getAppId();
    doReturn(anEnvironment().uuid(ENV_ID).appId(APP_ID).build()).when(context).getEnv();

    WorkflowStandardParams workflowStandardParams = WorkflowStandardParams.Builder.aWorkflowStandardParams().build();
    workflowStandardParams.setCurrentUser(EmbeddedUser.builder().name("test").email("test@harness.io").build());
    doReturn(workflowStandardParams).when(context).getContextElement(ContextElementType.STANDARD);

    doReturn(anEnvironment().uuid(ENV_ID).appId(APP_ID).build()).when(context).fetchRequiredEnvironment();
    doReturn(ACTIVITY_WITH_ID).when(activityService).save(any(Activity.class));
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldExecute() {
    doReturn(EXECUTION_RESPONSE).when(state).executeInternal(context, ACTIVITY_ID);
    state.execute(context);
    verify(state).createActivity(context);
    verify(state).executeInternal(context, ACTIVITY_ID);
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldDelegateTask() {
    GcbOptions gcbOption = new GcbOptions();
    gcbOption.setSpecSource(GcbOptions.GcbSpecSource.INLINE);
    gcbOption.setGcpConfigId("gcpConfigId");
    state.setGcbOptions(gcbOption);
    Application application = mock(Application.class);
    doReturn(application).when(context).fetchRequiredApp();
    when(templateUtils.processTemplateVariables(context, state.getTemplateVariables())).thenReturn(EMPTY_MAP);
    when(application.getAppId()).thenReturn("appId");
    GcpConfig gcpConfig = mock(GcpConfig.class);
    doReturn(gcpConfig).when(context).getGlobalSettingValue(any(), any());
    doReturn("workflowExecutionId").when(context).getWorkflowExecutionId();

    secretManager.getEncryptionDetails(gcpConfig, "appId", "workflowExecutionId");

    doReturn(emptyList()).when(secretManager).getEncryptionDetails(gcpConfig, "appId", "workflowExecutionId");

    state.executeInternal(context, ACTIVITY_ID);
    verify(delegateService).queueTask(any(DelegateTask.class));
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldMarkActivityAsSuccessTask() {
    GcbDelegateResponse gcbDelegateResponse = gcbDelegateResponseOf(
        GcbTaskParams.builder().build(), GcbBuildDetails.builder().status(GcbBuildStatus.SUCCESS).build());
    GcbExecutionData gcbExecutionData = mock(GcbExecutionData.class);
    when(context.getStateExecutionData()).thenReturn(gcbExecutionData);
    when(context.getAppId()).thenReturn("appId");
    when(gcbExecutionData.getStatus()).thenReturn(ExecutionStatus.SUCCESS);
    when(gcbExecutionData.withDelegateResponse(gcbDelegateResponse)).thenReturn(gcbExecutionData);
    doNothing().when(state).handleSweepingOutput(eq(sweepingOutputService), eq(context), eq(gcbExecutionData));

    Map<String, ResponseData> response = ImmutableMap.of("", gcbDelegateResponse);

    state.handleAsyncResponse(context, response);
    verify(activityService)
        .updateStatus(
            gcbDelegateResponse.getParams().getActivityId(), context.getAppId(), gcbExecutionData.getStatus());
    verify(state).handleSweepingOutput(eq(sweepingOutputService), eq(context), eq(gcbExecutionData));
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldSaveCreatedActivityTask() {
    Activity mockActivity = mock(Activity.class);
    String mockUuid = "mockUuid";
    when(activityService.save(any(Activity.class))).thenReturn(mockActivity);
    when(mockActivity.getUuid()).thenReturn(mockUuid);
    String activityUuid = state.createActivity(context);
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

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfGcpSettingIsNotFound() {
    TemplateExpression gcpConfigExp = new TemplateExpression();
    when(templateExpressionProcessor.resolveTemplateExpression(context, gcpConfigExp)).thenReturn("resolvedExpression");
    when(context.getAccountId()).thenReturn(ACCOUNT_ID);
    when(context.getGlobalSettingValue(ACCOUNT_ID, "resolvedExpression")).thenReturn(null);
    when(settingService.getSettingAttributeByName(ACCOUNT_ID, "resolvedExpression")).thenReturn(null);

    assertThat(state.resolveGcpTemplateExpression(gcpConfigExp, context)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldSetGcpConfigIdAndReturnTrueIfResolvedExpressionIsId() {
    TemplateExpression gcpConfigExp = new TemplateExpression();
    state.setGcbOptions(new GcbOptions());
    when(templateExpressionProcessor.resolveTemplateExpression(context, gcpConfigExp)).thenReturn("gcpConfigId");
    when(context.getAccountId()).thenReturn(ACCOUNT_ID);
    when(context.getGlobalSettingValue(ACCOUNT_ID, "gcpConfigId")).thenReturn(new GcpConfig());

    assertThat(state.resolveGcpTemplateExpression(gcpConfigExp, context)).isTrue();
    assertThat(state.getGcbOptions().getGcpConfigId()).isEqualTo("gcpConfigId");
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldFetchSettingAndSetGcpConfigIdAndReturnTrueIfResolvedExpressionIsName() {
    TemplateExpression gcpConfigExp = new TemplateExpression();
    state.setGcbOptions(new GcbOptions());
    SettingAttribute settingAttribute = new SettingAttribute();
    settingAttribute.setUuid("gcpConfigId");
    when(templateExpressionProcessor.resolveTemplateExpression(context, gcpConfigExp)).thenReturn("gcpConfigName");
    when(context.getAccountId()).thenReturn(ACCOUNT_ID);
    when(context.getGlobalSettingValue(ACCOUNT_ID, "gcpConfigName")).thenReturn(null);
    when(settingService.getSettingAttributeByName(ACCOUNT_ID, "gcpConfigName")).thenReturn(settingAttribute);

    assertThat(state.resolveGcpTemplateExpression(gcpConfigExp, context)).isTrue();
    assertThat(state.getGcbOptions().getGcpConfigId()).isEqualTo("gcpConfigId");
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfGitSettingIsNotFound() {
    TemplateExpression gitConfigExp = new TemplateExpression();
    when(templateExpressionProcessor.resolveTemplateExpression(context, gitConfigExp)).thenReturn("resolvedExpression");
    when(context.getAccountId()).thenReturn(ACCOUNT_ID);
    when(context.getGlobalSettingValue(ACCOUNT_ID, "resolvedExpression")).thenReturn(null);
    when(settingService.getSettingAttributeByName(ACCOUNT_ID, "resolvedExpression")).thenReturn(null);

    assertThat(state.resolveGitTemplateExpression(gitConfigExp, context)).isFalse();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldSetGitConfigIdAndReturnTrueIfResolvedExpressionIsId() {
    TemplateExpression gitConfigExp = new TemplateExpression();
    GcbOptions gcbOption = new GcbOptions();
    gcbOption.setRepositorySpec(new GcbRemoteBuildSpec());
    state.setGcbOptions(gcbOption);
    when(templateExpressionProcessor.resolveTemplateExpression(context, gitConfigExp)).thenReturn("gitConfigId");
    when(context.getAccountId()).thenReturn(ACCOUNT_ID);
    when(context.getGlobalSettingValue(ACCOUNT_ID, "gitConfigId")).thenReturn(new GitConfig());

    assertThat(state.resolveGitTemplateExpression(gitConfigExp, context)).isTrue();
    assertThat(state.getGcbOptions().getRepositorySpec().getGitConfigId()).isEqualTo("gitConfigId");
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldFetchSettingAndSetGitConfigIdAndReturnTrueIfResolvedExpressionIsName() {
    TemplateExpression gitConfigExp = new TemplateExpression();
    GcbOptions gcbOption = new GcbOptions();
    gcbOption.setRepositorySpec(new GcbRemoteBuildSpec());
    state.setGcbOptions(gcbOption);
    SettingAttribute settingAttribute = new SettingAttribute();
    settingAttribute.setUuid("gitConfigId");
    when(templateExpressionProcessor.resolveTemplateExpression(context, gitConfigExp)).thenReturn("gitConfigName");
    when(context.getAccountId()).thenReturn(ACCOUNT_ID);
    when(context.getGlobalSettingValue(ACCOUNT_ID, "gitConfigName")).thenReturn(null);
    when(settingService.getSettingAttributeByName(ACCOUNT_ID, "gitConfigName")).thenReturn(settingAttribute);

    assertThat(state.resolveGitTemplateExpression(gitConfigExp, context)).isTrue();
    assertThat(state.getGcbOptions().getRepositorySpec().getGitConfigId()).isEqualTo("gitConfigId");
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldFailExecutionWhenProvidedGcpSettingDoesNotExist() {
    TemplateExpression gcpConfigExp = new TemplateExpression();
    List<TemplateExpression> templateExpressions = singletonList(gcpConfigExp);
    state.setGcbOptions(new GcbOptions());
    state.setTemplateExpressions(templateExpressions);
    when(templateExpressionProcessor.getTemplateExpression(templateExpressions, "gcpConfigId"))
        .thenReturn(gcpConfigExp);
    when(templateExpressionProcessor.resolveTemplateExpression(context, gcpConfigExp)).thenReturn("resolvedExpression");
    when(context.getAccountId()).thenReturn(ACCOUNT_ID);
    when(context.getGlobalSettingValue(ACCOUNT_ID, "resolvedExpression")).thenReturn(null);
    when(settingService.getSettingAttributeByName(ACCOUNT_ID, "resolvedExpression")).thenReturn(null);

    assertThat(state.executeInternal(context, ACTIVITY_ID).getExecutionStatus()).isEqualTo(FAILED);
    assertThat(state.executeInternal(context, ACTIVITY_ID).getErrorMessage())
        .isEqualTo("Google Cloud Provider does not exist. Please update with an appropriate cloud provider.");
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldFailExecutionWhenProvidedGitSettingDoesNotExist() {
    TemplateExpression gitConfigExp = new TemplateExpression();
    List<TemplateExpression> templateExpressions = singletonList(gitConfigExp);
    state.setGcbOptions(new GcbOptions());
    state.setTemplateExpressions(templateExpressions);
    when(templateExpressionProcessor.getTemplateExpression(templateExpressions, "gitConfigId"))
        .thenReturn(gitConfigExp);
    when(templateExpressionProcessor.resolveTemplateExpression(context, gitConfigExp)).thenReturn("resolvedExpression");
    when(context.getAccountId()).thenReturn(ACCOUNT_ID);
    when(context.getGlobalSettingValue(ACCOUNT_ID, "resolvedExpression")).thenReturn(null);
    when(settingService.getSettingAttributeByName(ACCOUNT_ID, "resolvedExpression")).thenReturn(null);

    assertThat(state.executeInternal(context, ACTIVITY_ID).getExecutionStatus()).isEqualTo(FAILED);
    assertThat(state.executeInternal(context, ACTIVITY_ID).getErrorMessage())
        .isEqualTo("Git connector does not exist. Please update with an appropriate git connector.");
  }
}
