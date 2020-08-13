package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.RIHAZ;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.appmanifest.AppManifestKind.HELM_CHART_OVERRIDE;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.helpers.ext.k8s.request.K8sValuesLocation.ServiceOverride;
import static software.wings.service.intfc.ServiceTemplateService.EncryptedFieldComputeMode.OBTAIN_VALUE;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.StateType.HELM_DEPLOY;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.RELEASE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.STATE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import io.harness.beans.DelegateTask;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.SweepingOutputInstance;
import io.harness.category.element.UnitTests;
import io.harness.container.ContainerInfo;
import io.harness.delegate.beans.DelegateTaskDetails;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.HelmClientException;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.VariableResolverTracker;
import io.harness.k8s.model.HelmVersion;
import io.harness.k8s.model.ImageDetails;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.Key;
import software.wings.WingsBaseTest;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.api.HelmDeployContextElement;
import software.wings.api.HelmDeployStateExecutionData;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElement.Builder;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.FeatureName;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.HelmExecutionSummary;
import software.wings.beans.InfraMappingSweepingOutput;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.common.InfrastructureConstants;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.common.VariableProcessor;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.features.api.FeatureService;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.helm.HelmCommandExecutionResponse;
import software.wings.helpers.ext.helm.HelmHelper;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.request.HelmCommandRequest.HelmCommandType;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.request.HelmValuesFetchTaskParameters;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;
import software.wings.helpers.ext.helm.response.HelmReleaseHistoryCommandResponse;
import software.wings.helpers.ext.helm.response.HelmValuesFetchTaskResponse;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.GitConfigHelperService;
import software.wings.service.impl.GitFileConfigHelperService;
import software.wings.service.impl.HelmChartConfigHelperService;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.impl.servicetemplates.ServiceTemplateHelper;
import software.wings.service.impl.yaml.GitClientHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.k8s.K8sStateHelper;
import software.wings.utils.ApplicationManifestUtils;
import software.wings.utils.WingsTestConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelmDeployStateTest extends WingsBaseTest {
  private static final String HELM_CONTROLLER_NAME = "helm-controller-name";
  private static final String HELM_RELEASE_NAME_PREFIX = "helm-release-name-prefix";
  private static final String CHART_NAME = "chart-name";
  private static final String CHART_VERSION = "0.1.0";
  private static final String CHART_URL = "http://google.com";
  private static final String GIT_CONNECTOR_ID = "connectorId";
  public static final String GIT_BRANCH = "git_branch";
  public static final String GIT_COMMIT_ID = "commit_id";
  public static final String GIT_FILE_PATH_DIRECTORY = "templates/";
  public static final String GIT_FILE_PATH_FULL_DIRECTORY = "/templates/";
  public static final String GIT_YAML_FILE_PATH = "templates/values.yaml";
  public static final String GIT_YML_FILE_PATH = "templates/values.yml";
  public static final String GIT_NOT_YML_FILE_PATH = "templates/values";
  public static final String FILE_PATH_VALIDATION_MSG_KEY = "File path";
  public static final String FILE_PATH_DIRECTORY_VALIDATION_MSG_VALUE =
      "File path cannot be directory if git connector is selected";
  public static final String FILE_PATH_NOT_YAML_FILE_VALIDATION_MSG_VALUE =
      "File path has to be YAML file if git connector is selected";
  private static final String COMMAND_FLAGS = "--tls";
  private static final String PHASE_NAME = "phaseName";

  @Mock private AppService appService;
  @Mock private ArtifactService artifactService;
  @Mock private ActivityService activityService;
  @Mock private DelegateService delegateService;
  @Mock private EnvironmentService environmentService;
  @Mock private ManagerExpressionEvaluator evaluator;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private MainConfiguration configuration;
  @Mock private PortalConfig portalConfig;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private VariableProcessor variableProcessor;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private SecretManager secretManager;
  @Mock private ContainerDeploymentManagerHelper containerDeploymentHelper;
  @Mock private SettingsService settingsService;
  @Mock private GitConfigHelperService gitConfigHelperService;
  @Mock private ArtifactCollectionUtils artifactCollectionUtils;
  @Mock private ApplicationManifestService applicationManifestService;
  @Mock private ApplicationManifestUtils applicationManifestUtils;
  @Mock private HelmChartConfigHelperService helmChartConfigHelperService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private ServiceTemplateHelper serviceTemplateHelper;
  @Mock private SweepingOutputService sweepingOutputService;
  @Mock private HelmHelper helmHelper;
  @Mock private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private GitFileConfigHelperService gitFileConfigHelperService;
  @Mock private SubdomainUrlHelperIntfc subdomainUrlHelper;
  @Mock private LogService logService;
  @Mock private K8sStateHelper k8sStateHelper;
  @Mock private FeatureService featureService;
  @Mock private StateExecutionService stateExecutionService;
  @Mock private GitClientHelper gitClientHelper;
  @Mock private TemplateExpressionProcessor templateExpressionProcessor;

  @InjectMocks HelmDeployState helmDeployState = new HelmDeployState("helmDeployState");
  @InjectMocks HelmRollbackState helmRollbackState = new HelmRollbackState("helmRollbackState");

  @InjectMocks
  private WorkflowStandardParams workflowStandardParams = aWorkflowStandardParams()
                                                              .withAppId(APP_ID)
                                                              .withEnvId(ENV_ID)
                                                              .withArtifactIds(Lists.newArrayList(ARTIFACT_ID))
                                                              .build();
  private ServiceElement serviceElement = ServiceElement.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build();

  @InjectMocks
  private PhaseElement phaseElement = PhaseElement.builder()
                                          .uuid(generateUuid())
                                          .serviceElement(serviceElement)
                                          .infraMappingId(INFRA_MAPPING_ID)
                                          .workflowExecutionId(WORKFLOW_EXECUTION_ID)
                                          .phaseName(PHASE_NAME)
                                          .deploymentType(DeploymentType.HELM.name())
                                          .build();

  private StateExecutionInstance stateExecutionInstance =
      aStateExecutionInstance()
          .displayName(STATE_NAME)
          .addContextElement(workflowStandardParams)
          .addContextElement(phaseElement)
          .addContextElement(ContainerServiceElement.builder()
                                 .uuid(serviceElement.getUuid())
                                 .maxInstances(10)
                                 .clusterName(CLUSTER_NAME)
                                 .namespace("default")
                                 .name(HELM_CONTROLLER_NAME)
                                 .resizeStrategy(RESIZE_NEW_FIRST)
                                 .infraMappingId(INFRA_MAPPING_ID)
                                 .deploymentType(DeploymentType.KUBERNETES)
                                 .build())
          .addStateExecutionData(HelmDeployStateExecutionData.builder().build())
          .build();

  private InfrastructureMapping infrastructureMapping = aGcpKubernetesInfrastructureMapping()
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .withClusterName(CLUSTER_NAME)
                                                            .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                                                            .withDeploymentType(DeploymentType.KUBERNETES.name())
                                                            .build();

  private String outputName = InfrastructureConstants.PHASE_INFRA_MAPPING_KEY_NAME + phaseElement.getUuid();
  private SweepingOutputInstance sweepingOutputInstance =
      SweepingOutputInstance.builder()
          .appId(APP_ID)
          .name(outputName)
          .uuid(generateUuid())
          .workflowExecutionId(WORKFLOW_EXECUTION_ID)
          .stateExecutionId(null)
          .pipelineExecutionId(null)
          .value(InfraMappingSweepingOutput.builder().infraMappingId(INFRA_MAPPING_ID).build())
          .build();

  private Application app = anApplication().uuid(APP_ID).name(APP_NAME).build();
  private Environment env = anEnvironment().appId(APP_ID).uuid(ENV_ID).name(ENV_NAME).build();
  private Service service = Service.builder().appId(APP_ID).uuid(SERVICE_ID).name(SERVICE_NAME).build();
  private ExecutionContextImpl context;

  @Before
  public void setup() throws InterruptedException {
    context = new ExecutionContextImpl(stateExecutionInstance);
    helmDeployState.setHelmReleaseNamePrefix(HELM_RELEASE_NAME_PREFIX);

    EmbeddedUser currentUser = EmbeddedUser.builder().name("test").email("test@harness.io").build();
    workflowStandardParams.setCurrentUser(currentUser);
    when(sweepingOutputService.find(any())).thenReturn(sweepingOutputInstance);

    when(appService.get(APP_ID)).thenReturn(app);
    when(appService.getApplicationWithDefaults(APP_ID)).thenReturn(app);
    when(serviceResourceService.getWithDetails(APP_ID, SERVICE_ID)).thenReturn(service);
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(service);
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(env);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(infrastructureMapping);

    when(activityService.save(any(Activity.class))).thenReturn(Activity.builder().uuid(ACTIVITY_ID).build());
    when(secretManager.getEncryptionDetails(anyObject(), anyString(), anyString())).thenReturn(Collections.emptyList());
    when(configuration.getPortal()).thenReturn(portalConfig);
    when(portalConfig.getUrl()).thenReturn("http://www.url.com");
    when(serviceTemplateService.getTemplateRefKeysByService(APP_ID, SERVICE_ID, ENV_ID))
        .thenReturn(singletonList(new Key<>(ServiceTemplate.class, "serviceTemplate", TEMPLATE_ID)));

    when(serviceTemplateService.get(APP_ID, TEMPLATE_ID)).thenReturn(aServiceTemplate().withUuid(TEMPLATE_ID).build());
    when(serviceTemplateService.computeServiceVariables(APP_ID, ENV_ID, TEMPLATE_ID, null, OBTAIN_VALUE))
        .thenReturn(emptyList());
    when(workflowExecutionService.getExecutionDetails(anyString(), anyString(), anyBoolean()))
        .thenReturn(WorkflowExecution.builder().build());
    when(containerDeploymentHelper.getContainerServiceParams(any(), any(), any()))
        .thenReturn(ContainerServiceParams.builder().build());
    when(artifactCollectionUtils.fetchContainerImageDetails(any(), any())).thenReturn(ImageDetails.builder().build());

    when(delegateService.executeTask(any()))
        .thenReturn(HelmCommandExecutionResponse.builder()
                        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                        .helmCommandResponse(HelmReleaseHistoryCommandResponse.builder().build())
                        .build());
    when(variableProcessor.getVariables(any(), any())).thenReturn(emptyMap());
    when(evaluator.substitute(anyString(), anyMap(), any(VariableResolverTracker.class), anyString()))
        .thenAnswer(i -> i.getArguments()[0]);
    when(featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, ACCOUNT_ID)).thenReturn(false);

    on(workflowStandardParams).set("appService", appService);
    on(workflowStandardParams).set("environmentService", environmentService);
    on(workflowStandardParams).set("featureFlagService", featureFlagService);

    on(context).set("infrastructureMappingService", infrastructureMappingService);
    on(context).set("serviceResourceService", serviceResourceService);
    on(context).set("artifactService", artifactService);
    on(context).set("variableProcessor", variableProcessor);
    on(context).set("evaluator", evaluator);
    on(context).set("featureFlagService", featureFlagService);
    on(context).set("stateExecutionInstance", stateExecutionInstance);
    on(context).set("sweepingOutputService", sweepingOutputService);
    on(workflowStandardParams).set("subdomainUrlHelper", subdomainUrlHelper);
    when(featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, ACCOUNT_ID)).thenReturn(false);
    when(subdomainUrlHelper.getPortalBaseUrl(any())).thenReturn("baseUrl");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecute() throws InterruptedException {
    helmDeployState.setSteadyStateTimeout(5);
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder()
            .storeType(StoreType.HelmSourceRepo)
            .gitFileConfig(GitFileConfig.builder().connectorId(GIT_CONNECTOR_ID).build())
            .build();
    when(serviceResourceService.getHelmChartSpecification(APP_ID, SERVICE_ID))
        .thenReturn(HelmChartSpecification.builder()
                        .chartName(CHART_NAME)
                        .chartUrl(CHART_URL)
                        .chartVersion(CHART_VERSION)
                        .build());
    when(serviceTemplateHelper.fetchServiceTemplateId(any())).thenReturn(SERVICE_TEMPLATE_ID);
    when(applicationManifestService.getAppManifest(APP_ID, null, SERVICE_ID, AppManifestKind.K8S_MANIFEST))
        .thenReturn(applicationManifest);
    when(gitFileConfigHelperService.renderGitFileConfig(any(), any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(1, GitFileConfig.class));
    when(settingsService.fetchGitConfigFromConnectorId(GIT_CONNECTOR_ID)).thenReturn(GitConfig.builder().build());
    ExecutionResponse executionResponse = helmDeployState.execute(context);
    assertStateExecutionResponse(executionResponse);

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertExecutedDelegateTask(delegateTask);

    verify(delegateService).executeTask(any());
    verify(gitConfigHelperService, times(1)).renderGitConfig(any(), any());
    verify(gitFileConfigHelperService, times(1)).renderGitFileConfig(any(), any());
    verify(stateExecutionService, times(2)).appendDelegateTaskDetails(anyString(), any(DelegateTaskDetails.class));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteWithHelmChartRepo() throws InterruptedException {
    helmDeployState.setSteadyStateTimeout(5);
    HelmChartConfig chartConfigWithConnectorId = HelmChartConfig.builder().connectorId("connectorId").build();
    HelmChartConfig chartConfigWithoutConnectorId =
        HelmChartConfig.builder().chartVersion("1.0.0").chartUrl(CHART_URL).chartName(CHART_NAME).build();
    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .storeType(StoreType.HelmChartRepo)
                                                  .helmChartConfig(chartConfigWithConnectorId)
                                                  .build();

    when(serviceResourceService.getHelmChartSpecification(APP_ID, SERVICE_ID))
        .thenReturn(HelmChartSpecification.builder()
                        .chartName(CHART_NAME)
                        .chartUrl(CHART_URL)
                        .chartVersion(CHART_VERSION)
                        .build());
    when(applicationManifestService.getAppManifest(APP_ID, null, SERVICE_ID, AppManifestKind.K8S_MANIFEST))
        .thenReturn(applicationManifest);
    when(helmChartConfigHelperService.getHelmChartConfigTaskParams(context, applicationManifest))
        .thenReturn(HelmChartConfigParams.builder().repoName("repoName").build());
    // Case when connectorId is not blank
    ExecutionResponse executionResponse = helmDeployState.execute(context);
    assertStateExecutionResponse(executionResponse);

    // Case when connectorId is blank
    applicationManifest.setHelmChartConfig(chartConfigWithoutConnectorId);
    when(applicationManifestService.getAppManifest(APP_ID, null, SERVICE_ID, AppManifestKind.K8S_MANIFEST))
        .thenReturn(applicationManifest);
    executionResponse = helmDeployState.execute(context);
    assertStateExecutionResponse(executionResponse);

    verify(applicationManifestUtils, times(0))
        .applyK8sValuesLocationBasedHelmChartOverride(any(), any(Map.class), any());
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(2)).queueTask(captor.capture());
    List<DelegateTask> delegateTasks = captor.getAllValues();

    assertExecutedDelegateTask(delegateTasks.get(0));
    assertExecutedDelegateTask(delegateTasks.get(1));

    verify(delegateService, times(2)).executeTask(any());
  }

  private void assertStateExecutionResponse(ExecutionResponse executionResponse) {
    assertThat(executionResponse.isAsync()).isEqualTo(true);
    assertThat(executionResponse.getCorrelationIds()).contains(ACTIVITY_ID);
    HelmDeployStateExecutionData helmDeployStateExecutionData =
        (HelmDeployStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(helmDeployStateExecutionData.getActivityId()).isEqualTo(ACTIVITY_ID);
    assertThat(helmDeployStateExecutionData.getChartName()).isEqualTo(CHART_NAME);
    assertThat(helmDeployStateExecutionData.getChartRepositoryUrl()).isEqualTo(CHART_URL);
    assertThat(helmDeployStateExecutionData.getReleaseName()).isEqualTo(HELM_RELEASE_NAME_PREFIX);
    assertThat(helmDeployStateExecutionData.getReleaseOldVersion()).isEqualTo(0);
    assertThat(helmDeployStateExecutionData.getReleaseNewVersion()).isEqualTo(1);
    assertThat(helmDeployStateExecutionData.getCommandFlags()).isNull();
  }

  private void assertExecutedDelegateTask(DelegateTask delegateTask) {
    HelmInstallCommandRequest helmInstallCommandRequest =
        (HelmInstallCommandRequest) delegateTask.getData().getParameters()[0];
    assertThat(delegateTask.getData().getTimeout()).isEqualTo(300000);
    assertThat(helmInstallCommandRequest.getHelmCommandType()).isEqualTo(HelmCommandType.INSTALL);
    assertThat(helmInstallCommandRequest.getReleaseName()).isEqualTo(HELM_RELEASE_NAME_PREFIX);
    assertThat(helmInstallCommandRequest.getRepoName()).isEqualTo("app-name-service-name");
    assertThat(helmInstallCommandRequest.getCommandFlags()).isNull();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecuteWithNullChartSpec() {
    when(serviceResourceService.getHelmChartSpecification(APP_ID, SERVICE_ID)).thenReturn(null);

    helmDeployState.execute(context);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecuteWithNullReleaseName() {
    when(serviceResourceService.getHelmChartSpecification(APP_ID, SERVICE_ID)).thenReturn(null);
    helmDeployState.setHelmReleaseNamePrefix(null);

    helmDeployState.execute(context);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testErrorResponseFromDelegate() throws InterruptedException {
    when(serviceResourceService.getHelmChartSpecification(APP_ID, SERVICE_ID))
        .thenReturn(HelmChartSpecification.builder()
                        .chartName(CHART_NAME)
                        .chartUrl(CHART_URL)
                        .chartVersion(CHART_VERSION)
                        .build());

    when(delegateService.executeTask(any())).thenReturn(RemoteMethodReturnValueData.builder().build());

    helmDeployState.execute(context);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testEmptyHelmChartSpec() {
    helmDeployState.execute(context);
    verify(serviceResourceService).getHelmChartSpecification(APP_ID, SERVICE_ID);
    verify(delegateService, never()).queueTask(any());
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testEmptyHelmChartSpecWithGit() {
    when(settingsService.fetchGitConfigFromConnectorId(GIT_CONNECTOR_ID)).thenReturn(GitConfig.builder().build());
    when(serviceTemplateHelper.fetchServiceTemplateId(any())).thenReturn(SERVICE_TEMPLATE_ID);
    doNothing().when(gitConfigHelperService).setSshKeySettingAttributeIfNeeded(any());
    helmDeployState.setGitFileConfig(GitFileConfig.builder().connectorId(GIT_CONNECTOR_ID).build());
    ExecutionResponse executionResponse = helmDeployState.execute(context);
    assertThat(executionResponse.isAsync()).isEqualTo(true);
    HelmDeployStateExecutionData helmDeployStateExecutionData =
        (HelmDeployStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(helmDeployStateExecutionData.getChartName()).isEqualTo(null);
    assertThat(helmDeployStateExecutionData.getChartRepositoryUrl()).isEqualTo(null);
    verify(delegateService).queueTask(any());
    verify(gitClientHelper).updateRepoUrl(any(GitConfig.class), anyString());
  }

  @Test()
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testHelmChartSpecWithGitDirectoryFilePath() {
    helmDeployState.setGitFileConfig(GitFileConfig.builder()
                                         .connectorId(GIT_CONNECTOR_ID)
                                         .commitId(GIT_COMMIT_ID)
                                         .filePath(GIT_FILE_PATH_DIRECTORY)
                                         .build());

    Map<String, String> invalidFields = helmDeployState.validateFields();

    assertThat(invalidFields).containsKey(FILE_PATH_VALIDATION_MSG_KEY);
    assertThat(invalidFields).containsValue(FILE_PATH_DIRECTORY_VALIDATION_MSG_VALUE);
  }

  @Test()
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testHelmChartSpecWithGitFullDirectoryFilePath() {
    helmDeployState.setGitFileConfig(GitFileConfig.builder()
                                         .connectorId(GIT_CONNECTOR_ID)
                                         .branch(GIT_BRANCH)
                                         .filePath(GIT_FILE_PATH_FULL_DIRECTORY)
                                         .build());

    Map<String, String> invalidFields = helmDeployState.validateFields();

    assertThat(invalidFields).containsKey(FILE_PATH_VALIDATION_MSG_KEY);
    assertThat(invalidFields).containsValue(FILE_PATH_DIRECTORY_VALIDATION_MSG_VALUE);
  }

  @Test()
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testHelmChartSpecWithGitYAMLFilePath() {
    helmDeployState.setGitFileConfig(
        GitFileConfig.builder().connectorId(GIT_CONNECTOR_ID).branch(GIT_BRANCH).filePath(GIT_YAML_FILE_PATH).build());

    Map<String, String> invalidFields = helmDeployState.validateFields();

    assertThat(invalidFields).isEmpty();
  }

  @Test()
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testHelmChartSpecWithGitYMLFilePath() {
    helmDeployState.setGitFileConfig(
        GitFileConfig.builder().connectorId(GIT_CONNECTOR_ID).branch(GIT_BRANCH).filePath(GIT_YML_FILE_PATH).build());

    Map<String, String> invalidFields = helmDeployState.validateFields();

    assertThat(invalidFields).isEmpty();
  }

  @Test()
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testHelmChartSpecWithGitNotYAMLFilePath() {
    helmDeployState.setGitFileConfig(GitFileConfig.builder()
                                         .connectorId(GIT_CONNECTOR_ID)
                                         .branch(GIT_BRANCH)
                                         .filePath(GIT_NOT_YML_FILE_PATH)
                                         .build());

    Map<String, String> invalidFields = helmDeployState.validateFields();

    assertThat(invalidFields).containsKey(FILE_PATH_VALIDATION_MSG_KEY);
    assertThat(invalidFields).containsValue(FILE_PATH_NOT_YAML_FILE_VALIDATION_MSG_VALUE);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHelmChartSpecWithGitEmptyYAMLFilePath() {
    helmDeployState.setGitFileConfig(
        GitFileConfig.builder().connectorId(GIT_CONNECTOR_ID).branch(GIT_BRANCH).filePath("").build());

    Map<String, String> invalidFields = helmDeployState.validateFields();

    assertThat(invalidFields).containsKey(FILE_PATH_VALIDATION_MSG_KEY);
    assertThat(invalidFields).containsValue("File path must not be blank if git connector is selected");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHelmChartSpecWithInvalidGitBranch() {
    helmDeployState.setGitFileConfig(
        GitFileConfig.builder().connectorId(GIT_CONNECTOR_ID).filePath(GIT_NOT_YML_FILE_PATH).build());

    Map<String, String> invalidFields = helmDeployState.validateFields();
    assertThat(invalidFields).containsKey("Branch or commit id");
    assertThat(invalidFields).containsValue("Branch or commit id must not be blank if git connector is selected");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHelmChartSpecWithMissingHelmReleaseNamePrefix() {
    Map<String, String> invalidFields;
    helmDeployState.setHelmReleaseNamePrefix(null);

    invalidFields = helmDeployState.validateFields();
    assertThat(invalidFields).containsKey("Helm release name prefix");
    assertThat(invalidFields.get("Helm release name prefix")).isEqualTo("Helm release name prefix must not be blank");

    helmDeployState.setHelmReleaseNamePrefix("");
    invalidFields = helmDeployState.validateFields();
    assertThat(invalidFields).containsKey("Helm release name prefix");
    assertThat(invalidFields.get("Helm release name prefix")).isEqualTo("Helm release name prefix must not be blank");

    helmDeployState.setHelmReleaseNamePrefix("     ");
    invalidFields = helmDeployState.validateFields();
    assertThat(invalidFields).containsKey("Helm release name prefix");
    assertThat(invalidFields.get("Helm release name prefix")).isEqualTo("Helm release name prefix must not be blank");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecuteWithCommandFlags() {
    helmDeployState.setCommandFlags(COMMAND_FLAGS);
    when(serviceResourceService.getHelmChartSpecification(APP_ID, SERVICE_ID))
        .thenReturn(HelmChartSpecification.builder()
                        .chartName(CHART_NAME)
                        .chartUrl(CHART_URL)
                        .chartVersion(CHART_VERSION)
                        .build());
    when(serviceTemplateHelper.fetchServiceTemplateId(any())).thenReturn(SERVICE_TEMPLATE_ID);
    when(k8sStateHelper.fetchTagsFromK8sCloudProvider(any())).thenReturn(Arrays.asList("delegateName"));
    ExecutionResponse executionResponse = helmDeployState.execute(context);
    HelmDeployStateExecutionData helmDeployStateExecutionData =
        (HelmDeployStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(helmDeployStateExecutionData.getCommandFlags()).isEqualTo(COMMAND_FLAGS);

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();

    assertThat(delegateTask.getTags()).isEqualTo(Arrays.asList("delegateName"));
    HelmInstallCommandRequest helmInstallCommandRequest =
        (HelmInstallCommandRequest) delegateTask.getData().getParameters()[0];
    assertThat(helmInstallCommandRequest.getCommandFlags()).isEqualTo(COMMAND_FLAGS);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecuteWithHelmRollbackForCommandFlags() {
    when(serviceResourceService.getHelmChartSpecification(APP_ID, SERVICE_ID))
        .thenReturn(HelmChartSpecification.builder()
                        .chartName(CHART_NAME)
                        .chartUrl(CHART_URL)
                        .chartVersion(CHART_VERSION)
                        .build());
    context.pushContextElement(HelmDeployContextElement.builder()
                                   .releaseName(HELM_RELEASE_NAME_PREFIX)
                                   .commandFlags(COMMAND_FLAGS)
                                   .newReleaseRevision(2)
                                   .previousReleaseRevision(1)
                                   .build());

    ExecutionResponse executionResponse = helmRollbackState.execute(context);
    HelmDeployStateExecutionData helmDeployStateExecutionData =
        (HelmDeployStateExecutionData) executionResponse.getStateExecutionData();

    assertThat(helmDeployStateExecutionData.getReleaseName()).isEqualTo(HELM_RELEASE_NAME_PREFIX);
    assertThat(helmDeployStateExecutionData.getCommandFlags()).isEqualTo(COMMAND_FLAGS);

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();

    HelmRollbackCommandRequest helmRollbackCommandRequest =
        (HelmRollbackCommandRequest) delegateTask.getData().getParameters()[0];
    assertThat(helmRollbackCommandRequest.getCommandFlags()).isEqualTo(COMMAND_FLAGS);
    assertThat(helmRollbackCommandRequest.getReleaseName()).isEqualTo(HELM_RELEASE_NAME_PREFIX);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseForHelmTaskInSuccess() {
    HelmCommandExecutionResponse helmCommandResponse =
        HelmCommandExecutionResponse.builder()
            .helmCommandResponse(HelmInstallCommandResponse.builder()
                                     .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                     .containerInfoList(emptyList())
                                     .helmChartInfo(HelmChartInfo.builder().build())
                                     .build())
            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
            .build();

    Map<String, ResponseData> response = new HashMap<>();
    response.put("activityId", helmCommandResponse);

    String instanceElementName = "instanceElement";
    List<InstanceStatusSummary> instanceStatusSummaries = Lists.newArrayList(
        InstanceStatusSummaryBuilder.anInstanceStatusSummary()
            .withStatus(ExecutionStatus.SUCCESS)
            .withInstanceElement(Builder.anInstanceElement().displayName(instanceElementName).build())
            .build());

    when(containerDeploymentHelper.getInstanceStatusSummaries(any(), any())).thenReturn(instanceStatusSummaries);
    ExecutionResponse executionResponse = helmDeployState.handleAsyncResponseForHelmTask(context, response);

    verify(activityService).updateStatus("activityId", APP_ID, ExecutionStatus.SUCCESS);
    verify(containerDeploymentHelper, times(1)).getInstanceStatusSummaries(any(), any());
    verify(workflowExecutionService, times(1)).getWorkflowExecution(any(), any());
    assertThat(executionResponse.getContextElements()).isNotEmpty();
    assertThat(((InstanceElementListParam) executionResponse.getContextElements().get(0))
                   .getInstanceElements()
                   .get(0)
                   .getName())
        .isEqualTo(instanceElementName);

    assertThat(executionResponse.getNotifyElements()).isNotEmpty();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getStateExecutionData()).isNotNull();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseForHelmTaskInFailure() {
    HelmCommandExecutionResponse helmCommandResponse = HelmCommandExecutionResponse.builder()
                                                           .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                                           .errorMessage("Failed")
                                                           .build();

    Map<String, ResponseData> response = new HashMap<>();
    response.put("activityId", helmCommandResponse);

    ExecutionResponse executionResponse = helmDeployState.handleAsyncResponseForHelmTask(context, response);

    verify(activityService).updateStatus("activityId", APP_ID, ExecutionStatus.FAILED);
    verify(containerDeploymentHelper, times(0)).getInstanceStatusSummaries(any(), any());
    verify(workflowExecutionService, times(0)).getWorkflowExecution(any(), any());
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(executionResponse.getErrorMessage()).isEqualTo("Failed");
    assertThat(executionResponse.getStateExecutionData()).isNotNull();
    assertThat(executionResponse.getNotifyElements()).isEmpty();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseForHelmTaskWithInstallCommandResponseFailure() {
    HelmCommandExecutionResponse helmCommandResponse =
        HelmCommandExecutionResponse.builder()
            .errorMessage("Failed")
            .helmCommandResponse(HelmInstallCommandResponse.builder()
                                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                     .helmChartInfo(HelmChartInfo.builder().build())
                                     .containerInfoList(emptyList())
                                     .build())
            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
            .build();

    Map<String, ResponseData> response = new HashMap<>();
    response.put("activityId", helmCommandResponse);

    ExecutionResponse executionResponse = helmDeployState.handleAsyncResponseForHelmTask(context, response);

    verify(activityService).updateStatus("activityId", APP_ID, ExecutionStatus.SUCCESS);
    verify(containerDeploymentHelper, times(0)).getInstanceStatusSummaries(any(), any());
    verify(workflowExecutionService, times(1)).getWorkflowExecution(any(), any());
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getErrorMessage()).isEqualTo("Failed");
    assertThat(executionResponse.getStateExecutionData()).isNotNull();
    assertThat(executionResponse.getNotifyElements()).isEmpty();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseForGitFetchFilesTaskForFailure() throws InterruptedException {
    GitCommandExecutionResponse gitCommandExecutionResponse =
        GitCommandExecutionResponse.builder().gitCommandStatus(GitCommandStatus.FAILURE).build();

    HelmDeployStateExecutionData helmStateExecutionData =
        (HelmDeployStateExecutionData) context.getStateExecutionData();

    String activityId = "activityId";
    helmStateExecutionData.setActivityId(activityId);
    helmStateExecutionData.setCurrentTaskType(TaskType.GIT_COMMAND);

    Map<String, ResponseData> response = new HashMap<>();
    response.put(activityId, gitCommandExecutionResponse);

    ExecutionResponse executionResponse = helmDeployState.handleAsyncInternal(context, response);

    verify(activityService).updateStatus("activityId", APP_ID, ExecutionStatus.FAILED);
    verify(applicationManifestUtils, times(0))
        .getValuesFilesFromGitFetchFilesResponse(
            anyMapOf(K8sValuesLocation.class, ApplicationManifest.class), any(GitCommandExecutionResponse.class));

    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseForHelmFetchTaskForFailure() throws InterruptedException {
    HelmValuesFetchTaskResponse helmValuesFetchTaskResponse =
        HelmValuesFetchTaskResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();

    HelmDeployStateExecutionData helmStateExecutionData =
        (HelmDeployStateExecutionData) context.getStateExecutionData();

    String activityId = "activityId";
    helmStateExecutionData.setActivityId(activityId);
    helmStateExecutionData.setCurrentTaskType(TaskType.HELM_VALUES_FETCH);

    Map<String, ResponseData> response = new HashMap<>();
    response.put(activityId, helmValuesFetchTaskResponse);
    ExecutionResponse executionResponse = helmDeployState.handleAsyncInternal(context, response);

    verify(activityService).updateStatus("activityId", APP_ID, ExecutionStatus.FAILED);
    verify(applicationManifestUtils, times(0))
        .getValuesFilesFromGitFetchFilesResponse(
            anyMapOf(K8sValuesLocation.class, ApplicationManifest.class), any(GitCommandExecutionResponse.class));
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseOverallFailure() throws Exception {
    HelmDeployState spyDeployState = spy(helmDeployState);
    HelmValuesFetchTaskResponse response = HelmValuesFetchTaskResponse.builder().build();
    Map<String, ResponseData> responseDataMap = ImmutableMap.of(ACTIVITY_ID, response);

    // Rethrow instance of WingsException
    doThrow(new HelmClientException("Client exception"))
        .when(spyDeployState)
        .handleAsyncInternal(context, responseDataMap);
    assertThatThrownBy(() -> spyDeployState.handleAsyncResponse(context, responseDataMap))
        .isInstanceOf(HelmClientException.class);

    // Throw InvalidRequestException on RuntimeException
    doThrow(new RuntimeException("Some exception got thrown"))
        .when(spyDeployState)
        .handleAsyncInternal(context, responseDataMap);
    assertThatThrownBy(() -> spyDeployState.handleAsyncResponse(context, responseDataMap))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseForHelmFetchTask() {
    HelmDeployStateExecutionData stateExecutionData = (HelmDeployStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setCurrentTaskType(TaskType.HELM_VALUES_FETCH);
    doReturn(
        HelmChartSpecification.builder().chartName(CHART_NAME).chartUrl(CHART_URL).chartVersion(CHART_VERSION).build())
        .when(serviceResourceService)
        .getHelmChartSpecification(APP_ID, SERVICE_ID);

    testHandleAsyncResponseForHelmFetchTaskWithValuesInGit();
    testHandleAsyncResponseForHelmFetchTaskWithNoValuesInGit();
  }

  private void testHandleAsyncResponseForHelmFetchTaskWithValuesInGit() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    HelmValuesFetchTaskResponse response = HelmValuesFetchTaskResponse.builder()
                                               .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                               .valuesFileContent("fileContent")
                                               .build();
    Map<String, ResponseData> responseDataMap = ImmutableMap.of(ACTIVITY_ID, response);

    doReturn(appManifestMap)
        .when(applicationManifestUtils)
        .getOverrideApplicationManifests(context, AppManifestKind.VALUES);
    doReturn(GitFetchFilesTaskParams.builder().build())
        .when(applicationManifestUtils)
        .createGitFetchFilesTaskParams(context, app, appManifestMap);

    ArgumentCaptor<DelegateTask> delegateTaskCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    doReturn("taskId").when(delegateService).queueTask(delegateTaskCaptor.capture());
    doReturn(true).when(applicationManifestUtils).isValuesInGit(appManifestMap);
    helmDeployState.handleAsyncResponse(context, responseDataMap);
    assertThat(delegateTaskCaptor.getValue().getData().getTaskType()).isEqualTo(TaskType.GIT_FETCH_FILES_TASK.name());
  }

  private void testHandleAsyncResponseForHelmFetchTaskWithNoValuesInGit() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    HelmValuesFetchTaskResponse response =
        HelmValuesFetchTaskResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
    Map<String, ResponseData> responseDataMap = ImmutableMap.of(ACTIVITY_ID, response);
    ArgumentCaptor<DelegateTask> delegateTaskCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    doReturn("taskId").when(delegateService).queueTask(delegateTaskCaptor.capture());

    doReturn(false).when(applicationManifestUtils).isValuesInGit(appManifestMap);
    helmDeployState.handleAsyncResponse(context, responseDataMap);
    assertThat(delegateTaskCaptor.getValue().getData().getTaskType()).isEqualTo(TaskType.HELM_COMMAND_TASK.name());
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testHelmDeployWithCustomArtifact() {
    when(serviceResourceService.getHelmChartSpecification(APP_ID, SERVICE_ID))
        .thenReturn(HelmChartSpecification.builder()
                        .chartName(CHART_NAME)
                        .chartUrl(CHART_URL)
                        .chartVersion(CHART_VERSION)
                        .build());
    when(serviceTemplateHelper.fetchServiceTemplateId(any())).thenReturn(SERVICE_TEMPLATE_ID);
    when(artifactCollectionUtils.fetchContainerImageDetails(any(), any())).thenReturn(ImageDetails.builder().build());
    when(artifactService.get(ARTIFACT_ID)).thenReturn(anArtifact().withArtifactStreamId(ARTIFACT_STREAM_ID).build());
    when(artifactStreamServiceBindingService.listArtifactStreamIds(anyString()))
        .thenReturn(Arrays.asList(ARTIFACT_STREAM_ID));

    HelmDeployStateExecutionData helmStateExecutionData =
        (HelmDeployStateExecutionData) context.getStateExecutionData();
    String valuesFile = "# imageName: ${DOCKER_IMAGE_NAME}\n"
        + "# tag: ${DOCKER_IMAGE_TAG}";
    Map<K8sValuesLocation, Collection<String>> valuesFiles = new HashMap<>();
    valuesFiles.put(K8sValuesLocation.Service, singletonList(valuesFile));
    helmStateExecutionData.setValuesFiles(valuesFiles);

    helmDeployState.execute(context);

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    HelmInstallCommandRequest helmInstallCommandRequest =
        (HelmInstallCommandRequest) delegateTask.getData().getParameters()[0];
    assertThat(helmInstallCommandRequest.getVariableOverridesYamlFiles().get(0)).isEqualTo(valuesFile);

    when(artifactCollectionUtils.fetchContainerImageDetails(any(), any()))
        .thenReturn(ImageDetails.builder().name("IMAGE_NAME").tag("TAG").build());
    helmDeployState.execute(context);

    captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(2)).queueTask(captor.capture());
    delegateTask = captor.getValue();
    helmInstallCommandRequest = (HelmInstallCommandRequest) delegateTask.getData().getParameters()[0];
    String renderedValuesFile = "# imageName: IMAGE_NAME\n"
        + "# tag: TAG";
    assertThat(helmInstallCommandRequest.getVariableOverridesYamlFiles().get(0)).isEqualTo(renderedValuesFile);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testPriorityOrderOfValuesYamlFile() {
    Map<K8sValuesLocation, Collection<String>> k8sValuesLocationContentMap = new HashMap<>();
    k8sValuesLocationContentMap.put(ServiceOverride, singletonList("ServiceOverride"));
    k8sValuesLocationContentMap.put(K8sValuesLocation.Service, singletonList("Service"));
    k8sValuesLocationContentMap.put(K8sValuesLocation.Environment, singletonList("Environment"));
    k8sValuesLocationContentMap.put(K8sValuesLocation.EnvironmentGlobal, singletonList("EnvironmentGlobal"));
    List<String> expectedValuesYamlList =
        Arrays.asList("Service", "ServiceOverride", "EnvironmentGlobal", "Environment");

    List<String> actualValuesYamlList = helmDeployState.getOrderedValuesYamlList(k8sValuesLocationContentMap);

    assertThat(actualValuesYamlList).isEqualTo(expectedValuesYamlList);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testPriorityOrderOfMultipleValuesYamlFile() {
    Map<K8sValuesLocation, Collection<String>> k8sValuesMap = new HashMap<>();
    k8sValuesMap.put(K8sValuesLocation.Service, Arrays.asList("Service1", "Service2", "Service3"));
    k8sValuesMap.put(ServiceOverride, Arrays.asList("ServiceOverride1", "ServiceOverride2"));
    k8sValuesMap.put(K8sValuesLocation.EnvironmentGlobal, Arrays.asList("EnvironmentGlobal1", "EnvironmentGlobal2"));
    k8sValuesMap.put(K8sValuesLocation.Environment, Arrays.asList("Environment1", "Environment2"));

    List<String> orderedValuesYamlList = helmDeployState.getOrderedValuesYamlList(k8sValuesMap);

    assertThat(orderedValuesYamlList)
        .containsExactly("Service1", "Service2", "Service3", "ServiceOverride1", "ServiceOverride2",
            "EnvironmentGlobal1", "EnvironmentGlobal2", "Environment1", "Environment2");
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void testIsRollBackNotNeeded() {
    assertThat(isInitialRollback()).isTrue();
    assertThat(isNotInitialRollback()).isFalse();
  }

  private boolean isInitialRollback() {
    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance()
            .addContextElement(HelmDeployContextElement.builder().previousReleaseRevision(0).build())
            .build();
    ExecutionContext context = new ExecutionContextImpl(stateExecutionInstance);

    helmDeployState.setStateType(StateType.HELM_ROLLBACK.name());

    return helmDeployState.isRollBackNotNeeded(context);
  }

  private boolean isNotInitialRollback() {
    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance()
            .addContextElement(HelmDeployContextElement.builder().previousReleaseRevision(1).build())
            .build();
    ExecutionContext context = new ExecutionContextImpl(stateExecutionInstance);

    helmDeployState.setStateType(StateType.HELM_ROLLBACK.name());

    return helmDeployState.isRollBackNotNeeded(context);
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void testInitialRollbackNotNeeded() {
    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance()
            .addContextElement(HelmDeployContextElement.builder().previousReleaseRevision(0).build())
            .build();
    ExecutionContext context = new ExecutionContextImpl(stateExecutionInstance);
    HelmDeployStateExecutionData stateExecutionData = HelmDeployStateExecutionData.builder().build();

    doReturn(true).when(logService).batchedSaveCommandUnitLogs(any(), any(), any());

    ExecutionResponse executionResponse =
        helmDeployState.initialRollbackNotNeeded(context, ACTIVITY_ID, stateExecutionData);

    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getStateExecutionData()).isEqualTo(stateExecutionData);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteHelmTaskWithNoInitialRollbackNeeded() throws InterruptedException {
    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance()
            .addContextElement(workflowStandardParams)
            .addContextElement(phaseElement)
            .addContextElement(
                HelmDeployContextElement.builder().releaseName("release").previousReleaseRevision(0).build())
            .build();
    HelmDeployState spyHelmDeployState = spy(helmDeployState);
    on(context).set("stateExecutionInstance", stateExecutionInstance);

    spyHelmDeployState.setStateType(StateType.HELM_ROLLBACK.name());

    doReturn(true).when(logService).batchedSaveCommandUnitLogs(any(), any(), any());
    doReturn(Activity.builder().uuid(ACTIVITY_ID).build())
        .when(spyHelmDeployState)
        .createActivity(eq(context), anyListOf(CommandUnit.class));

    spyHelmDeployState.executeInternal(context);
    verify(spyHelmDeployState, times(1)).isRollBackNotNeeded(context);
    verify(spyHelmDeployState, times(1))
        .initialRollbackNotNeeded(eq(context), eq(ACTIVITY_ID), any(HelmDeployStateExecutionData.class));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testTagsInExecuteGitTask() {
    context.pushContextElement(HelmDeployContextElement.builder()
                                   .releaseName(HELM_RELEASE_NAME_PREFIX)
                                   .commandFlags(COMMAND_FLAGS)
                                   .newReleaseRevision(2)
                                   .previousReleaseRevision(1)
                                   .build());

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    appManifestMap.put(K8sValuesLocation.Environment,
        ApplicationManifest.builder()
            .storeType(StoreType.Remote)
            .gitFileConfig(GitFileConfig.builder().build())
            .build());
    when(applicationManifestUtils.getApplicationManifests(context, AppManifestKind.VALUES)).thenReturn(appManifestMap);
    when(applicationManifestUtils.isValuesInGit(appManifestMap)).thenReturn(true);
    when(applicationManifestUtils.createGitFetchFilesTaskParams(context, app, appManifestMap))
        .thenReturn(GitFetchFilesTaskParams.builder().isBindTaskFeatureSet(true).build());
    when(k8sStateHelper.fetchTagsFromK8sCloudProvider(any())).thenReturn(Arrays.asList("delegateName"));

    ExecutionResponse executionResponse = helmDeployState.execute(context);

    HelmDeployStateExecutionData helmDeployStateExecutionData =
        (HelmDeployStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(helmDeployStateExecutionData.getCurrentTaskType()).isEqualTo(TaskType.GIT_COMMAND);

    verifyDelegateNameInTags();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteGitSyncWithPopulateGitFilePathList() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = ImmutableMap.of(ServiceOverride,
        ApplicationManifest.builder()
            .storeType(StoreType.Remote)
            .gitFileConfig(GitFileConfig.builder().build())
            .build());
    when(applicationManifestUtils.getApplicationManifests(context, AppManifestKind.VALUES)).thenReturn(appManifestMap);
    when(applicationManifestUtils.isValuesInGit(appManifestMap)).thenReturn(true);
    when(applicationManifestUtils.createGitFetchFilesTaskParams(context, app, appManifestMap))
        .thenReturn(GitFetchFilesTaskParams.builder().isBindTaskFeatureSet(true).build());

    ExecutionResponse executionResponse = helmDeployState.execute(context);
    verify(applicationManifestUtils, times(1)).populateRemoteGitConfigFilePathList(context, appManifestMap);
    HelmDeployStateExecutionData helmDeployStateExecutionData =
        (HelmDeployStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(helmDeployStateExecutionData.getCurrentTaskType()).isEqualTo(TaskType.GIT_COMMAND);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testTimeout() {
    HelmDeployState state = new HelmDeployState("helm");

    assertThat(state.getTimeoutMillis()).isEqualTo(null);

    state.setSteadyStateTimeout(5);
    assertThat(state.getTimeoutMillis()).isEqualTo(300000);

    state.setSteadyStateTimeout(Integer.MAX_VALUE);
    assertThat(state.getTimeoutMillis()).isNull();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testTagsInExecuteHelmValuesFetchTask() {
    context.pushContextElement(HelmDeployContextElement.builder()
                                   .releaseName(HELM_RELEASE_NAME_PREFIX)
                                   .commandFlags(COMMAND_FLAGS)
                                   .newReleaseRevision(2)
                                   .previousReleaseRevision(1)
                                   .build());

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    appManifestMap.put(K8sValuesLocation.Environment,
        ApplicationManifest.builder()
            .storeType(StoreType.Remote)
            .helmChartConfig(HelmChartConfig.builder().build())
            .build());

    when(featureFlagService.isEnabled(FeatureName.BIND_FETCH_FILES_TASK_TO_DELEGATE, context.getAccountId()))
        .thenReturn(true);
    when(k8sStateHelper.fetchTagsFromK8sCloudProvider(any())).thenReturn(Arrays.asList("delegateName"));
    when(applicationManifestUtils.getApplicationManifests(context, AppManifestKind.VALUES)).thenReturn(appManifestMap);
    when(applicationManifestUtils.isValuesInHelmChartRepo(context)).thenReturn(true);

    ExecutionResponse executionResponse = helmDeployState.execute(context);

    HelmDeployStateExecutionData helmDeployStateExecutionData =
        (HelmDeployStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(helmDeployStateExecutionData.getCurrentTaskType()).isEqualTo(TaskType.HELM_VALUES_FETCH);

    verifyDelegateNameInTags();
  }

  private void verifyDelegateNameInTags() {
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask.getTags()).isEqualTo(Arrays.asList("delegateName"));
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getHelmCommandRequestTimeoutValue() {
    HelmInstallCommandRequest commandRequest;

    helmDeployState.setSteadyStateTimeout(0);
    commandRequest = getHelmRollbackCommandRequest(helmDeployState);
    assertThat(commandRequest.getTimeoutInMillis()).isEqualTo(600000);

    helmDeployState.setSteadyStateTimeout(5);
    commandRequest = getHelmRollbackCommandRequest(helmDeployState);
    assertThat(commandRequest.getTimeoutInMillis()).isEqualTo(300000);

    helmDeployState.setSteadyStateTimeout(Integer.MAX_VALUE);
    commandRequest = getHelmRollbackCommandRequest(helmDeployState);
    assertThat(commandRequest.getTimeoutInMillis()).isEqualTo(600000);
  }

  private HelmInstallCommandRequest getHelmRollbackCommandRequest(HelmDeployState helmRollbackState) {
    return (HelmInstallCommandRequest) helmRollbackState.getHelmCommandRequest(context,
        HelmChartSpecification.builder().build(), ContainerServiceParams.builder().build(), "release-name",
        WingsTestConstants.ACCOUNT_ID, WingsTestConstants.APP_ID, WingsTestConstants.ACTIVITY_ID,
        ImageDetails.builder().build(), "repo", GitConfig.builder().build(), Collections.emptyList(), null,
        K8sDelegateManifestConfig.builder().build(), Collections.emptyMap(), HelmVersion.V3);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteHelmValuesFetchTaskWithHelmChartServiceManifest() {
    Map<K8sValuesLocation, ApplicationManifest> helmOverrideManifestMap = new HashMap<>();
    helmOverrideManifestMap.put(
        K8sValuesLocation.EnvironmentGlobal, ApplicationManifest.builder().storeType(StoreType.Local).build());

    ApplicationManifest serviceHelmChartManifest =
        ApplicationManifest.builder()
            .storeType(StoreType.HelmChartRepo)
            .helmChartConfig(HelmChartConfig.builder().chartName("helm-chart").build())
            .build();

    doReturn(serviceHelmChartManifest).when(applicationManifestUtils).getApplicationManifestForService(context);
    when(helmChartConfigHelperService.getHelmChartConfigTaskParams(context, serviceHelmChartManifest))
        .thenReturn(HelmChartConfigParams.builder().repoName("repoName").build());

    helmDeployState.executeHelmValuesFetchTask(context, ACTIVITY_ID, helmOverrideManifestMap);

    ArgumentCaptor<DelegateTask> delegateTaskCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(applicationManifestUtils, times(1))
        .applyK8sValuesLocationBasedHelmChartOverride(
            serviceHelmChartManifest, helmOverrideManifestMap, K8sValuesLocation.EnvironmentGlobal);
    verify(delegateService, times(1)).queueTask(delegateTaskCaptor.capture());

    DelegateTask task = delegateTaskCaptor.getValue();
    HelmValuesFetchTaskParameters taskParameters = (HelmValuesFetchTaskParameters) task.getData().getParameters()[0];
    assertThat(taskParameters.getHelmChartConfigTaskParams()).isNotNull();
    assertThat(taskParameters.getHelmChartConfigTaskParams().getRepoName()).isEqualTo("repoName");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleAsyncInternalForGitTask() throws InterruptedException {
    HelmDeployState spyDeployState = spy(helmDeployState);
    GitCommandExecutionResponse gitCommandExecutionResponse =
        GitCommandExecutionResponse.builder().gitCommandStatus(GitCommandStatus.SUCCESS).build();
    HelmDeployStateExecutionData helmStateExecutionData =
        (HelmDeployStateExecutionData) context.getStateExecutionData();

    Map<K8sValuesLocation, ApplicationManifest> helmOverrideManifestMap = new HashMap<>();
    Map<K8sValuesLocation, ApplicationManifest> applicationManifestMap = new HashMap<>();
    String activityId = "activityId";
    helmStateExecutionData.setActivityId(activityId);
    helmStateExecutionData.setCurrentTaskType(TaskType.GIT_COMMAND);
    helmStateExecutionData.setAppManifestMap(applicationManifestMap);
    ExecutionResponse helmTaskExecutionResponse = ExecutionResponse.builder().build();
    Map<String, ResponseData> response = new HashMap<>();
    response.put(activityId, gitCommandExecutionResponse);

    doReturn(helmOverrideManifestMap)
        .when(applicationManifestUtils)
        .getOverrideApplicationManifests(context, HELM_CHART_OVERRIDE);
    doReturn(helmTaskExecutionResponse)
        .when(spyDeployState)
        .executeHelmTask(context, activityId, applicationManifestMap, helmOverrideManifestMap);
    ExecutionResponse executionResponse = spyDeployState.handleAsyncInternal(context, response);

    assertThat(executionResponse).isSameAs(helmTaskExecutionResponse);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testUpdateHelmExecutionSummaryNegative() {
    HelmExecutionSummary executionSummary = spy(HelmExecutionSummary.builder().build());
    WorkflowExecution workflowExecution =
        spy(WorkflowExecution.builder().appId(APP_ID).helmExecutionSummary(executionSummary).build());
    doReturn(workflowExecution)
        .when(workflowExecutionService)
        .getWorkflowExecution(context.getAppId(), context.getWorkflowExecutionId());

    testUpdateHelmExecutionSummaryWithReleaseHistoryCommandResponse(executionSummary);
    testUpdateHelmExecutionSummaryWithoutHelmDeployStateType(executionSummary);
    testUpdateHelmExecutionSummaryWithoutHelmChartInfo(executionSummary);
    testUpdateHelmExecutionSummaryWithException();
  }

  private void testUpdateHelmExecutionSummaryWithReleaseHistoryCommandResponse(HelmExecutionSummary executionSummary) {
    reset(executionSummary);
    HelmReleaseHistoryCommandResponse commandResponse = HelmReleaseHistoryCommandResponse.builder().build();
    helmDeployState.updateHelmExecutionSummary(context, commandResponse);
    verifyZeroInteractions(executionSummary);
  }

  private void testUpdateHelmExecutionSummaryWithoutHelmDeployStateType(HelmExecutionSummary executionSummary) {
    reset(executionSummary);
    HelmInstallCommandResponse commandResponse = HelmInstallCommandResponse.builder().build();
    helmDeployState.setStateType("NON_HELM_DEPLOY");
    helmDeployState.updateHelmExecutionSummary(context, commandResponse);
    verifyZeroInteractions(executionSummary);
  }

  private void testUpdateHelmExecutionSummaryWithoutHelmChartInfo(HelmExecutionSummary executionSummary) {
    reset(executionSummary);
    HelmInstallCommandResponse commandResponse = HelmInstallCommandResponse.builder().helmChartInfo(null).build();
    helmDeployState.setStateType(HELM_DEPLOY.name());
    helmDeployState.updateHelmExecutionSummary(context, commandResponse);
    verifyZeroInteractions(executionSummary);
  }

  private void testUpdateHelmExecutionSummaryWithException() {
    HelmInstallCommandResponse commandResponse = HelmInstallCommandResponse.builder().build();
    doThrow(new RuntimeException("Something went wrong"))
        .when(workflowExecutionService)
        .getWorkflowExecution(context.getAppId(), context.getWorkflowExecutionId());
    // Test exception hasn't been propagated upper
    helmDeployState.updateHelmExecutionSummary(context, commandResponse);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testUpdateHelmExecutionSummary() {
    HelmExecutionSummary executionSummary = spy(HelmExecutionSummary.builder().build());
    WorkflowExecution workflowExecution =
        spy(WorkflowExecution.builder().appId(APP_ID).helmExecutionSummary(executionSummary).build());
    doReturn(workflowExecution)
        .when(workflowExecutionService)
        .getWorkflowExecution(context.getAppId(), context.getWorkflowExecutionId());

    testUpdateHelmExecutionSummaryWithHelmChartInfoDetails(executionSummary);
    testUpdateHelmExecutionSummaryWithContainerInfoList(executionSummary);
  }

  private void testUpdateHelmExecutionSummaryWithHelmChartInfoDetails(HelmExecutionSummary executionSummary) {
    HelmChartInfo helmChartInfo = HelmChartInfo.builder().build();
    HelmChartInfo summaryHelmChartInfo = spy(HelmChartInfo.builder().build());
    HelmInstallCommandResponse commandResponse =
        HelmInstallCommandResponse.builder().helmChartInfo(helmChartInfo).build();
    helmDeployState.setStateType(HELM_DEPLOY.name());

    // empty helm chart info
    reset(executionSummary);
    helmDeployState.updateHelmExecutionSummary(context, commandResponse);
    verify(executionSummary, times(1)).getHelmChartInfo();
    verify(executionSummary, times(1)).setHelmChartInfo(HelmChartInfo.builder().build());
    verifyNoMoreInteractions(executionSummary);

    // with helm chart values
    reset(executionSummary);
    doReturn(summaryHelmChartInfo).when(executionSummary).getHelmChartInfo();
    helmChartInfo.setName("helm-chart");
    helmChartInfo.setVersion("helm-version");
    helmChartInfo.setRepoUrl("helm-repo-url");
    helmDeployState.updateHelmExecutionSummary(context, commandResponse);
    verify(executionSummary, never()).setHelmChartInfo(HelmChartInfo.builder().build());
    verify(summaryHelmChartInfo, times(1)).setName("helm-chart");
    verify(summaryHelmChartInfo, times(1)).setVersion("helm-version");
    verify(summaryHelmChartInfo, times(1)).setRepoUrl("helm-repo-url");
    verifyNoMoreInteractions(summaryHelmChartInfo);
  }

  private void testUpdateHelmExecutionSummaryWithContainerInfoList(HelmExecutionSummary executionSummary) {
    reset(executionSummary);
    HelmInstallCommandResponse commandResponse =
        HelmInstallCommandResponse.builder()
            .containerInfoList(Arrays.asList(
                ContainerInfo.builder().podName("p1").build(), ContainerInfo.builder().podName("p2").build()))
            .helmChartInfo(HelmChartInfo.builder().build())
            .build();
    helmDeployState.setStateType(HELM_DEPLOY.name());
    helmDeployState.updateHelmExecutionSummary(context, commandResponse);
    ArgumentCaptor<List<ContainerInfo>> containerInfoListCaptor = ArgumentCaptor.forClass((Class) List.class);
    verify(executionSummary, times(1)).setContainerInfoList(containerInfoListCaptor.capture());
    List<ContainerInfo> containerInfoList = containerInfoListCaptor.getValue();
    assertThat(containerInfoList.stream().map(ContainerInfo::getPodName)).containsExactlyInAnyOrder("p1", "p2");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteHelmTaskWithInvalidChartSpec() {
    HelmChartSpecification helmChartSpec = HelmChartSpecification.builder().build();
    doReturn(null).when(serviceResourceService).getHelmChartSpecification(anyString(), anyString());

    // Missing HelmChartSpecification
    assertThatThrownBy(() -> helmDeployState.executeHelmTask(context, ACTIVITY_ID, emptyMap(), emptyMap()))
        .hasMessageContaining("Invalid chart specification");

    doReturn(helmChartSpec).when(serviceResourceService).getHelmChartSpecification(anyString(), anyString());

    // Empty chart name and missing chart url
    helmChartSpec.setChartName("");
    helmChartSpec.setChartUrl(null);
    assertThatThrownBy(() -> helmDeployState.executeHelmTask(context, ACTIVITY_ID, emptyMap(), emptyMap()))
        .hasMessageContaining("Invalid chart specification");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteHelmTaskWithTemplatizedGitConfig() throws Exception {
    HelmChartSpecification helmChartSpec = HelmChartSpecification.builder().chartName("name").build();
    List<TemplateExpression> expressions = singletonList(TemplateExpression.builder().fieldName("connectorId").build());
    SettingAttribute attribute = aSettingAttribute().build();
    helmDeployState.setGitFileConfig(GitFileConfig.builder().build());
    helmDeployState.setTemplateExpressions(expressions);

    doReturn(helmChartSpec).when(serviceResourceService).getHelmChartSpecification(anyString(), anyString());
    doReturn(expressions.get(0)).when(templateExpressionProcessor).getTemplateExpression(expressions, "connectorId");
    doReturn(attribute)
        .when(templateExpressionProcessor)
        .resolveSettingAttributeByNameOrId(context, expressions.get(0), SettingVariableTypes.GIT);

    // Invalid connectorId
    attribute.setValue(mock(SettingValue.class)); // Check is !(settingValue instanceof GitConfig)
    assertThatThrownBy(() -> helmDeployState.executeHelmTask(context, ACTIVITY_ID, emptyMap(), emptyMap()))
        .hasMessageContaining("Git connector not found");

    // Valid connectorId
    attribute.setValue(GitConfig.builder().build());
    helmDeployState.executeHelmTask(context, ACTIVITY_ID, emptyMap(), emptyMap());
    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(1)).queueTask(taskCaptor.capture());

    HelmInstallCommandRequest request = (HelmInstallCommandRequest) taskCaptor.getValue().getData().getParameters()[0];
    assertThat(request.getGitConfig()).isEqualTo(attribute.getValue());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetValuesYamlOverridesWithoutImageDetails() {
    String yamlFileContent = "tag: ${DOCKER_IMAGE_TAG}\nimage: ${DOCKER_IMAGE_NAME}";
    //    ImageDetails imageDetails = ImageDetails.builder().tag("Tag").name("Image").domainName("domain").build();
    doAnswer(invocation -> {
      Map values = invocation.getArgumentAt(1, Map.class);
      values.put(ServiceOverride, singletonList(yamlFileContent));
      return null;
    })
        .when(applicationManifestUtils)
        .populateValuesFilesFromAppManifest(anyMap(), anyMap());
    ContainerServiceParams serviceParams = ContainerServiceParams.builder().build();

    List<String> files = helmDeployState.getValuesYamlOverrides(context, serviceParams, null, emptyMap());
    assertThat(files.get(0)).isEqualTo("tag: ${DOCKER_IMAGE_TAG}\nimage: ${DOCKER_IMAGE_NAME}");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetValuesYamlOverridesWithImageDetails() {
    String yamlFileContent = "tag: ${DOCKER_IMAGE_TAG}\nimage: ${DOCKER_IMAGE_NAME}";
    ImageDetails imageDetails = ImageDetails.builder().tag("Tag").name("Image").domainName("domain").build();
    doAnswer(invocation -> {
      Map values = invocation.getArgumentAt(1, Map.class);
      values.put(ServiceOverride, singletonList(yamlFileContent));
      return null;
    })
        .when(applicationManifestUtils)
        .populateValuesFilesFromAppManifest(anyMap(), anyMap());
    ContainerServiceParams serviceParams = ContainerServiceParams.builder().build();

    List<String> files = helmDeployState.getValuesYamlOverrides(context, serviceParams, imageDetails, emptyMap());
    assertThat(files.get(0)).isEqualTo("tag: Tag\nimage: domain/Image");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetValuesYamlOverridesWithDomainAlreadyInFileContent() {
    String yamlFileContent = "tag: ${DOCKER_IMAGE_TAG}\nimage: domain/${DOCKER_IMAGE_NAME}";
    ImageDetails imageDetails = ImageDetails.builder().tag("Tag").name("Image").domainName("domain").build();
    doAnswer(invocation -> {
      Map values = invocation.getArgumentAt(1, Map.class);
      values.put(ServiceOverride, singletonList(yamlFileContent));
      return null;
    })
        .when(applicationManifestUtils)
        .populateValuesFilesFromAppManifest(anyMap(), anyMap());
    ContainerServiceParams serviceParams = ContainerServiceParams.builder().build();

    List<String> files = helmDeployState.getValuesYamlOverrides(context, serviceParams, imageDetails, emptyMap());
    assertThat(files.get(0)).isEqualTo("tag: Tag\nimage: domain/Image");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetPreviousReleaseVersionFromInvalidResponse() throws Exception {
    GitConfig gitConfig = GitConfig.builder().build();
    KubernetesClusterConfig k8sClusterConfig = KubernetesClusterConfig.builder().build();
    doReturn(new ResponseData() {}).when(delegateService).executeTask(any(DelegateTask.class));

    String helmV2ExpectedMessage = "Make sure that the helm client and tiller is installed";
    testGetPreviousReleaseVersionInvalidResponse(HelmVersion.V2, null, mock(SettingValue.class), helmV2ExpectedMessage);
    String helmV3ExpectedMessage = "Make sure Helm 3 is installed";
    testGetPreviousReleaseVersionInvalidResponse(HelmVersion.V3, null, k8sClusterConfig, helmV3ExpectedMessage);
    String gitConfigExpectedMessage = "and delegate has git connectivity";
    testGetPreviousReleaseVersionInvalidResponse(HelmVersion.V3, gitConfig, k8sClusterConfig, gitConfigExpectedMessage);
    String useKubernetesDelegateConfigExpectedMessage = "and correct delegate name is selected in the cloud provider";
    k8sClusterConfig.setUseKubernetesDelegate(true);
    testGetPreviousReleaseVersionInvalidResponse(
        HelmVersion.V3, gitConfig, k8sClusterConfig, useKubernetesDelegateConfigExpectedMessage);
  }

  private void testGetPreviousReleaseVersionInvalidResponse(
      HelmVersion version, GitConfig gitConfig, SettingValue settingValue, String expectedMessage) throws Exception {
    ContainerServiceParams params =
        ContainerServiceParams.builder().settingAttribute(aSettingAttribute().withValue(settingValue).build()).build();
    assertThatThrownBy(()
                           -> helmDeployState.getPreviousReleaseVersion(context, app, RELEASE_NAME, params, gitConfig,
                               emptyList(), "", version, 0, HelmDeployStateExecutionData.builder()))
        .hasMessageContaining(expectedMessage);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetSteadyStateTimeout() {
    helmDeployState.setSteadyStateTimeout(999);
    assertThat(helmDeployState.getSteadyStateTimeout()).isEqualTo(999);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCreateActivityWithBuildOrchestrationWorkflowType() {
    doAnswer(invocation -> invocation.getArgumentAt(0, Activity.class)).when(activityService).save(any(Activity.class));
    on(stateExecutionInstance).set("orchestrationWorkflowType", OrchestrationWorkflowType.BUILD);
    Activity activity = helmDeployState.createActivity(context, emptyList());
    assertThat(activity.getEnvironmentId()).isEqualTo(GLOBAL_ENV_ID);
    assertThat(activity.getEnvironmentName()).isEqualTo(GLOBAL_ENV_ID);
    assertThat(activity.getEnvironmentType()).isEqualTo(EnvironmentType.ALL);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCreateActivityWithInstanceElementDetails() {
    doAnswer(invocation -> invocation.getArgumentAt(0, Activity.class)).when(activityService).save(any(Activity.class));
    ServiceTemplateElement serviceTemplateElement =
        ServiceTemplateElement.Builder.aServiceTemplateElement()
            .withUuid("templateId")
            .withName("serviceTemplateElement")
            .withServiceElement(ServiceElement.builder().uuid("serviceId").name("serviceName").build())
            .build();
    InstanceElement instanceElement = Builder.anInstanceElement()
                                          .uuid("instanceId")
                                          .serviceTemplateElement(serviceTemplateElement)
                                          .host(HostElement.builder().hostName(HOST_NAME).build())
                                          .build();
    stateExecutionInstance.getContextElements().add(instanceElement);
    Activity activity = helmDeployState.createActivity(context, emptyList());
    assertThat(activity.getServiceTemplateId()).isEqualTo("templateId");
    assertThat(activity.getServiceTemplateName()).isEqualTo("serviceTemplateElement");
    assertThat(activity.getServiceId()).isEqualTo("serviceId");
    assertThat(activity.getServiceName()).isEqualTo("serviceName");
    assertThat(activity.getServiceInstanceId()).isEqualTo("instanceId");
    assertThat(activity.getHostName()).isEqualTo(HOST_NAME);
  }
}
