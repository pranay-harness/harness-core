/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.PRABU;
import static io.harness.rule.OwnerRule.PUNEET;
import static io.harness.rule.OwnerRule.SRINIVAS;

import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.StateType.ARTIFACT_COLLECTION;
import static software.wings.sm.states.ArtifactCollectionState.DEFAULT_ARTIFACT_COLLECTION_STATE_TIMEOUT_MILLIS;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.delay.DelayEventHelper;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;

import software.wings.api.ArtifactCollectionExecutionData;
import software.wings.api.WorkflowElement;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Application;
import software.wings.beans.TemplateExpression;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.common.VariableProcessor;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.service.ArtifactStreamHelper;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.WorkflowStandardParams.Builder;
import software.wings.sm.states.ArtifactCollectionState.ArtifactCollectionStateKeys;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class ArtifactCollectionStateTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ArtifactService artifactService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private AppService appService;
  @Mock MainConfiguration configuration;
  @Mock PortalConfig portalConfig;
  @Mock VariableProcessor variableProcessor;
  @Mock DelayEventHelper delayEventHelper;
  @Mock AccountService accountService;
  @Mock FeatureFlagService featureFlagService;
  @Mock private SubdomainUrlHelperIntfc subdomainUrlHelper;
  @Mock private ArtifactStreamHelper artifactStreamHelper;
  @Mock private BuildSourceService buildSourceService;
  @Mock private ArtifactCollectionUtils artifactCollectionUtils;
  @Mock private TemplateExpressionProcessor templateExpressionProcessor;

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
  private NexusArtifactStream nexusArtifactStream = NexusArtifactStream.builder()
                                                        .accountId(ACCOUNT_ID)
                                                        .appId(APP_ID)
                                                        .settingId(SETTING_ID)
                                                        .jobname("${repo}")
                                                        .groupId("${groupId}")
                                                        .artifactPaths(asList("${path}"))
                                                        .autoPopulate(false)
                                                        .serviceId(SERVICE_ID)
                                                        .name("testNexus")
                                                        .uuid(ARTIFACT_STREAM_ID)
                                                        .sourceName(ARTIFACT_SOURCE_NAME)
                                                        .build();

  private WorkflowStandardParams workflowStandardParams =
      Builder.aWorkflowStandardParams()
          .withAppId(APP_ID)
          .withWorkflowElement(WorkflowElement.builder()
                                   .variables(ImmutableMap.of("sourceCommitHash",
                                       "0fcb2caa537745f8228fb081aac2af55765d8e62", "buildNo", "1.1"))
                                   .build())
          .build();
  private StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                              .displayName(StateType.ARTIFACT_COLLECTION.name())
                                                              .orchestrationWorkflowType(BUILD)
                                                              .addContextElement(workflowStandardParams)
                                                              .build();

  private ExecutionContextImpl executionContext = new ExecutionContextImpl(stateExecutionInstance);

  @Before
  public void setUp() throws Exception {
    artifactCollectionState.setArtifactStreamId(ARTIFACT_STREAM_ID);
    FieldUtils.writeField(executionContext, "variableProcessor", variableProcessor, true);
    FieldUtils.writeField(executionContext, "evaluator", expressionEvaluator, true);
    FieldUtils.writeField(executionContext, "featureFlagService", featureFlagService, true);
    FieldUtils.writeField(workflowStandardParams, "appService", appService, true);
    FieldUtils.writeField(workflowStandardParams, "configuration", configuration, true);
    FieldUtils.writeField(workflowStandardParams, "accountService", accountService, true);
    on(workflowStandardParams).set("subdomainUrlHelper", subdomainUrlHelper);

    when(configuration.getPortal()).thenReturn(portalConfig);
    when(portalConfig.getUrl()).thenReturn("http://portalUrl");
    when(appService.get(APP_ID))
        .thenReturn(Application.Builder.anApplication().appId(APP_ID).uuid(APP_ID).accountId(ACCOUNT_ID).build());
    when(appService.getApplicationWithDefaults(APP_ID))
        .thenReturn(Application.Builder.anApplication().appId(APP_ID).uuid(APP_ID).accountId(ACCOUNT_ID).build());
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(artifactService.fetchLastCollectedApprovedArtifactForArtifactStream(jenkinsArtifactStream))
        .thenReturn(anArtifact().withAppId(APP_ID).withStatus(Status.APPROVED).build());
    when(delayEventHelper.delay(anyInt(), any())).thenReturn("anyGUID");
    when(featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, ACCOUNT_ID)).thenReturn(false);
    when(subdomainUrlHelper.getPortalBaseUrl(any())).thenReturn("baseUrl");
    nexusArtifactStream.setArtifactStreamParameterized(true);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldFailOnNoArtifactStream() {
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(null);
    ExecutionResponse executionResponse = artifactCollectionState.execute(executionContext);
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    verify(artifactStreamService).get(ARTIFACT_STREAM_ID);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void shouldExecuteWithDelayQueue() {
    when(artifactService.fetchLastCollectedApprovedArtifactForArtifactStream(jenkinsArtifactStream)).thenReturn(null);
    ExecutionResponse executionResponse = artifactCollectionState.execute(executionContext);
    assertThat(executionResponse).isNotNull().hasFieldOrPropertyWithValue("async", true);
    verify(artifactStreamService).get(ARTIFACT_STREAM_ID);
    verify(delayEventHelper).delay(anyInt(), any());
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldHandleAsyncResponse() {
    artifactCollectionState.handleAsyncResponse(executionContext,
        ImmutableMap.of(
            ACTIVITY_ID, ArtifactCollectionExecutionData.builder().artifactStreamId(ARTIFACT_STREAM_ID).build()));
    verify(workflowExecutionService).refreshBuildExecutionSummary(anyString(), any());
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldArtifactCollectionEvaluateBuildNo() {
    artifactCollectionState.setBuildNo("${regex.extract('...', ${workflow.variables.sourceCommitHash})}");
    when(artifactService.getArtifactByBuildNumber(jenkinsArtifactStream, "0fc", false))
        .thenReturn(anArtifact().withAppId(APP_ID).withStatus(Status.APPROVED).build());
    artifactCollectionState.handleAsyncResponse(executionContext,
        ImmutableMap.of(
            ACTIVITY_ID, ArtifactCollectionExecutionData.builder().artifactStreamId(ARTIFACT_STREAM_ID).build()));
    verify(workflowExecutionService).refreshBuildExecutionSummary(anyString(), any());
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  @Ignore("for srinivas to modify; need to simulate real Jenkins")
  public void shouldArtifactCollectionEvaluateBuildNoFromDescription() {
    artifactCollectionState.setBuildNo("${regex.replace('tag: ([\\w-]+)', '$1', ${Jenkins.description}}");
    when(artifactService.getArtifactByBuildNumber(jenkinsArtifactStream, "0fc", false))
        .thenReturn(anArtifact().withAppId(APP_ID).withStatus(Status.APPROVED).build());
    artifactCollectionState.handleAsyncResponse(executionContext,
        ImmutableMap.of(
            ACTIVITY_ID, ArtifactCollectionExecutionData.builder().artifactStreamId(ARTIFACT_STREAM_ID).build()));
    verify(workflowExecutionService).refreshBuildExecutionSummary(anyString(), any());
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetTimeout() {
    Integer timeoutMillis = artifactCollectionState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo(Math.toIntExact(DEFAULT_ARTIFACT_COLLECTION_STATE_TIMEOUT_MILLIS));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetSetTimeout() {
    artifactCollectionState.setTimeoutMillis((int) TimeUnit.HOURS.toMillis(1));
    Integer timeoutMillis = artifactCollectionState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo((int) TimeUnit.HOURS.toMillis(1));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldHandleAbort() {
    executionContext.getStateExecutionInstance().setStateExecutionMap(
        ImmutableMap.of(ARTIFACT_COLLECTION.name(), ArtifactCollectionExecutionData.builder().build()));
    artifactCollectionState.handleAbortEvent(executionContext);
    assertThat(executionContext.getStateExecutionData()).isNotNull();

    // TODO: getErrorMsg returns null - is this expected
    // assertThat(executionContext.getStateExecutionData().getErrorMsg()).isNotBlank();
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldExecuteForParameterizedArtifactStreamWithLastCollectedArtifact() {
    Map<String, Object> runtimeValues = new HashMap<>();
    runtimeValues.put("repo", "harness-maven");
    runtimeValues.put("group", "mygroup");
    runtimeValues.put("artifactId", "todolist");
    artifactCollectionState.setRuntimeValues(runtimeValues);
    artifactCollectionState.setBuildNo("1.0");
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(nexusArtifactStream);
    when(artifactService.getArtifactByBuildNumberAndSourceName(
             nexusArtifactStream, "1.0", false, "${repo}/${groupId}/${path}"))
        .thenReturn(anArtifact().withAppId(APP_ID).withStatus(Status.APPROVED).build());
    artifactCollectionState.execute(executionContext);
    verify(artifactStreamService).get(ARTIFACT_STREAM_ID);
    verify(workflowExecutionService).refreshBuildExecutionSummary(anyString(), any());
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldExecuteForParameterizedArtifactStreamWithNewBuild() {
    Map<String, Object> runtimeValues = new HashMap<>();
    runtimeValues.put("repo", "harness-maven");
    runtimeValues.put("group", "mygroup");
    runtimeValues.put("artifactId", "todolist");
    artifactCollectionState.setRuntimeValues(runtimeValues);
    artifactCollectionState.setBuildNo("${workflow.variables.buildNo}");
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(nexusArtifactStream);
    when(artifactService.getArtifactByBuildNumberAndSourceName(
             nexusArtifactStream, "1.1", false, "${repo}/${groupId}/${path}"))
        .thenReturn(null);
    when(buildSourceService.getBuild(anyString(), anyString(), anyString(), any()))
        .thenReturn(BuildDetails.Builder.aBuildDetails().withNumber("1.1").build());
    Map<String, String> map = new HashMap<>();
    map.put("buildNo", "1.1");
    Artifact artifact = Artifact.Builder.anArtifact().withMetadata(map).withUuid(ARTIFACT_ID).build();
    when(artifactCollectionUtils.getArtifact(any(), any())).thenReturn(artifact);
    when(artifactService.create(artifact, nexusArtifactStream, false)).thenReturn(artifact);
    artifactCollectionState.execute(executionContext);
    verify(artifactStreamService).get(ARTIFACT_STREAM_ID);
    verify(workflowExecutionService).refreshBuildExecutionSummary(anyString(), any());
    assertThat(runtimeValues.get("buildNo")).isEqualTo("1.1");
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldNotExecuteWithoutRuntimeValuesForParameterizedArtifactStream() {
    Map<String, Object> runtimeValues = new HashMap<>();
    artifactCollectionState.setRuntimeValues(runtimeValues);
    artifactCollectionState.setBuildNo("1.1");
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(nexusArtifactStream);
    ExecutionResponse executionResponse = artifactCollectionState.execute(executionContext);
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldNotExecuteWithoutBuildNumberForParameterizedArtifactStream() {
    Map<String, Object> runtimeValues = new HashMap<>();
    runtimeValues.put("repo", "harness-maven");
    runtimeValues.put("group", "mygroup");
    runtimeValues.put("artifactId", "todolist");
    artifactCollectionState.setRuntimeValues(runtimeValues);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(nexusArtifactStream);
    ExecutionResponse executionResponse = artifactCollectionState.execute(executionContext);
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldNotExecuteIfArtifactCollectionFailsForParameterizedArtifactStream() {
    Map<String, Object> runtimeValues = new HashMap<>();
    runtimeValues.put("repo", "harness-maven");
    runtimeValues.put("group", "mygroup");
    runtimeValues.put("artifactId", "todolist");
    artifactCollectionState.setRuntimeValues(runtimeValues);
    artifactCollectionState.setBuildNo("1.1");
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(nexusArtifactStream);
    when(artifactService.getArtifactByBuildNumberAndSourceName(
             nexusArtifactStream, "1.1", false, "${repo}/${groupId}/${path}"))
        .thenReturn(null);
    when(buildSourceService.getBuild(anyString(), anyString(), anyString(), any())).thenReturn(null);
    ExecutionResponse executionResponse = artifactCollectionState.execute(executionContext);
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldExecuteForTemplatizedArtifactStream() {
    List<TemplateExpression> templateExpressions =
        Collections.singletonList(TemplateExpression.builder()
                                      .fieldName(ArtifactCollectionStateKeys.artifactStreamId)
                                      .expression("${ArtifactStream}")
                                      .build());
    artifactCollectionState.setTemplateExpressions(templateExpressions);
    artifactCollectionState.setBuildNo("${workflow.variables.buildNo}");
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(nexusArtifactStream);

    nexusArtifactStream.setArtifactStreamParameterized(false);
    when(templateExpressionProcessor.getTemplateExpression(
             templateExpressions, ArtifactCollectionStateKeys.artifactStreamId))
        .thenReturn(templateExpressions.get(0));
    when(templateExpressionProcessor.resolveTemplateExpression(executionContext, templateExpressions.get(0)))
        .thenReturn(ARTIFACT_STREAM_ID);
    when(artifactService.fetchLastCollectedApprovedArtifactForArtifactStream(nexusArtifactStream))
        .thenReturn(Artifact.Builder.anArtifact().build());
    Map<String, String> map = new HashMap<>();
    map.put("buildNo", "1.1");
    Artifact artifact = Artifact.Builder.anArtifact().withMetadata(map).withUuid(ARTIFACT_ID).build();
    when(artifactCollectionUtils.getArtifact(any(), any())).thenReturn(artifact);

    ExecutionResponse executionResponse = artifactCollectionState.execute(executionContext);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldExecuteForTemplatizedArtifactStreamByName() {
    List<TemplateExpression> templateExpressions =
        Collections.singletonList(TemplateExpression.builder()
                                      .fieldName(ArtifactCollectionStateKeys.artifactStreamId)
                                      .expression("${ArtifactStream}")
                                      .build());
    artifactCollectionState.setTemplateExpressions(templateExpressions);
    artifactCollectionState.setBuildNo("${workflow.variables.buildNo}");
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(nexusArtifactStream);

    when(templateExpressionProcessor.getTemplateExpression(
             templateExpressions, ArtifactCollectionStateKeys.artifactStreamId))
        .thenReturn(templateExpressions.get(0));
    when(templateExpressionProcessor.resolveTemplateExpression(executionContext, templateExpressions.get(0)))
        .thenReturn(ARTIFACT_SOURCE_NAME);
    when(artifactStreamService.fetchByArtifactSourceVariableValue(APP_ID, ARTIFACT_SOURCE_NAME))
        .thenReturn(nexusArtifactStream);

    when(artifactService.fetchLastCollectedApprovedArtifactForArtifactStream(nexusArtifactStream))
        .thenReturn(Artifact.Builder.anArtifact().build());
    Map<String, String> map = new HashMap<>();
    map.put("buildNo", "1.1");
    Artifact artifact = Artifact.Builder.anArtifact().withMetadata(map).withUuid(ARTIFACT_ID).build();
    when(artifactCollectionUtils.getArtifact(any(), any())).thenReturn(artifact);

    ExecutionResponse executionResponse = artifactCollectionState.execute(executionContext);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(executionResponse.getErrorMessage())
        .isEqualTo(
            "Parameterized Artifact Source testNexus cannot be used as a value for templatized artifact variable");
  }
}
