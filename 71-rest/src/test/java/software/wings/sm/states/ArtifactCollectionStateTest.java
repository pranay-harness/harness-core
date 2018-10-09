package software.wings.sm.states;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.common.Constants.DEFAULT_ARTIFACT_COLLECTION_STATE_TIMEOUT_MILLIS;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.StateType.ARTIFACT_COLLECTION;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.api.ArtifactCollectionExecutionData;
import software.wings.api.WorkflowElement;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Application;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.common.VariableProcessor;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.scheduler.JobScheduler;
import software.wings.service.impl.DelayEventHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.WorkflowStandardParams.Builder;

import java.util.concurrent.TimeUnit;

public class ArtifactCollectionStateTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private JobScheduler jobScheduler;
  @Mock private ArtifactService artifactService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private AppService appService;
  @Mock MainConfiguration configuration;
  @Mock PortalConfig portalConfig;
  @Mock VariableProcessor variableProcessor;
  @Mock DelayEventHelper delayEventHelper;
  @Mock AccountService accountService;

  private ManagerExpressionEvaluator expressionEvaluator = new ManagerExpressionEvaluator();

  @InjectMocks
  private ArtifactCollectionState artifactCollectionState = new ArtifactCollectionState("Collect Artifact");

  private JenkinsArtifactStream jenkinsArtifactStream = JenkinsArtifactStream.builder()
                                                            .appId(APP_ID)
                                                            .uuid(ARTIFACT_STREAM_ID)
                                                            .sourceName(ARTIFACT_SOURCE_NAME)
                                                            .settingId(SETTING_ID)
                                                            .jobname("JOB")
                                                            .serviceId(SERVICE_ID)
                                                            .artifactPaths(asList("*WAR"))
                                                            .build();
  private WorkflowStandardParams workflowStandardParams =
      Builder.aWorkflowStandardParams()
          .withAppId(APP_ID)
          .withWorkflowElement(
              WorkflowElement.builder()
                  .variables(ImmutableMap.of("sourceCommitHash", "0fcb2caa537745f8228fb081aac2af55765d8e62"))
                  .build())
          .build();
  private StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                              .withDisplayName(StateType.ARTIFACT_COLLECTION.name())
                                                              .withOrchestrationWorkflowType(BUILD)
                                                              .addContextElement(workflowStandardParams)
                                                              .build();

  private ExecutionContextImpl executionContext = new ExecutionContextImpl(stateExecutionInstance);

  @Before
  public void setUp() throws Exception {
    artifactCollectionState.setArtifactStreamId(ARTIFACT_STREAM_ID);
    setInternalState(executionContext, "variableProcessor", variableProcessor);
    setInternalState(executionContext, "evaluator", expressionEvaluator);
    setInternalState(workflowStandardParams, "appService", appService);
    setInternalState(workflowStandardParams, "configuration", configuration);
    setInternalState(workflowStandardParams, "accountService", accountService);

    when(configuration.getPortal()).thenReturn(portalConfig);
    when(portalConfig.getUrl()).thenReturn("http://portalUrl");
    when(appService.get(APP_ID))
        .thenReturn(
            Application.Builder.anApplication().withAppId(APP_ID).withUuid(APP_ID).withAccountId(ACCOUNT_ID).build());
    when(appService.getApplicationWithDefaults(APP_ID))
        .thenReturn(
            Application.Builder.anApplication().withAppId(APP_ID).withUuid(APP_ID).withAccountId(ACCOUNT_ID).build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(artifactService.fetchLastCollectedApprovedArtifactForArtifactStream(
             APP_ID, ARTIFACT_STREAM_ID, ARTIFACT_SOURCE_NAME))
        .thenReturn(anArtifact().withAppId(APP_ID).withStatus(Status.APPROVED).build());
    when(delayEventHelper.delay(anyInt(), any())).thenReturn("anyGUID");
  }

  @Test
  public void shouldFailOnNoArtifactStream() {
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(null);
    ExecutionResponse executionResponse = artifactCollectionState.execute(executionContext);
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    verify(artifactStreamService).get(APP_ID, ARTIFACT_STREAM_ID);
  }

  @Test
  public void shouldExecuteWithDelayQueue() {
    ExecutionResponse executionResponse = artifactCollectionState.execute(executionContext);
    assertThat(executionResponse).isNotNull().hasFieldOrPropertyWithValue("async", true);
    verify(artifactStreamService).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(delayEventHelper).delay(anyInt(), any());
  }

  @Test
  public void shouldHandleAsyncResponse() {
    artifactCollectionState.handleAsyncResponse(executionContext,
        ImmutableMap.of(
            ACTIVITY_ID, ArtifactCollectionExecutionData.builder().artifactStreamId(ARTIFACT_STREAM_ID).build()));
    verify(workflowExecutionService).refreshBuildExecutionSummary(anyString(), anyString(), any());
  }

  @Test
  public void shouldArtifactCollectionEvaluateBuildNo() {
    artifactCollectionState.setBuildNo("${regex.extract('...', ${workflow.variables.sourceCommitHash})}");
    when(artifactService.getArtifactByBuildNumber(APP_ID, ARTIFACT_STREAM_ID, ARTIFACT_SOURCE_NAME, "0fc", false))
        .thenReturn(anArtifact().withAppId(APP_ID).withStatus(Status.APPROVED).build());
    artifactCollectionState.handleAsyncResponse(executionContext,
        ImmutableMap.of(
            ACTIVITY_ID, ArtifactCollectionExecutionData.builder().artifactStreamId(ARTIFACT_STREAM_ID).build()));
    verify(workflowExecutionService).refreshBuildExecutionSummary(anyString(), anyString(), any());
  }

  @Test
  @Ignore // for srinivas to modify; need to simulate real Jenkins
  public void shouldArtifactCollectionEvaluateBuildNoFromDescription() {
    artifactCollectionState.setBuildNo("${regex.replace('tag: ([\\w-]+)', '$1', ${Jenkins.description}}");
    when(artifactService.getArtifactByBuildNumber(APP_ID, ARTIFACT_STREAM_ID, ARTIFACT_SOURCE_NAME, "0fc", false))
        .thenReturn(anArtifact().withAppId(APP_ID).withStatus(Status.APPROVED).build());
    artifactCollectionState.handleAsyncResponse(executionContext,
        ImmutableMap.of(
            ACTIVITY_ID, ArtifactCollectionExecutionData.builder().artifactStreamId(ARTIFACT_STREAM_ID).build()));
    verify(workflowExecutionService).refreshBuildExecutionSummary(anyString(), anyString(), any());
  }

  @Test
  public void shouldGetTimeout() {
    Integer timeoutMillis = artifactCollectionState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo(Math.toIntExact(DEFAULT_ARTIFACT_COLLECTION_STATE_TIMEOUT_MILLIS));
  }

  @Test
  public void shouldGetSetTimeout() {
    artifactCollectionState.setTimeoutMillis((int) TimeUnit.HOURS.toMillis(1));
    Integer timeoutMillis = artifactCollectionState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo((int) TimeUnit.HOURS.toMillis(1));
  }

  @Test
  public void shouldHandleAbort() {
    executionContext.getStateExecutionInstance().setStateExecutionMap(
        ImmutableMap.of(ARTIFACT_COLLECTION.name(), ArtifactCollectionExecutionData.builder().build()));
    artifactCollectionState.handleAbortEvent(executionContext);
    assertThat(executionContext.getStateExecutionData()).isNotNull();
    assertThat(executionContext.getStateExecutionData().getErrorMsg()).isNotBlank();
  }
}
