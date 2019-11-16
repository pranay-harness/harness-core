package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.ABORTED;
import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.POOJA;
import static io.harness.rule.OwnerRule.ROHIT;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.rule.OwnerRule.Owner;
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
import software.wings.api.WorkflowElement;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EnvSummary;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.User;
import software.wings.beans.User.Builder;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.common.NotificationMessageResolver;
import software.wings.security.SecretManager;
import software.wings.service.impl.workflow.WorkflowNotificationHelper;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.ApprovalState.ApprovalStateType;
import software.wings.sm.states.EnvState.EnvStateKeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

    Set<String> excludeFromAggregation = new HashSet<>();
    when(workflowExecutionService.getExecutionDetails(
             context.getAppId(), context.getWorkflowExecutionId(), true, excludeFromAggregation))
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
  @Owner(developers = ROHIT)
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
  @Owner(developers = ROHIT)
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
  @Owner(developers = POOJA)
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
  @Owner(developers = ANSHUL)
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
  @Owner(developers = POOJA)
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
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void testParseProperties() {
    final Map<String, Object> properties = new HashMap<>();
    properties.put(EnvStateKeys.disable, true);
    approvalState.parseProperties(properties);
    assertThat(properties.get(EnvStateKeys.disableAssertion)).isEqualTo("true");
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
}