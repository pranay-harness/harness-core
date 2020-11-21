package software.wings.service.impl.workflow;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.PRABU;

import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.Workflow.WorkflowBuilder;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID_ARTIFACTORY;
import static software.wings.utils.WingsTestConstants.BUILD_NO;
import static software.wings.utils.WingsTestConstants.HELM_CHART_ID;
import static software.wings.utils.WingsTestConstants.MANIFEST_ID;
import static software.wings.utils.WingsTestConstants.PHASE_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.FeatureName;
import software.wings.beans.GraphNode;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.LastDeployedArtifactInformation;
import software.wings.beans.ManifestVariable;
import software.wings.beans.PhaseStepType;
import software.wings.beans.Service;
import software.wings.beans.VariableType;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.HelmChart;
import software.wings.beans.appmanifest.LastDeployedHelmChartInformation;
import software.wings.beans.appmanifest.ManifestSummary;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.applicationmanifest.HelmChartService;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateType;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;

public class WorkflowServiceImplTest extends WingsBaseTest {
  public static final String CHART_NAME = "CHART_NAME";
  public static final String VERSION = "VERSION";
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ArtifactService artifactService;
  @Mock private HelmChartService helmChartService;
  @Mock private AppService appService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private ApplicationManifestService applicationManifestService;
  @Mock private ServiceResourceService serviceResourceService;
  @InjectMocks @Inject private WorkflowService workflowService;
  @Mock private Query<WorkflowExecution> query;
  @Mock private Query<WorkflowExecution> emptyQuery;

  @Before
  public void setUp() {
    when(featureFlagService.isEnabled(eq(FeatureName.HELM_CHART_AS_ARTIFACT), anyString())).thenReturn(true);
    when(featureFlagService.isEnabled(eq(FeatureName.ARTIFACT_STREAM_REFACTOR), anyString())).thenReturn(false);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldGetLastDeployedArtifactFromPreviousIndirectExecution() {
    Workflow workflow =
        WorkflowBuilder.aWorkflow().name(WORKFLOW_NAME).uuid(WORKFLOW_ID).accountId(ACCOUNT_ID).appId(APP_ID).build();

    List<Artifact> artifacts =
        asList(anArtifact()
                   .withUuid(ARTIFACT_ID)
                   .withArtifactSourceName(ARTIFACT_SOURCE_NAME)
                   .withArtifactStreamId(ARTIFACT_STREAM_ID)
                   .withMetadata(Collections.singletonMap(ArtifactMetadataKeys.buildNo, BUILD_NO))
                   .build());

    List<HelmChart> helmCharts = asList(HelmChart.builder()
                                            .uuid(HELM_CHART_ID)
                                            .serviceId(SERVICE_ID)
                                            .name(CHART_NAME)
                                            .applicationManifestId(MANIFEST_ID)
                                            .version(VERSION)
                                            .build());
    WorkflowExecution workflowExecution =
        WorkflowExecution.builder()
            .artifacts(artifacts)
            .helmCharts(helmCharts)
            .appId(APP_ID)
            .pipelineResumeId(PIPELINE_EXECUTION_ID)
            .pipelineExecutionId(PIPELINE_EXECUTION_ID)
            .pipelineSummary(PipelineSummary.builder().pipelineId(PIPELINE_ID).pipelineName(PIPELINE_NAME).build())
            .build();

    when(wingsPersistence.createQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.accountId, workflow.getAccountId())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.appId, workflow.getAppId())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.workflowId, workflow.getUuid())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.status, SUCCESS)).thenReturn(query);
    when(query.order(any(Sort.class))).thenReturn(query);
    when(query.get()).thenReturn(workflowExecution);
    PageResponse<Artifact> pageResponse = new PageResponse<>();
    pageResponse.setResponse(artifacts);
    when(artifactService.listArtifactsForService(APP_ID, SERVICE_ID, new PageRequest<>())).thenReturn(pageResponse);
    WorkflowServiceImpl workflowServiceImpl = (WorkflowServiceImpl) workflowService;
    LastDeployedArtifactInformation artifactInformation = workflowServiceImpl.fetchLastDeployedArtifact(
        workflow, asList(ARTIFACT_STREAM_ID, ARTIFACT_STREAM_ID_ARTIFACTORY), SERVICE_ID);
    assertThat(artifactInformation).isNotNull();
    assertThat(artifactInformation.getArtifact().getArtifactSourceName()).isEqualTo(ARTIFACT_SOURCE_NAME);
    assertThat(artifactInformation.getArtifact().getBuildNo()).isEqualTo(BUILD_NO);
    assertThat(artifactInformation.getExecutionId()).isEqualTo(PIPELINE_EXECUTION_ID);
    assertThat(artifactInformation.getExecutionEntityId()).isEqualTo(PIPELINE_ID);
    assertThat(artifactInformation.getExecutionEntityType()).isEqualTo(WorkflowType.PIPELINE);
    assertThat(artifactInformation.getExecutionEntityName()).isEqualTo(PIPELINE_NAME);

    PageResponse<HelmChart> pageResponse2 = new PageResponse<>();
    pageResponse2.setResponse(helmCharts);
    when(helmChartService.listHelmChartsForService(APP_ID, SERVICE_ID, new PageRequest<>())).thenReturn(pageResponse2);

    LastDeployedHelmChartInformation helmChartInformation =
        workflowServiceImpl.fetchLastDeployedHelmChart(workflow, SERVICE_ID);
    assertThat(helmChartInformation).isNotNull();
    assertThat(helmChartInformation.getHelmchart().getName()).isEqualTo(CHART_NAME);
    assertThat(helmChartInformation.getHelmchart().getVersion()).isEqualTo(VERSION);
    assertThat(helmChartInformation.getExecutionId()).isEqualTo(PIPELINE_EXECUTION_ID);
    assertThat(helmChartInformation.getExecutionEntityId()).isEqualTo(PIPELINE_ID);
    assertThat(helmChartInformation.getExecutionEntityType()).isEqualTo(WorkflowType.PIPELINE);
    assertThat(helmChartInformation.getExecutionEntityName()).isEqualTo(PIPELINE_NAME);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldReturnNullIfNoSuccessfulExecutionFound() {
    Workflow workflow =
        WorkflowBuilder.aWorkflow().name(WORKFLOW_NAME).uuid(WORKFLOW_ID).accountId(ACCOUNT_ID).appId(APP_ID).build();

    when(wingsPersistence.createQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.accountId, workflow.getAccountId())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.appId, workflow.getAppId())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.workflowId, workflow.getUuid())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.status, SUCCESS)).thenReturn(emptyQuery);
    when(emptyQuery.order(any(Sort.class))).thenReturn(emptyQuery);
    when(emptyQuery.get()).thenReturn(null);
    WorkflowServiceImpl workflowServiceImpl = (WorkflowServiceImpl) workflowService;
    LastDeployedArtifactInformation artifactInformation = workflowServiceImpl.fetchLastDeployedArtifact(
        workflow, asList(ARTIFACT_STREAM_ID, ARTIFACT_STREAM_ID_ARTIFACTORY), SERVICE_ID);
    assertThat(artifactInformation).isNull();

    LastDeployedHelmChartInformation helmChartInformation =
        workflowServiceImpl.fetchLastDeployedHelmChart(workflow, SERVICE_ID);
    assertThat(helmChartInformation).isNull();
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldReturnNullIfArtifactStreamOrAppManifestChanged() {
    Workflow workflow =
        WorkflowBuilder.aWorkflow().name(WORKFLOW_NAME).uuid(WORKFLOW_ID).accountId(ACCOUNT_ID).appId(APP_ID).build();

    WorkflowExecution workflowExecution =
        WorkflowExecution.builder()
            .artifacts(asList(anArtifact()
                                  .withArtifactSourceName(ARTIFACT_SOURCE_NAME)
                                  .withArtifactStreamId(ARTIFACT_STREAM_ID)
                                  .withMetadata(Collections.singletonMap(ArtifactMetadataKeys.buildNo, BUILD_NO))
                                  .build()))
            .status(ExecutionStatus.SUCCESS)
            .pipelineExecutionId(PIPELINE_EXECUTION_ID)
            .pipelineSummary(PipelineSummary.builder().pipelineId(PIPELINE_ID).pipelineName(PIPELINE_NAME).build())
            .build();

    when(wingsPersistence.createQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);
    when(query.order(any(Sort.class))).thenReturn(query);
    when(query.get()).thenReturn(workflowExecution);
    WorkflowServiceImpl workflowServiceImpl = (WorkflowServiceImpl) workflowService;
    LastDeployedArtifactInformation artifactInformation =
        workflowServiceImpl.fetchLastDeployedArtifact(workflow, asList(ARTIFACT_STREAM_ID_ARTIFACTORY), SERVICE_ID);
    assertThat(artifactInformation).isNull();

    LastDeployedHelmChartInformation helmChartInformation =
        workflowServiceImpl.fetchLastDeployedHelmChart(workflow, SERVICE_ID);
    assertThat(helmChartInformation).isNull();
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldReturnNullIfArtifactOrManifestModifiedInStream() {
    Workflow workflow =
        WorkflowBuilder.aWorkflow().name(WORKFLOW_NAME).uuid(WORKFLOW_ID).accountId(ACCOUNT_ID).appId(APP_ID).build();

    List<Artifact> artifacts =
        asList(anArtifact()
                   .withUuid(ARTIFACT_ID)
                   .withArtifactSourceName(ARTIFACT_SOURCE_NAME)
                   .withArtifactStreamId(ARTIFACT_STREAM_ID)
                   .withMetadata(Collections.singletonMap(ArtifactMetadataKeys.buildNo, BUILD_NO))
                   .build());
    List<HelmChart> helmCharts = asList(HelmChart.builder()
                                            .uuid(HELM_CHART_ID)
                                            .serviceId(SERVICE_ID)
                                            .name(CHART_NAME)
                                            .applicationManifestId(MANIFEST_ID)
                                            .version(VERSION)
                                            .build());
    WorkflowExecution workflowExecution =
        WorkflowExecution.builder()
            .artifacts(artifacts)
            .helmCharts(helmCharts)
            .appId(APP_ID)
            .pipelineResumeId(PIPELINE_EXECUTION_ID)
            .pipelineExecutionId(PIPELINE_EXECUTION_ID)
            .pipelineSummary(PipelineSummary.builder().pipelineId(PIPELINE_ID).pipelineName(PIPELINE_NAME).build())
            .build();

    when(wingsPersistence.createQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.accountId, workflow.getAccountId())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.appId, workflow.getAppId())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.workflowId, workflow.getUuid())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.status, SUCCESS)).thenReturn(query);
    when(query.order(any(Sort.class))).thenReturn(query);
    when(query.get()).thenReturn(workflowExecution);
    PageResponse<Artifact> pageResponse = new PageResponse<>();
    pageResponse.setResponse(
        asList(anArtifact()
                   .withUuid(ARTIFACT_ID + 2)
                   .withArtifactSourceName(ARTIFACT_SOURCE_NAME)
                   .withArtifactStreamId(ARTIFACT_STREAM_ID)
                   .withMetadata(Collections.singletonMap(ArtifactMetadataKeys.buildNo, BUILD_NO + 2))
                   .build()));
    when(artifactService.listArtifactsForService(APP_ID, SERVICE_ID, new PageRequest<>())).thenReturn(pageResponse);

    PageResponse<HelmChart> pageResponse2 = new PageResponse<>();
    pageResponse2.setResponse(asList(HelmChart.builder()
                                         .uuid(HELM_CHART_ID + 2)
                                         .serviceId(SERVICE_ID)
                                         .name(CHART_NAME)
                                         .applicationManifestId(MANIFEST_ID)
                                         .version(VERSION + 2)
                                         .build()));
    when(helmChartService.listHelmChartsForService(APP_ID, SERVICE_ID, new PageRequest<>())).thenReturn(pageResponse2);
    WorkflowServiceImpl workflowServiceImpl = (WorkflowServiceImpl) workflowService;
    LastDeployedArtifactInformation artifactInformation = workflowServiceImpl.fetchLastDeployedArtifact(
        workflow, asList(ARTIFACT_STREAM_ID, ARTIFACT_STREAM_ID_ARTIFACTORY), SERVICE_ID);
    assertThat(artifactInformation).isNull();

    LastDeployedHelmChartInformation helmChartInformation =
        workflowServiceImpl.fetchLastDeployedHelmChart(workflow, SERVICE_ID);
    assertThat(helmChartInformation).isNull();
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldPopulateManifestVariableForHttpShellScript() {
    GraphNode shellScript = GraphNode.builder()
                                .type(StateType.SHELL_SCRIPT.name())
                                .name("printHelm")
                                .properties(ImmutableMap.<String, Object>builder()
                                                .put("scriptType", "BASH")
                                                .put("scriptString", "echo ${helmChart.name}")
                                                .put("executeOnDelegate", "true")
                                                .put("timeout", 120000)
                                                .build())
                                .build();

    Workflow workflow = createWorkflowWithPhaseStep(shellScript);

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().storeType(StoreType.HelmChartRepo).pollForChanges(true).build();
    when(applicationManifestService.getManifestByServiceId(APP_ID, SERVICE_ID)).thenReturn(applicationManifest);

    DeploymentMetadata deploymentMetadata =
        workflowService.fetchDeploymentMetadata(APP_ID, workflow, Collections.EMPTY_MAP, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST, false, null, DeploymentMetadata.Include.ARTIFACT_SERVICE);

    assertThat(deploymentMetadata.getManifestVariables()).isNotNull();
    assertThat(deploymentMetadata.getManifestVariables()).hasSize(1);
    ManifestVariable variable = deploymentMetadata.getManifestVariables().get(0);

    assertThat(variable.getType()).isEqualTo(VariableType.MANIFEST);
    assertThat(variable.getName()).isEqualTo("helmChart");

    GraphNode httpStep = GraphNode.builder()
                             .type(StateType.HTTP.name())
                             .name("accessHelmRepo")
                             .properties(ImmutableMap.<String, Object>builder()
                                             .put("url", "${helmChart.url}/list")
                                             .put("timeout", 120000)
                                             .build())
                             .build();

    workflow = aWorkflow()
                   .name(WORKFLOW_NAME)
                   .appId(APP_ID)
                   .workflowType(WorkflowType.ORCHESTRATION)
                   .orchestrationWorkflow(aBasicOrchestrationWorkflow()
                                              .withRollbackWorkflowPhaseIdMap(Collections.singletonMap(PHASE_ID,
                                                  aWorkflowPhase()
                                                      .serviceId(SERVICE_ID)
                                                      .phaseSteps(asList(aPhaseStep(PhaseStepType.COLLECT_ARTIFACT,
                                                          PhaseStepType.COLLECT_ARTIFACT.toString())
                                                                             .addStep(httpStep)
                                                                             .build()))
                                                      .build()))
                                              .build())
                   .templatized(true)
                   .build();

    deploymentMetadata = workflowService.fetchDeploymentMetadata(APP_ID, workflow, Collections.EMPTY_MAP,
        Collections.EMPTY_LIST, Collections.EMPTY_LIST, false, null, DeploymentMetadata.Include.ARTIFACT_SERVICE);

    assertThat(deploymentMetadata.getManifestVariables()).isNotNull();
    assertThat(deploymentMetadata.getManifestVariables()).hasSize(1);
    variable = deploymentMetadata.getManifestVariables().get(0);

    assertThat(variable.getType()).isEqualTo(VariableType.MANIFEST);
    assertThat(variable.getName()).isEqualTo("helmChart");
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldPopulateManifestVariableForK8sHelmDeploy() {
    GraphNode helmDeploy = GraphNode.builder().type(StateType.HELM_DEPLOY.name()).name("helmDeploy").build();

    Workflow workflow = createWorkflowWithPhaseStep(helmDeploy);

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().storeType(StoreType.HelmChartRepo).pollForChanges(true).build();
    when(applicationManifestService.getManifestByServiceId(APP_ID, SERVICE_ID)).thenReturn(applicationManifest);

    DeploymentMetadata deploymentMetadata =
        workflowService.fetchDeploymentMetadata(APP_ID, workflow, Collections.EMPTY_MAP, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST, false, null, DeploymentMetadata.Include.ARTIFACT_SERVICE);

    assertThat(deploymentMetadata.getManifestVariables()).isNotNull();
    assertThat(deploymentMetadata.getManifestVariables()).hasSize(1);
    ManifestVariable variable = deploymentMetadata.getManifestVariables().get(0);

    assertThat(variable.getType()).isEqualTo(VariableType.MANIFEST);
    assertThat(variable.getName()).isEqualTo("helmChart");

    GraphNode k8sDeploy = GraphNode.builder().type(StateType.K8S_CANARY_DEPLOY.name()).name("K8sDeploy").build();

    workflow = aWorkflow()
                   .name(WORKFLOW_NAME)
                   .appId(APP_ID)
                   .workflowType(WorkflowType.ORCHESTRATION)
                   .orchestrationWorkflow(aBasicOrchestrationWorkflow()
                                              .withRollbackWorkflowPhaseIdMap(Collections.singletonMap(PHASE_ID,
                                                  aWorkflowPhase()
                                                      .serviceId(SERVICE_ID)
                                                      .phaseSteps(asList(aPhaseStep(PhaseStepType.COLLECT_ARTIFACT,
                                                          PhaseStepType.COLLECT_ARTIFACT.toString())
                                                                             .addStep(k8sDeploy)
                                                                             .build()))
                                                      .build()))
                                              .build())
                   .templatized(true)
                   .build();

    deploymentMetadata = workflowService.fetchDeploymentMetadata(APP_ID, workflow, Collections.EMPTY_MAP,
        Collections.EMPTY_LIST, Collections.EMPTY_LIST, false, null, DeploymentMetadata.Include.ARTIFACT_SERVICE);

    assertThat(deploymentMetadata.getManifestVariables()).isNotNull();
    assertThat(deploymentMetadata.getManifestVariables()).hasSize(1);
    variable = deploymentMetadata.getManifestVariables().get(0);

    assertThat(variable.getType()).isEqualTo(VariableType.MANIFEST);
    assertThat(variable.getName()).isEqualTo("helmChart");
  }

  @NotNull
  private Workflow createWorkflowWithPhaseStep(GraphNode helmDeploy) {
    return aWorkflow()
        .name(WORKFLOW_NAME)
        .appId(APP_ID)
        .workflowType(WorkflowType.ORCHESTRATION)
        .orchestrationWorkflow(
            aBasicOrchestrationWorkflow()
                .withWorkflowPhases(asList(aWorkflowPhase()
                                               .serviceId(SERVICE_ID)
                                               .phaseSteps(asList(aPhaseStep(PhaseStepType.COLLECT_ARTIFACT,
                                                   PhaseStepType.COLLECT_ARTIFACT.toString())
                                                                      .addStep(helmDeploy)
                                                                      .build()))
                                               .build()))
                .build())
        .templatized(true)
        .build();
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldPopulateAppManifestSummaryAndServiceName() {
    GraphNode k8sDeploy = GraphNode.builder().type(StateType.K8S_BLUE_GREEN_DEPLOY.name()).name("K8sDeploy").build();

    Workflow workflow = createWorkflowWithPhaseStep(k8sDeploy);

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder()
            .storeType(StoreType.HelmChartRepo)
            .pollForChanges(true)
            .helmChartConfig(HelmChartConfig.builder().connectorId(SETTING_ID).build())
            .build();
    applicationManifest.setUuid(MANIFEST_ID);
    when(applicationManifestService.getManifestByServiceId(APP_ID, SERVICE_ID)).thenReturn(applicationManifest);
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(Service.builder().name(SERVICE_NAME).build());
    when(helmChartService.getLastCollectedManifest(ACCOUNT_ID, MANIFEST_ID))
        .thenReturn(HelmChart.builder().uuid(HELM_CHART_ID).name("chart").version("1").displayName("chart-1").build());

    DeploymentMetadata deploymentMetadata =
        workflowService.fetchDeploymentMetadata(APP_ID, workflow, Collections.EMPTY_MAP, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST, false, null, DeploymentMetadata.Include.ARTIFACT_SERVICE);

    assertThat(deploymentMetadata.getManifestVariables()).hasSize(1);
    ManifestVariable manifestVariable = deploymentMetadata.getManifestVariables().get(0);
    ManifestSummary manifestSummary = manifestVariable.getApplicationManifestSummary().getLastCollectedManifest();
    assertThat(manifestSummary.getUuid()).isEqualTo(HELM_CHART_ID);
    assertThat(manifestSummary.getVersionNo()).isEqualTo("1");
    assertThat(manifestVariable.getServiceName()).isEqualTo(SERVICE_NAME);
    assertThat(manifestVariable.getApplicationManifestSummary().getDefaultManifest()).isNull();
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldPopulateDefaultVersionForRerun() {
    GraphNode k8sDeploy = GraphNode.builder().type(StateType.K8S_APPLY.name()).name("K8sDeploy").build();

    Workflow workflow = createWorkflowWithPhaseStep(k8sDeploy);

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder()
            .storeType(StoreType.HelmChartRepo)
            .pollForChanges(true)
            .helmChartConfig(HelmChartConfig.builder().connectorId(SETTING_ID).build())
            .build();
    applicationManifest.setUuid(MANIFEST_ID);
    when(applicationManifestService.getManifestByServiceId(APP_ID, SERVICE_ID)).thenReturn(applicationManifest);
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(Service.builder().name(SERVICE_NAME).build());
    when(helmChartService.getLastCollectedManifest(ACCOUNT_ID, MANIFEST_ID))
        .thenReturn(HelmChart.builder().uuid(HELM_CHART_ID).name("chart").version("1").displayName("chart-1").build());

    List<HelmChart> helmCharts = asList(HelmChart.builder()
                                            .uuid(HELM_CHART_ID + 1)
                                            .serviceId(SERVICE_ID)
                                            .name(CHART_NAME)
                                            .applicationManifestId(MANIFEST_ID)
                                            .version(VERSION + 1)
                                            .build());
    WorkflowExecution workflowExecution =
        WorkflowExecution.builder()
            .helmCharts(helmCharts)
            .appId(APP_ID)
            .pipelineResumeId(PIPELINE_EXECUTION_ID)
            .pipelineExecutionId(PIPELINE_EXECUTION_ID)
            .pipelineSummary(PipelineSummary.builder().pipelineId(PIPELINE_ID).pipelineName(PIPELINE_NAME).build())
            .build();

    DeploymentMetadata deploymentMetadata =
        workflowService.fetchDeploymentMetadata(APP_ID, workflow, Collections.EMPTY_MAP, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST, false, workflowExecution, DeploymentMetadata.Include.ARTIFACT_SERVICE);

    assertThat(deploymentMetadata.getManifestVariables()).hasSize(1);
    ManifestVariable manifestVariable = deploymentMetadata.getManifestVariables().get(0);
    ManifestSummary manifestSummary = manifestVariable.getApplicationManifestSummary().getLastCollectedManifest();
    assertThat(manifestSummary.getUuid()).isEqualTo(HELM_CHART_ID);
    assertThat(manifestSummary.getVersionNo()).isEqualTo("1");
    assertThat(manifestVariable.getServiceName()).isEqualTo(SERVICE_NAME);
    manifestSummary = manifestVariable.getApplicationManifestSummary().getDefaultManifest();
    assertThat(manifestSummary.getUuid()).isEqualTo(HELM_CHART_ID + 1);
    assertThat(manifestSummary.getVersionNo()).isEqualTo(VERSION + 1);
  }
}
