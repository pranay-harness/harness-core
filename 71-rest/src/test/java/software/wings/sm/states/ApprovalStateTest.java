package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.ABORTED;
import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.REJECTED;
import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_EXPIRED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_NEEDED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_STATE_CHANGE_NOTIFICATION;
import static software.wings.security.SecretManager.JWT_CATEGORY.EXTERNAL_SERVICE_SECRET;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.BUILD_JOB_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.NOTIFICATION_GROUP_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageResponse;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.exception.UnexpectedException;
import io.harness.rule.Owner;
import io.harness.serializer.KryoUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.api.AmiServiceDeployElement;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.ApprovalStateExecutionData.ApprovalStateExecutionDataKeys;
import software.wings.api.ServiceNowExecutionData;
import software.wings.api.WorkflowElement;
import software.wings.api.jira.JiraExecutionData;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EnvSummary;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.NameValuePair;
import software.wings.beans.NameValuePair.NameValuePairKeys;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.User;
import software.wings.beans.User.Builder;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.beans.approval.ApprovalStateParams;
import software.wings.beans.approval.JiraApprovalParams;
import software.wings.beans.approval.ServiceNowApprovalParams;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.common.NotificationMessageResolver;
import software.wings.security.SecretManager;
import software.wings.service.impl.JiraHelperService;
import software.wings.service.impl.workflow.WorkflowNotificationHelper;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.ApprovalPolingService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.servicenow.ServiceNowService;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionContext;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.ApprovalState.ApprovalStateKeys;
import software.wings.sm.states.ApprovalState.ApprovalStateType;
import software.wings.sm.states.EnvState.EnvStateKeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 11/3/16.
 */
public class ApprovalStateTest extends WingsBaseTest {
  private static final WorkflowStandardParams WORKFLOW_STANDARD_PARAMS =
      aWorkflowStandardParams().withArtifactIds(asList(ARTIFACT_ID)).build();
  private static final String USER_NAME_1_KEY = "UserName1";
  private Integer DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS = 7 * 24 * 60 * 60 * 1000; // 7 days
  @Mock private ExecutionContextImpl context;
  @Mock private AlertService alertService;
  @Mock private NotificationService notificationService;
  @Mock private NotificationSetupService notificationSetupService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private NotificationMessageResolver notificationMessageResolver;
  @Mock private WorkflowNotificationHelper workflowNotificationHelper;
  @Mock private PipelineService pipelineService;
  @Mock private WorkflowExecution workflowExecution;
  @Mock private SecretManager secretManager;
  @Mock private SweepingOutputService sweepingOutputService;
  @Mock private JiraHelperService jiraHelperService;
  @Mock private ServiceNowService serviceNowService;
  @Mock private ApprovalPolingService approvalPolingService;

  @InjectMocks private ApprovalState approvalState = new ApprovalState("ApprovalState");

  @Before
  public void setUp() throws Exception {
    when(context.getApp()).thenReturn(anApplication().accountId(ACCOUNT_ID).uuid(APP_ID).build());
    when(context.getAppId()).thenReturn(APP_ID);
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(WORKFLOW_STANDARD_PARAMS);
    when(context.getWorkflowExecutionId()).thenReturn(PIPELINE_WORKFLOW_EXECUTION_ID);
    when(context.getWorkflowExecutionName()).thenReturn(BUILD_JOB_NAME);
    when(context.getWorkflowId()).thenReturn(WORKFLOW_ID);
    when(secretManager.getJWTSecret(EXTERNAL_SERVICE_SECRET)).thenReturn("secret");
    writeField(approvalState, "secretManager", secretManager, true);

    PipelineStageElement pipelineStageElement = PipelineStageElement.builder().name("ApprovalState").build();
    PipelineStage pipelineStage = PipelineStage.builder()
                                      .pipelineStageElements(Collections.singletonList(pipelineStageElement))
                                      .name("Approval Needed")
                                      .build();
    Pipeline pipeline = Pipeline.builder()
                            .name("pipeline")
                            .appId(APP_ID)
                            .uuid(WORKFLOW_ID)
                            .pipelineStages(Collections.singletonList(pipelineStage))
                            .build();
    when(pipelineService.readPipeline(APP_ID, WORKFLOW_ID, true)).thenReturn(pipeline);

    Map<String, String> metadata = new HashMap<>();
    metadata.put(ArtifactMetadataKeys.buildNo, "2");
    Artifact artifact = anArtifact().withMetadata(metadata).withArtifactSourceName("Artifact").build();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(Collections.singletonList(artifact));

    EnvSummary envsummary = EnvSummary.builder().name("Environment").build();

    List<ElementExecutionSummary> elementExecutionSummaries = new ArrayList<>();
    AmiServiceDeployElement amiServiceDeployElement = AmiServiceDeployElement.builder().build();

    elementExecutionSummaries.add(0, anElementExecutionSummary().withContextElement(amiServiceDeployElement).build());

    WorkflowExecution workflowExecution = WorkflowExecution.builder()
                                              .executionArgs(executionArgs)
                                              .environments(Collections.singletonList(envsummary))
                                              .serviceExecutionSummaries(elementExecutionSummaries)
                                              .build();

    when(workflowExecutionService.getExecutionDetails(context.getAppId(), context.getWorkflowExecutionId(), true))
        .thenReturn(workflowExecution);

    String approvalId = generateUuid();
    Map<String, String> claims = new HashMap<>();
    claims.put("approvalId", approvalId);
    String secret = "secret";
    when(secretManager.generateJWTTokenWithCustomTimeOut(claims, secret, 60 * 60 * 1000 * 168)).thenReturn("token");

    when(workflowExecutionService.getWorkflowExecution(APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID))
        .thenReturn(WorkflowExecution.builder()
                        .status(ExecutionStatus.NEW)
                        .appId(APP_ID)
                        .triggeredBy(EmbeddedUser.builder().name(USER_NAME).uuid(USER_NAME).build())
                        .createdAt(70L)
                        .build());

    when(notificationSetupService.listDefaultNotificationGroup(any()))
        .thenReturn(asList(aNotificationGroup()
                               .withName(USER_NAME)
                               .withUuid(NOTIFICATION_GROUP_ID)
                               .withAccountId(ACCOUNT_ID)
                               .build()));

    Map<String, String> placeholders = new HashMap<>();
    when(notificationMessageResolver.getPlaceholderValues(
             any(), any(), any(Long.class), any(Long.class), any(), any(), any(), any(), any()))
        .thenReturn(placeholders);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldExecutePipeline() {
    PageResponse pageResponse = new PageResponse();
    pageResponse.setResponse(asList(User.Builder.anUser().build()));
    when(context.getStateExecutionInstance())
        .thenReturn(aStateExecutionInstance().executionType(WorkflowType.PIPELINE).build());

    when(context.getWorkflowType()).thenReturn(WorkflowType.PIPELINE);

    ExecutionResponse executionResponse = approvalState.execute(context);
    verify(alertService)
        .openAlert(eq(ACCOUNT_ID), eq(APP_ID), eq(AlertType.ApprovalNeeded), any(ApprovalNeededAlert.class));
    assertThat(executionResponse.isAsync()).isTrue();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(PAUSED);

    Mockito.verify(workflowNotificationHelper, Mockito.times(1))
        .sendApprovalNotification(Mockito.eq(ACCOUNT_ID), Mockito.eq(APPROVAL_NEEDED_NOTIFICATION), Mockito.anyMap(),
            Mockito.any(), Mockito.eq(ApprovalStateType.USER_GROUP));
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(PAUSED);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldExecuteWorkflow() {
    PageResponse pageResponse = new PageResponse();
    pageResponse.setResponse(asList(User.Builder.anUser().build()));
    when(context.getStateExecutionInstance())
        .thenReturn(aStateExecutionInstance().executionType(WorkflowType.ORCHESTRATION).build());

    when(context.getWorkflowType()).thenReturn(WorkflowType.ORCHESTRATION);
    ExecutionResponse executionResponse = approvalState.execute(context);
    verify(alertService)
        .openAlert(eq(ACCOUNT_ID), eq(APP_ID), eq(AlertType.ApprovalNeeded), any(ApprovalNeededAlert.class));
    assertThat(executionResponse.isAsync()).isTrue();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(PAUSED);

    Mockito.verify(workflowNotificationHelper, Mockito.times(1))
        .sendApprovalNotification(Mockito.eq(ACCOUNT_ID), Mockito.eq(APPROVAL_NEEDED_NOTIFICATION), Mockito.anyMap(),
            Mockito.any(), Mockito.eq(ApprovalStateType.USER_GROUP));
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(PAUSED);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldSkipDisabledStep() {
    PageResponse pageResponse = new PageResponse();
    pageResponse.setResponse(Collections.singletonList(Builder.anUser().build()));
    approvalState.setDisableAssertion("true");
    when(context.evaluateExpression(eq("true"), any())).thenReturn(true);
    ExecutionResponse executionResponse = approvalState.execute(context);
    verify(alertService, times(0))
        .openAlert(eq(ACCOUNT_ID), eq(APP_ID), eq(AlertType.ApprovalNeeded), any(ApprovalNeededAlert.class));
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SKIPPED);
    assertThat(executionResponse.getErrorMessage()).isNotEmpty();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetTimeout() {
    Integer timeoutMillis = approvalState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo(DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetSetTimeout() {
    approvalState.setTimeoutMillis((int) (0.6 * TimeUnit.HOURS.toMillis(1)));
    Integer timeoutMillis = approvalState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo((int) (0.6 * TimeUnit.HOURS.toMillis(1)));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldHandlePipelineAbortWithTimeoutMsg() {
    approvalState.setTimeoutMillis((int) (0.6 * TimeUnit.HOURS.toMillis(1)));

    when(context.getStateExecutionInstance())
        .thenReturn(aStateExecutionInstance().executionType(WorkflowType.PIPELINE).uuid(generateUuid()).build());

    when(notificationMessageResolver.getApprovalType(any())).thenReturn(WorkflowType.PIPELINE.name());

    ApprovalStateExecutionData approvalStateExecutionData =
        ApprovalStateExecutionData.builder().approvalId("APPROVAL_ID").build();
    approvalStateExecutionData.setStartTs((long) (System.currentTimeMillis() - (0.6 * TimeUnit.HOURS.toMillis(1))));
    when(context.getStateExecutionData()).thenReturn(approvalStateExecutionData);

    approvalState.handleAbortEvent(context);
    assertThat(context.getStateExecutionData()).isNotNull();
    assertThat(context.getStateExecutionData().getErrorMsg()).contains("Pipeline was not approved within 36m");

    Mockito.verify(workflowNotificationHelper, Mockito.times(1))
        .sendApprovalNotification(Mockito.eq(ACCOUNT_ID), Mockito.eq(APPROVAL_EXPIRED_NOTIFICATION), Mockito.anyMap(),
            Mockito.any(), Mockito.eq(ApprovalStateType.USER_GROUP));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldHandleWorkflowAbortWithTimeoutMsg() {
    approvalState.setTimeoutMillis((int) (0.6 * TimeUnit.HOURS.toMillis(1)));

    when(context.getStateExecutionInstance())
        .thenReturn(aStateExecutionInstance().executionType(WorkflowType.ORCHESTRATION).uuid(generateUuid()).build());

    when(notificationMessageResolver.getApprovalType(any())).thenReturn(WorkflowType.ORCHESTRATION.name());

    ApprovalStateExecutionData approvalStateExecutionData =
        ApprovalStateExecutionData.builder().approvalId("APPROVAL_ID").build();
    approvalStateExecutionData.setStartTs((long) (System.currentTimeMillis() - (0.6 * TimeUnit.HOURS.toMillis(1))));
    when(context.getStateExecutionData()).thenReturn(approvalStateExecutionData);
    approvalState.handleAbortEvent(context);
    assertThat(context.getStateExecutionData()).isNotNull();
    assertThat(context.getStateExecutionData().getErrorMsg()).contains("Workflow was not approved within 36m");

    Mockito.verify(workflowNotificationHelper, Mockito.times(1))
        .sendApprovalNotification(Mockito.eq(ACCOUNT_ID), Mockito.eq(APPROVAL_EXPIRED_NOTIFICATION), Mockito.anyMap(),
            Mockito.any(), Mockito.eq(ApprovalStateType.USER_GROUP));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldHandlePipelineAbortWithAbortMsg() {
    approvalState.setTimeoutMillis((int) (0.6 * TimeUnit.HOURS.toMillis(1)));

    when(context.getStateExecutionInstance())
        .thenReturn(aStateExecutionInstance().executionType(WorkflowType.PIPELINE).uuid(generateUuid()).build());

    when(notificationMessageResolver.getApprovalType(any())).thenReturn(WorkflowType.PIPELINE.name());

    ApprovalStateExecutionData approvalStateExecutionData =
        ApprovalStateExecutionData.builder().approvalId("APPROVAL_ID").build();
    approvalStateExecutionData.setStartTs(System.currentTimeMillis());

    when(context.getStateExecutionData()).thenReturn(approvalStateExecutionData);
    approvalState.handleAbortEvent(context);
    assertThat(context.getStateExecutionData()).isNotNull();
    assertThat(context.getStateExecutionData().getErrorMsg()).contains("Pipeline was aborted");
    Mockito.verify(workflowNotificationHelper, Mockito.times(1))
        .sendApprovalNotification(Mockito.eq(ACCOUNT_ID), Mockito.eq(APPROVAL_STATE_CHANGE_NOTIFICATION),
            Mockito.anyMap(), Mockito.any(), Mockito.eq(ApprovalStateType.USER_GROUP));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldHandleWorkflowAbortWithAbortMsg() {
    approvalState.setTimeoutMillis((int) (0.6 * TimeUnit.HOURS.toMillis(1)));

    when(context.getStateExecutionInstance())
        .thenReturn(aStateExecutionInstance().executionType(WorkflowType.ORCHESTRATION).uuid(generateUuid()).build());

    when(notificationMessageResolver.getApprovalType(any())).thenReturn(WorkflowType.ORCHESTRATION.name());

    ApprovalStateExecutionData approvalStateExecutionData =
        ApprovalStateExecutionData.builder().approvalId("APPROVAL_ID").build();
    approvalStateExecutionData.setStartTs(System.currentTimeMillis());

    when(context.getStateExecutionData()).thenReturn(approvalStateExecutionData);
    approvalState.handleAbortEvent(context);
    assertThat(context.getStateExecutionData()).isNotNull();
    assertThat(context.getStateExecutionData().getErrorMsg()).contains("Workflow was aborted");
    Mockito.verify(workflowNotificationHelper, Mockito.times(1))
        .sendApprovalNotification(Mockito.eq(ACCOUNT_ID), Mockito.eq(APPROVAL_STATE_CHANGE_NOTIFICATION),
            Mockito.anyMap(), Mockito.any(), Mockito.eq(ApprovalStateType.USER_GROUP));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetPlaceholderValues() {
    ApprovalStateExecutionData approvalStateExecutionData = ApprovalStateExecutionData.builder().build();
    approvalStateExecutionData.setStartTs(100L);

    when(context.getStateExecutionData()).thenReturn(approvalStateExecutionData);

    approvalState.getPlaceholderValues(context, USER_NAME_1_KEY, ABORTED);
    verify(notificationMessageResolver)
        .getPlaceholderValues(any(), eq(USER_NAME_1_KEY), eq(100L), any(Long.class), eq(""), eq("aborted"), any(),
            eq(ABORTED), eq(AlertType.ApprovalNeeded));

    approvalState.getPlaceholderValues(context, USER_NAME_1_KEY, PAUSED);
    verify(notificationMessageResolver)
        .getPlaceholderValues(any(), eq(USER_NAME), eq(70L), any(Long.class), eq(""), eq("paused"), any(), eq(PAUSED),
            eq(AlertType.ApprovalNeeded));

    approvalState.getPlaceholderValues(context, USER_NAME_1_KEY, SUCCESS);
    verify(notificationMessageResolver)
        .getPlaceholderValues(any(), eq(USER_NAME_1_KEY), any(Long.class), any(Long.class), eq(""), eq("approved"),
            any(), eq(SUCCESS), eq(AlertType.ApprovalNeeded));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testApprovalNeededAlertParamsForWorkflow() {
    when(context.getStateExecutionInstance())
        .thenReturn(aStateExecutionInstance().executionType(WorkflowType.ORCHESTRATION).build());
    when(context.getEnv()).thenReturn(anEnvironment().appId(APP_ID).uuid(ENV_ID).build());
    when(context.getWorkflowType()).thenReturn(WorkflowType.PIPELINE);
    approvalState.execute(context);
    ArgumentCaptor<ApprovalNeededAlert> argumentCaptor = ArgumentCaptor.forClass(ApprovalNeededAlert.class);
    verify(alertService).openAlert(eq(ACCOUNT_ID), eq(APP_ID), eq(AlertType.ApprovalNeeded), argumentCaptor.capture());

    ApprovalNeededAlert approvalNeededAlert = argumentCaptor.getValue();
    assertThat(approvalNeededAlert.getEnvId()).isEqualTo(ENV_ID);
    assertThat(approvalNeededAlert.getWorkflowType()).isEqualTo(WorkflowType.ORCHESTRATION);
    assertThat(approvalNeededAlert.getWorkflowExecutionId()).isEqualTo(PIPELINE_WORKFLOW_EXECUTION_ID);
    assertThat(approvalNeededAlert.getPipelineExecutionId()).isEqualTo(null);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testApprovalNeededAlertParamsForPipelineWithApproval() {
    when(context.getStateExecutionInstance())
        .thenReturn(aStateExecutionInstance().executionType(WorkflowType.PIPELINE).build());
    when(context.getWorkflowType()).thenReturn(WorkflowType.PIPELINE);
    approvalState.execute(context);
    ArgumentCaptor<ApprovalNeededAlert> argumentCaptor = ArgumentCaptor.forClass(ApprovalNeededAlert.class);
    verify(alertService).openAlert(eq(ACCOUNT_ID), eq(APP_ID), eq(AlertType.ApprovalNeeded), argumentCaptor.capture());

    ApprovalNeededAlert approvalNeededAlert = argumentCaptor.getValue();
    assertThat(approvalNeededAlert.getEnvId()).isEqualTo(null);
    assertThat(approvalNeededAlert.getWorkflowType()).isEqualTo(WorkflowType.PIPELINE);
    assertThat(approvalNeededAlert.getWorkflowExecutionId()).isEqualTo(null);
    assertThat(approvalNeededAlert.getPipelineExecutionId()).isEqualTo(PIPELINE_WORKFLOW_EXECUTION_ID);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testApprovalNeededAlertParamsForPipelineWithWorkflowApproval() {
    when(context.getStateExecutionInstance())
        .thenReturn(aStateExecutionInstance().executionType(WorkflowType.ORCHESTRATION).build());
    when(context.getEnv()).thenReturn(anEnvironment().appId(APP_ID).uuid(ENV_ID).build());
    when(context.getWorkflowType()).thenReturn(WorkflowType.PIPELINE);
    when(context.getContextElement(ContextElementType.STANDARD))
        .thenReturn(
            aWorkflowStandardParams()
                .withWorkflowElement(WorkflowElement.builder().pipelineDeploymentUuid(PIPELINE_EXECUTION_ID).build())
                .build());

    approvalState.execute(context);
    ArgumentCaptor<ApprovalNeededAlert> argumentCaptor = ArgumentCaptor.forClass(ApprovalNeededAlert.class);
    verify(alertService).openAlert(eq(ACCOUNT_ID), eq(APP_ID), eq(AlertType.ApprovalNeeded), argumentCaptor.capture());

    ApprovalNeededAlert approvalNeededAlert = argumentCaptor.getValue();
    assertThat(approvalNeededAlert.getEnvId()).isEqualTo(ENV_ID);
    assertThat(approvalNeededAlert.getWorkflowType()).isEqualTo(WorkflowType.ORCHESTRATION);
    assertThat(approvalNeededAlert.getWorkflowExecutionId()).isEqualTo(PIPELINE_WORKFLOW_EXECUTION_ID);
    assertThat(approvalNeededAlert.getPipelineExecutionId()).isEqualTo(PIPELINE_EXECUTION_ID);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testParseProperties() {
    final Map<String, Object> properties = new HashMap<>();
    Map<String, String> var_1 = new HashMap<>();
    Map<String, String> var_2 = new HashMap<>();
    Map<String, String> var_3 = new HashMap<>();
    var_1.put(NameValuePairKeys.name, "var_1");
    var_2.put(NameValuePairKeys.name, "var_2");
    var_2.put(NameValuePairKeys.value, "val_2");
    var_3.put(NameValuePairKeys.name, "var_1"); // duplicate name
    var_3.put(NameValuePairKeys.value, "val_3");
    properties.put(ApprovalStateKeys.variables, new ArrayList<>(asList(var_1, var_2, var_3)));
    properties.put(EnvStateKeys.disable, true);
    approvalState.parseProperties(properties);
    assertThat(properties.get(EnvStateKeys.disableAssertion)).isEqualTo("true");
    assertThat(approvalState.getVariables()).isNotNull().hasSize(2);
    assertThat(approvalState.getVariables().stream().filter(val -> val.getValue() == null).collect(Collectors.toList()))
        .isEmpty();

    assertThat(approvalState.getVariables().stream().map(NameValuePair::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("var_1", "var_2");
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void testSetPipelineVariables() {
    final ExecutionContext executionContextMock = mock(ExecutionContext.class);
    final WorkflowStandardParams workflowStandardParams = new WorkflowStandardParams();
    workflowStandardParams.setWorkflowVariables(ImmutableMap.of("key", "value"));
    doReturn(workflowStandardParams).when(executionContextMock).getContextElement(ContextElementType.STANDARD);
    approvalState.setPipelineVariables(executionContextMock);
    assertThat(workflowStandardParams.getWorkflowElement()).isNotNull();

    approvalState.setPipelineVariables(executionContextMock);
    assertThat(workflowStandardParams.getWorkflowElement().getVariables().get("key")).isEqualTo("value");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testFillSweepingOutput() {
    when(context.renderExpression(anyString(), any(StateExecutionContext.class)))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, String.class));

    when(context.prepareSweepingOutputBuilder(any())).thenReturn(SweepingOutputInstance.builder());
    approvalState.setSweepingOutputName("test");
    approvalState.setVariables(asList(NameValuePair.builder().name("test").value("test").valueType("TEXT").build()));
    verifyUserGroupSweepingOutput();
    verifyJiraSweepingOutput();
    verifySnowSweepingOutput();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testExecuteJiraApprovalFailure() {
    when(context.renderExpression(anyString()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, String.class));

    ApprovalStateParams approvalStateParams = new ApprovalStateParams();
    JiraApprovalParams jiraApprovalParams = new JiraApprovalParams();
    jiraApprovalParams.setIssueId("${a}");
    approvalStateParams.setJiraApprovalParams(jiraApprovalParams);
    approvalState.setApprovalStateParams(approvalStateParams);
    ApprovalStateExecutionData executionData =
        ApprovalStateExecutionData.builder().approvalStateType(ApprovalStateType.JIRA).build();

    ExecutionResponse executionResponse = approvalState.executeJiraApproval(context, executionData, "id");

    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);

    when(jiraHelperService.fetchIssue(any(), any(), any(), any(), any()))
        .thenReturn(JiraExecutionData.builder().executionStatus(FAILED).build());

    approvalStateParams.getJiraApprovalParams().setIssueId("issueId");
    executionResponse = approvalState.executeJiraApproval(context, executionData, "id");
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testExecuteJiraApprovalIfAlreadyApproved() {
    String approvalValue = "DONE";
    when(context.renderExpression(anyString()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, String.class));
    when(jiraHelperService.fetchIssue(any(), any(), any(), any(), any()))
        .thenReturn(JiraExecutionData.builder().currentStatus(approvalValue).build());

    ApprovalStateParams approvalStateParams = new ApprovalStateParams();
    JiraApprovalParams jiraApprovalParams = new JiraApprovalParams();
    jiraApprovalParams.setIssueId("issueId");
    jiraApprovalParams.setApprovalValue(approvalValue);
    approvalStateParams.setJiraApprovalParams(jiraApprovalParams);
    approvalState.setApprovalStateParams(approvalStateParams);
    ApprovalStateExecutionData executionData =
        ApprovalStateExecutionData.builder().approvalStateType(ApprovalStateType.JIRA).build();

    ExecutionResponse executionResponse = approvalState.executeJiraApproval(context, executionData, "id");
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testExecuteJiraApprovalIfAlreadyRejected() {
    String rejectionValue = "REJECTED";
    when(context.renderExpression(anyString()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, String.class));
    when(jiraHelperService.fetchIssue(any(), any(), any(), any(), any()))
        .thenReturn(JiraExecutionData.builder().currentStatus(rejectionValue).build());

    ApprovalStateParams approvalStateParams = new ApprovalStateParams();
    JiraApprovalParams jiraApprovalParams = new JiraApprovalParams();
    jiraApprovalParams.setIssueId("issueId");
    jiraApprovalParams.setRejectionValue(rejectionValue);
    approvalStateParams.setJiraApprovalParams(jiraApprovalParams);
    approvalState.setApprovalStateParams(approvalStateParams);
    ApprovalStateExecutionData executionData =
        ApprovalStateExecutionData.builder().approvalStateType(ApprovalStateType.JIRA).build();

    ExecutionResponse executionResponse = approvalState.executeJiraApproval(context, executionData, "id");
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(REJECTED);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testExecuteJiraApprovalWithPollingService() {
    when(context.renderExpression(anyString()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, String.class));
    when(jiraHelperService.fetchIssue(any(), any(), any(), any(), any()))
        .thenReturn(JiraExecutionData.builder().currentStatus("TODO").build());

    ApprovalStateParams approvalStateParams = new ApprovalStateParams();
    JiraApprovalParams jiraApprovalParams = new JiraApprovalParams();
    jiraApprovalParams.setIssueId("issueId");
    jiraApprovalParams.setApprovalValue("DONE");
    approvalStateParams.setJiraApprovalParams(jiraApprovalParams);
    approvalState.setApprovalStateParams(approvalStateParams);
    ApprovalStateExecutionData executionData =
        ApprovalStateExecutionData.builder().approvalStateType(ApprovalStateType.JIRA).build();

    ExecutionResponse executionResponse = approvalState.executeJiraApproval(context, executionData, "id");
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(PAUSED);

    when(approvalPolingService.save(any())).thenThrow(new UnexpectedException());
    executionResponse = approvalState.executeJiraApproval(context, executionData, "id");
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(FAILED);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testExecuteSnowApprovalFailure() {
    when(context.renderExpression(anyString()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, String.class));

    ApprovalStateParams approvalStateParams = new ApprovalStateParams();
    ServiceNowApprovalParams serviceNowApprovalParams = new ServiceNowApprovalParams();
    serviceNowApprovalParams.setIssueNumber("${a}");
    approvalStateParams.setServiceNowApprovalParams(serviceNowApprovalParams);
    approvalState.setApprovalStateParams(approvalStateParams);
    ApprovalStateExecutionData executionData =
        ApprovalStateExecutionData.builder().approvalStateType(ApprovalStateType.SERVICENOW).build();

    ExecutionResponse executionResponse = approvalState.executeServiceNowApproval(context, executionData, "id");

    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testExecuteSnowApprovalIfAlreadyApproved() {
    String approvalValue = "DONE";
    when(context.renderExpression(anyString()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, String.class));
    when(serviceNowService.getIssueUrl(anyString(), anyString(), any(), anyString(), anyString()))
        .thenReturn(ServiceNowExecutionData.builder().currentState(approvalValue).build());

    ApprovalStateParams approvalStateParams = new ApprovalStateParams();
    ServiceNowApprovalParams serviceNowApprovalParams = new ServiceNowApprovalParams();
    serviceNowApprovalParams.setIssueNumber("issueNumber");
    serviceNowApprovalParams.setApprovalValue(approvalValue);
    approvalStateParams.setServiceNowApprovalParams(serviceNowApprovalParams);
    approvalState.setApprovalStateParams(approvalStateParams);
    ApprovalStateExecutionData executionData =
        ApprovalStateExecutionData.builder().approvalStateType(ApprovalStateType.SERVICENOW).build();

    ExecutionResponse executionResponse = approvalState.executeServiceNowApproval(context, executionData, "id");
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testExecuteSnowApprovalIfAlreadyRejected() {
    String rejectionValue = "REJECTED";
    when(context.renderExpression(anyString()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, String.class));
    when(serviceNowService.getIssueUrl(anyString(), anyString(), any(), anyString(), anyString()))
        .thenReturn(ServiceNowExecutionData.builder().currentState(rejectionValue).build());

    ApprovalStateParams approvalStateParams = new ApprovalStateParams();
    ServiceNowApprovalParams serviceNowApprovalParams = new ServiceNowApprovalParams();
    serviceNowApprovalParams.setIssueNumber("issueNumber");
    serviceNowApprovalParams.setRejectionValue(rejectionValue);
    approvalStateParams.setServiceNowApprovalParams(serviceNowApprovalParams);
    approvalState.setApprovalStateParams(approvalStateParams);
    ApprovalStateExecutionData executionData =
        ApprovalStateExecutionData.builder().approvalStateType(ApprovalStateType.SERVICENOW).build();

    ExecutionResponse executionResponse = approvalState.executeServiceNowApproval(context, executionData, "id");
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(REJECTED);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testExecuteSnowApprovalWithPollingService() {
    when(context.renderExpression(anyString()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, String.class));
    when(serviceNowService.getIssueUrl(anyString(), anyString(), any(), anyString(), anyString()))
        .thenReturn(ServiceNowExecutionData.builder().currentState("TODO").build());

    ApprovalStateParams approvalStateParams = new ApprovalStateParams();
    ServiceNowApprovalParams serviceNowApprovalParams = new ServiceNowApprovalParams();
    serviceNowApprovalParams.setIssueNumber("issueNumber");
    serviceNowApprovalParams.setApprovalValue("DONE");
    approvalStateParams.setServiceNowApprovalParams(serviceNowApprovalParams);
    approvalState.setApprovalStateParams(approvalStateParams);
    ApprovalStateExecutionData executionData =
        ApprovalStateExecutionData.builder().approvalStateType(ApprovalStateType.SERVICENOW).build();

    ExecutionResponse executionResponse = approvalState.executeServiceNowApproval(context, executionData, "id");
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(PAUSED);

    when(approvalPolingService.save(any())).thenThrow(new UnexpectedException());
    executionResponse = approvalState.executeServiceNowApproval(context, executionData, "id");
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(FAILED);
  }

  private void verifyJiraSweepingOutput() {
    ArgumentCaptor<SweepingOutputInstance> captor = ArgumentCaptor.forClass(SweepingOutputInstance.class);
    ApprovalStateExecutionData executionData = ApprovalStateExecutionData.builder()
                                                   .approvalStateType(ApprovalStateType.JIRA)
                                                   .approvedBy(EmbeddedUser.builder().name(USER_NAME).build())
                                                   .build();
    final List<String> keys = GetSweepingOutputKeys(captor, executionData);
    assertJiraKeysInSweepingOutput(keys);
  }

  private void verifyUserGroupSweepingOutput() {
    ArgumentCaptor<SweepingOutputInstance> captor = ArgumentCaptor.forClass(SweepingOutputInstance.class);
    ApprovalStateExecutionData executionData = ApprovalStateExecutionData.builder()
                                                   .approvalStateType(ApprovalStateType.USER_GROUP)
                                                   .approvedBy(EmbeddedUser.builder().name(USER_NAME).build())
                                                   .build();
    final List<String> keys = GetSweepingOutputKeys(captor, executionData);
    assertUserGroupKeysInSweepingOutput(keys);
  }

  private void verifySnowSweepingOutput() {
    ArgumentCaptor<SweepingOutputInstance> captor = ArgumentCaptor.forClass(SweepingOutputInstance.class);
    ApprovalStateExecutionData executionData = ApprovalStateExecutionData.builder()
                                                   .approvalStateType(ApprovalStateType.SERVICENOW)
                                                   .approvedBy(EmbeddedUser.builder().name(USER_NAME).build())
                                                   .build();
    final List<String> keys = GetSweepingOutputKeys(captor, executionData);
    assertSnowKeysInSweepingOutput(keys);
  }

  private List<String> GetSweepingOutputKeys(
      ArgumentCaptor<SweepingOutputInstance> captor, ApprovalStateExecutionData executionData) {
    approvalState.fillSweepingOutput(context, executionData, null);

    verify(sweepingOutputService, times(1)).save(captor.capture());

    SweepingOutputInstance sweepingOutput = captor.getValue();
    Map<String, Object> data = (Map<String, Object>) KryoUtils.asInflatedObject(sweepingOutput.getOutput());

    reset(sweepingOutputService);
    return data.keySet().stream().collect(Collectors.toList());
  }

  private void assertUserGroupKeysInSweepingOutput(List<String> keys) {
    assertThat(keys).isNotNull().containsExactlyInAnyOrder(ApprovalState.APPROVAL_STATUS_KEY,
        ApprovalStateExecutionDataKeys.variables, ApprovalStateExecutionDataKeys.comments, "test",
        ApprovalStateExecutionDataKeys.approvedOn, ApprovalStateExecutionDataKeys.approvalFromSlack,
        ApprovalStateExecutionDataKeys.timeoutMillis, ApprovalStateExecutionDataKeys.approvalStateType,
        ApprovalStateExecutionDataKeys.approvedBy);
  }

  private void assertJiraKeysInSweepingOutput(List<String> keys) {
    assertThat(keys).isNotNull().containsExactlyInAnyOrder(ApprovalState.APPROVAL_STATUS_KEY,
        ApprovalStateExecutionDataKeys.variables, "test", ApprovalStateExecutionDataKeys.issueKey,
        ApprovalStateExecutionDataKeys.issueUrl, ApprovalStateExecutionDataKeys.currentStatus,
        ApprovalStateExecutionDataKeys.approvalField, ApprovalStateExecutionDataKeys.approvalValue,
        ApprovalStateExecutionDataKeys.rejectionField, ApprovalStateExecutionDataKeys.rejectionValue,
        ApprovalStateExecutionDataKeys.approvalFromSlack, ApprovalStateExecutionDataKeys.timeoutMillis,
        ApprovalStateExecutionDataKeys.approvalStateType);
  }

  private void assertSnowKeysInSweepingOutput(List<String> keys) {
    assertThat(keys).isNotNull().containsExactlyInAnyOrder(ApprovalState.APPROVAL_STATUS_KEY,
        ApprovalStateExecutionDataKeys.variables, "test", ApprovalStateExecutionDataKeys.ticketType,
        ApprovalStateExecutionDataKeys.ticketUrl, ApprovalStateExecutionDataKeys.approvalFromSlack,
        ApprovalStateExecutionDataKeys.timeoutMillis, ApprovalStateExecutionDataKeys.approvalStateType);
  }
}
