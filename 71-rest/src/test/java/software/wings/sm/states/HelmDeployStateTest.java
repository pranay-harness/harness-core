package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.service.intfc.ServiceTemplateService.EncryptedFieldComputeMode.OBTAIN_VALUE;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
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
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.STATE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import com.google.common.collect.Lists;

import io.harness.beans.DelegateTask;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.VariableResolverTracker;
import io.harness.rule.Owner;
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
import software.wings.api.InstanceElement.Builder;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.InfraMappingSweepingOutput;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.TaskType;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitCommandExecutionResponse.GitCommandStatus;
import software.wings.common.InfrastructureConstants;
import software.wings.common.VariableProcessor;
import software.wings.delegatetasks.RemoteMethodReturnValueData;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.helm.HelmCommandExecutionResponse;
import software.wings.helpers.ext.helm.HelmHelper;
import software.wings.helpers.ext.helm.request.HelmCommandRequest.HelmCommandType;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;
import software.wings.helpers.ext.helm.response.HelmReleaseHistoryCommandResponse;
import software.wings.helpers.ext.helm.response.HelmValuesFetchTaskResponse;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.GitConfigHelperService;
import software.wings.service.impl.GitFileConfigHelperService;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.impl.servicetemplates.ServiceTemplateHelper;
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
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.ApplicationManifestUtils;

import java.util.Arrays;
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
  @Mock private FeatureFlagService featureFlagService;
  @Mock private ServiceTemplateHelper serviceTemplateHelper;
  @Mock private SweepingOutputService sweepingOutputService;
  @Mock private HelmHelper helmHelper;
  @Mock private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private GitFileConfigHelperService gitFileConfigHelperService;

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
    when(featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, ACCOUNT_ID)).thenReturn(false);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecute() throws InterruptedException {
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

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();

    HelmInstallCommandRequest helmInstallCommandRequest =
        (HelmInstallCommandRequest) delegateTask.getData().getParameters()[0];
    assertThat(helmInstallCommandRequest.getHelmCommandType()).isEqualTo(HelmCommandType.INSTALL);
    assertThat(helmInstallCommandRequest.getReleaseName()).isEqualTo(HELM_RELEASE_NAME_PREFIX);
    assertThat(helmInstallCommandRequest.getRepoName()).isEqualTo("app-name-service-name");
    assertThat(helmInstallCommandRequest.getCommandFlags()).isNull();

    verify(delegateService).executeTask(any());
    verify(gitConfigHelperService, times(1)).renderGitConfig(any(), any());
    verify(gitFileConfigHelperService, times(1)).renderGitFileConfig(any(), any());
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
    ExecutionResponse executionResponse = helmDeployState.execute(context);
    HelmDeployStateExecutionData helmDeployStateExecutionData =
        (HelmDeployStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(helmDeployStateExecutionData.getCommandFlags()).isEqualTo(COMMAND_FLAGS);

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();

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
    verify(applicationManifestUtils, times(0)).getValuesFilesFromGitFetchFilesResponse(any());

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
    verify(applicationManifestUtils, times(0)).getValuesFilesFromGitFetchFilesResponse(any());
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
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
    Map<K8sValuesLocation, String> valuesFiles = new HashMap<>();
    valuesFiles.put(K8sValuesLocation.Service, valuesFile);
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
    Map<K8sValuesLocation, String> k8sValuesLocationContentMap = new HashMap<>();
    k8sValuesLocationContentMap.put(K8sValuesLocation.ServiceOverride, "ServiceOverride");
    k8sValuesLocationContentMap.put(K8sValuesLocation.Service, "Service");
    k8sValuesLocationContentMap.put(K8sValuesLocation.Environment, "Environment");
    k8sValuesLocationContentMap.put(K8sValuesLocation.EnvironmentGlobal, "EnvironmentGlobal");
    List<String> expectedValuesYamlList =
        Arrays.asList("Service", "ServiceOverride", "EnvironmentGlobal", "Environment");

    List<String> actualValuesYamlList = helmDeployState.getOrderedValuesYamlList(k8sValuesLocationContentMap);

    assertThat(actualValuesYamlList).isEqualTo(expectedValuesYamlList);
  }
}
