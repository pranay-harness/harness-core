package software.wings.sm.states.k8s;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.k8s.manifest.ManifestHelper.values_filename;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.appmanifest.AppManifestKind.K8S_MANIFEST;
import static software.wings.beans.appmanifest.StoreType.HelmChartRepo;
import static software.wings.beans.appmanifest.StoreType.HelmSourceRepo;
import static software.wings.beans.appmanifest.StoreType.KustomizeSourceRepo;
import static software.wings.beans.appmanifest.StoreType.Local;
import static software.wings.beans.appmanifest.StoreType.Remote;
import static software.wings.delegatetasks.GitFetchFilesTask.GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT;
import static software.wings.infra.InfraDefinitionTestConstants.INFRA_DEFINITION_NAME;
import static software.wings.settings.SettingVariableTypes.GCP;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.StepType.K8S_SCALE;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.sm.states.k8s.K8sScale.K8S_SCALE_COMMAND_NAME;
import static software.wings.sm.states.k8s.K8sTestConstants.VALUES_YAML_WITH_ARTIFACT_REFERENCE;
import static software.wings.sm.states.k8s.K8sTestConstants.VALUES_YAML_WITH_COMMENTED_ARTIFACT_REFERENCE;
import static software.wings.sm.states.k8s.K8sTestConstants.VALUES_YAML_WITH_NO_ARTIFACT_REFERENCE;
import static software.wings.sm.states.pcf.PcfStateTestHelper.PHASE_NAME;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_NAME;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_DEFINITION_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.STATE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.ResponseData;
import io.harness.deployment.InstanceDetails;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.K8sPodSyncException;
import io.harness.exception.WingsException;
import io.harness.expression.VariableResolverTracker;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.k8s.model.HelmVersion;
import io.harness.k8s.model.K8sPod;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.instancedetails.InstanceInfoVariables;
import software.wings.api.k8s.K8sElement;
import software.wings.api.k8s.K8sHelmDeploymentElement;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Account;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.K8sPodInfo;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.common.VariableProcessor;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.helm.response.HelmValuesFetchTaskResponse;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.k8s.response.K8sInstanceSyncResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.helpers.ext.kustomize.KustomizeHelper;
import software.wings.helpers.ext.openshift.OpenShiftManagerService;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.EventEmitter;
import software.wings.service.impl.GitConfigHelperService;
import software.wings.service.impl.GitFileConfigHelperService;
import software.wings.service.impl.HelmChartConfigHelperService;
import software.wings.service.impl.servicetemplates.ServiceTemplateHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.ApplicationManifestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class K8sStateHelperTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Mock private ActivityService activityService;
  @Mock private AppService appService;
  @Mock private EnvironmentService environmentService;
  @Mock private EventEmitter eventEmitter;
  @Mock private ApplicationManifestUtils applicationManifestUtils;
  @Mock private GitFileConfigHelperService gitFileConfigHelperService;
  @Mock private SettingsService settingsService;
  @Mock private SecretManager secretManager;
  @Mock private HelmChartConfigHelperService helmChartConfigHelperService;
  @Mock private KustomizeHelper kustomizeHelper;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private DelegateService delegateService;
  @Mock private ServiceTemplateHelper serviceTemplateHelper;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private ArtifactService artifactService;
  @Mock private SweepingOutputService sweepingOutputService;
  @Mock private ManagerExpressionEvaluator evaluator;
  @Mock private VariableProcessor variableProcessor;
  @Mock private SubdomainUrlHelperIntfc subdomainUrlHelper;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private AccountService accountService;
  @Mock private InfrastructureDefinitionService infrastructureDefinitionService;
  @Mock private OpenShiftManagerService openShiftManagerService;
  @Mock private ContainerDeploymentManagerHelper containerDeploymentManagerHelper;
  @Mock private InstanceService instanceService;
  @Mock private GitConfigHelperService gitConfigHelperService;

  @Inject @InjectMocks private K8sStateHelper k8sStateHelper;

  @Inject KryoSerializer kryoSerializer;

  private static final String APPLICATION_MANIFEST_ID = "AppManifestId";

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

  private ExecutionContextImpl context;
  private StateExecutionInstance stateExecutionInstance =
      aStateExecutionInstance()
          .displayName(STATE_NAME)
          .addContextElement(workflowStandardParams)
          .addContextElement(phaseElement)
          .addStateExecutionData(K8sStateExecutionData.builder().build())
          .build();
  private Application application;

  @Before
  public void setup() {
    context = new ExecutionContextImpl(stateExecutionInstance);
    on(workflowStandardParams).set("appService", appService);
    on(workflowStandardParams).set("environmentService", environmentService);
    on(context).set("infrastructureMappingService", infrastructureMappingService);
    on(context).set("serviceResourceService", serviceResourceService);
    on(context).set("artifactService", artifactService);
    on(context).set("variableProcessor", variableProcessor);
    on(context).set("evaluator", evaluator);
    on(context).set("featureFlagService", featureFlagService);
    on(context).set("stateExecutionInstance", stateExecutionInstance);
    on(context).set("sweepingOutputService", sweepingOutputService);
    on(workflowStandardParams).set("subdomainUrlHelper", subdomainUrlHelper);
    when(subdomainUrlHelper.getPortalBaseUrl(any())).thenReturn("baseUrl");

    when(accountService.getAccountWithDefaults(ACCOUNT_ID))
        .thenReturn(Account.Builder.anAccount().withUuid(ACCOUNT_ID).withAccountName(ACCOUNT_NAME).build());
    application = anApplication().appId(APP_ID).name(APP_NAME).accountId(ACCOUNT_ID).uuid(APP_ID).build();
    when(appService.getApplicationWithDefaults(APP_ID)).thenReturn(application);
    when(appService.get(any())).thenReturn(application);
    when(environmentService.get(APP_ID, ENV_ID, false))
        .thenReturn(Environment.Builder.anEnvironment()
                        .appId(APP_ID)
                        .environmentType(EnvironmentType.PROD)
                        .uuid(ENV_ID)
                        .build());
    when(evaluator.substitute(anyString(), anyMap(), any(VariableResolverTracker.class), anyString()))
        .thenAnswer(i -> i.getArguments()[0]);
    doReturn(K8sClusterConfig.builder().build())
        .when(containerDeploymentManagerHelper)
        .getK8sClusterConfig(any(), any());
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testCreateK8sActivity() {
    List<CommandUnit> commandUnits = new ArrayList<>();
    commandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Init));
    commandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Scale));

    k8sStateHelper.createK8sActivity(context, K8S_SCALE_COMMAND_NAME, K8S_SCALE.name(), activityService, commandUnits);

    ArgumentCaptor<Activity> activityArgumentCaptor = ArgumentCaptor.forClass(Activity.class);
    verify(activityService, times(1)).save(activityArgumentCaptor.capture());
    Activity activity = activityArgumentCaptor.getValue();
    assertThat(activity.getAppId()).isEqualTo(APP_ID);
    assertThat(activity.getEnvironmentId()).isEqualTo(ENV_ID);
    assertThat(activity.getCommandName()).isEqualTo(K8S_SCALE_COMMAND_NAME);
    assertThat(activity.getCommandType()).isEqualTo(K8S_SCALE.name());
    assertThat(activity.getCommandUnits()).isEqualTo(commandUnits);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCreateDelegateManifestConfig_GitSourceRepo() {
    ExecutionContext context = mock(ExecutionContext.class);

    GitFileConfig gitConfigAtService = GitFileConfig.builder().branch("1").filePath("abc").connectorId("c1").build();
    GitFileConfig gitConfigAtEnvOverride =
        GitFileConfig.builder().branch("2").filePath("def").connectorId("d1").build();

    ApplicationManifest appManifest = ApplicationManifest.builder()
                                          .gitFileConfig(gitConfigAtService)
                                          .kind(AppManifestKind.HELM_CHART_OVERRIDE)
                                          .storeType(StoreType.HelmSourceRepo)
                                          .build();

    ApplicationManifest appManifestOverride = ApplicationManifest.builder()
                                                  .gitFileConfig(gitConfigAtEnvOverride)
                                                  .kind(AppManifestKind.HELM_CHART_OVERRIDE)
                                                  .storeType(StoreType.HelmSourceRepo)
                                                  .build();

    doReturn(appManifest)
        .doReturn(appManifestOverride)
        .when(applicationManifestUtils)
        .getAppManifestByApplyingHelmChartOverride(context);
    when(gitFileConfigHelperService.renderGitFileConfig(any(), any())).thenAnswer(new Answer<GitFileConfig>() {
      @Override
      public GitFileConfig answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        return (GitFileConfig) args[1];
      }
    });

    doReturn(GitConfig.builder().build()).when(settingsService).fetchGitConfigFromConnectorId(anyString());
    doReturn(emptyList()).when(secretManager).getEncryptionDetails(any(), anyString(), any());

    K8sDelegateManifestConfig delegateManifestConfig =
        k8sStateHelper.createDelegateManifestConfig(context, appManifest);
    assertThat(delegateManifestConfig.getGitFileConfig()).isEqualTo(gitConfigAtService);
    assertThat(delegateManifestConfig.getGitFileConfig().getFilePath()).isEqualTo("abc/");
    assertThat(delegateManifestConfig.getGitFileConfig().getConnectorId()).isEqualTo("c1");
    assertThat(delegateManifestConfig.getGitFileConfig().getBranch()).isEqualTo("1");

    delegateManifestConfig = k8sStateHelper.createDelegateManifestConfig(context, appManifest);
    assertThat(delegateManifestConfig.getGitFileConfig()).isEqualTo(gitConfigAtEnvOverride);
    assertThat(delegateManifestConfig.getGitFileConfig().getFilePath()).isEqualTo("def/");
    assertThat(delegateManifestConfig.getGitFileConfig().getConnectorId()).isEqualTo("d1");
    assertThat(delegateManifestConfig.getGitFileConfig().getBranch()).isEqualTo("2");
    verify(gitConfigHelperService, times(2))
        .convertToRepoGitConfig(
            delegateManifestConfig.getGitConfig(), delegateManifestConfig.getGitFileConfig().getRepoName());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCreateDelegateManifestConfig_HelmChartRepo() {
    ExecutionContext context = mock(ExecutionContext.class);

    HelmChartConfig helmChartConfigAtService =
        HelmChartConfig.builder().chartName("n1").chartVersion("v1").connectorId("c1").build();
    HelmChartConfig helmChartConfigOverride =
        HelmChartConfig.builder().chartName("m1").chartVersion("w1").connectorId("d1").build();

    ApplicationManifest appManifest = ApplicationManifest.builder()
                                          .helmChartConfig(helmChartConfigAtService)
                                          .kind(AppManifestKind.HELM_CHART_OVERRIDE)
                                          .storeType(StoreType.HelmChartRepo)
                                          .build();

    ApplicationManifest appManifestOverride = ApplicationManifest.builder()
                                                  .helmChartConfig(helmChartConfigOverride)
                                                  .kind(AppManifestKind.HELM_CHART_OVERRIDE)
                                                  .storeType(StoreType.HelmChartRepo)
                                                  .build();

    doReturn(appManifest)
        .doReturn(appManifestOverride)
        .when(applicationManifestUtils)
        .getAppManifestByApplyingHelmChartOverride(context);
    when(helmChartConfigHelperService.getHelmChartConfigTaskParams(any(), any()))
        .thenAnswer(new Answer<HelmChartConfigParams>() {
          @Override
          public HelmChartConfigParams answer(InvocationOnMock invocation) throws Throwable {
            Object[] args = invocation.getArguments();
            ApplicationManifest applicationManifest = (ApplicationManifest) args[1];
            HelmChartConfig helmChartConfig = applicationManifest.getHelmChartConfig();
            return HelmChartConfigParams.builder()
                .chartName(helmChartConfig.getChartName())
                .chartVersion(helmChartConfig.getChartVersion())
                .build();
          }
        });
    K8sDelegateManifestConfig delegateManifestConfig =
        k8sStateHelper.createDelegateManifestConfig(context, appManifest);
    assertThat(delegateManifestConfig.getHelmChartConfigParams().getChartName()).isEqualTo("n1");
    assertThat(delegateManifestConfig.getHelmChartConfigParams().getChartVersion()).isEqualTo("v1");

    delegateManifestConfig = k8sStateHelper.createDelegateManifestConfig(context, appManifest);
    assertThat(delegateManifestConfig.getHelmChartConfigParams().getChartName()).isEqualTo("m1");
    assertThat(delegateManifestConfig.getHelmChartConfigParams().getChartVersion()).isEqualTo("w1");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDoManifestsUseArtifact() {
    ApplicationManifest applicationManifest = createApplicationManifest();
    ManifestFile manifestFile = createManifestFile();

    wingsPersistence.save(applicationManifest);
    wingsPersistence.save(manifestFile);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(createGCPInfraMapping());

    // Service K8S_MANIFEST
    boolean result = k8sStateHelper.doManifestsUseArtifact(APP_ID, INFRA_MAPPING_ID);
    assertThat(result).isTrue();

    manifestFile.setFileContent(VALUES_YAML_WITH_COMMENTED_ARTIFACT_REFERENCE);
    wingsPersistence.save(manifestFile);
    result = k8sStateHelper.doManifestsUseArtifact(APP_ID, INFRA_MAPPING_ID);
    assertThat(result).isFalse();

    manifestFile.setFileContent(VALUES_YAML_WITH_NO_ARTIFACT_REFERENCE);
    wingsPersistence.save(manifestFile);
    result = k8sStateHelper.doManifestsUseArtifact(APP_ID, INFRA_MAPPING_ID);
    assertThat(result).isFalse();

    // Service VALUES
    applicationManifest.setKind(AppManifestKind.VALUES);
    wingsPersistence.save(applicationManifest);
    manifestFile.setFileContent(VALUES_YAML_WITH_ARTIFACT_REFERENCE);
    wingsPersistence.save(manifestFile);
    result = k8sStateHelper.doManifestsUseArtifact(APP_ID, INFRA_MAPPING_ID);
    assertThat(result).isTrue();

    manifestFile.setFileContent(VALUES_YAML_WITH_COMMENTED_ARTIFACT_REFERENCE);
    wingsPersistence.save(manifestFile);
    result = k8sStateHelper.doManifestsUseArtifact(APP_ID, INFRA_MAPPING_ID);
    assertThat(result).isFalse();

    manifestFile.setFileContent(VALUES_YAML_WITH_NO_ARTIFACT_REFERENCE);
    wingsPersistence.save(manifestFile);
    result = k8sStateHelper.doManifestsUseArtifact(APP_ID, INFRA_MAPPING_ID);
    assertThat(result).isFalse();

    manifestFile.setFileContent(" ");
    wingsPersistence.save(manifestFile);
    result = k8sStateHelper.doManifestsUseArtifact(APP_ID, INFRA_MAPPING_ID);
    assertThat(result).isFalse();

    // Env VALUES
    applicationManifest.setServiceId(null);
    applicationManifest.setEnvId(ENV_ID);
    applicationManifest.setKind(AppManifestKind.VALUES);
    wingsPersistence.save(applicationManifest);
    manifestFile.setFileContent(VALUES_YAML_WITH_ARTIFACT_REFERENCE);
    wingsPersistence.save(manifestFile);
    result = k8sStateHelper.doManifestsUseArtifact(APP_ID, INFRA_MAPPING_ID);
    assertThat(result).isTrue();

    manifestFile.setFileContent(VALUES_YAML_WITH_COMMENTED_ARTIFACT_REFERENCE);
    wingsPersistence.save(manifestFile);
    result = k8sStateHelper.doManifestsUseArtifact(APP_ID, INFRA_MAPPING_ID);
    assertThat(result).isFalse();

    manifestFile.setFileContent(VALUES_YAML_WITH_NO_ARTIFACT_REFERENCE);
    wingsPersistence.save(manifestFile);
    result = k8sStateHelper.doManifestsUseArtifact(APP_ID, INFRA_MAPPING_ID);
    assertThat(result).isFalse();

    // Env Service VALUES
    applicationManifest.setServiceId(SERVICE_ID);
    wingsPersistence.save(applicationManifest);
    manifestFile.setFileContent(VALUES_YAML_WITH_ARTIFACT_REFERENCE);
    wingsPersistence.save(manifestFile);
    result = k8sStateHelper.doManifestsUseArtifact(APP_ID, INFRA_MAPPING_ID);
    assertThat(result).isTrue();

    manifestFile.setFileContent(VALUES_YAML_WITH_COMMENTED_ARTIFACT_REFERENCE);
    wingsPersistence.save(manifestFile);
    result = k8sStateHelper.doManifestsUseArtifact(APP_ID, INFRA_MAPPING_ID);
    assertThat(result).isFalse();

    manifestFile.setFileContent(VALUES_YAML_WITH_NO_ARTIFACT_REFERENCE);
    wingsPersistence.save(manifestFile);
    result = k8sStateHelper.doManifestsUseArtifact(APP_ID, INFRA_MAPPING_ID);
    assertThat(result).isFalse();

    try {
      when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(null);
      k8sStateHelper.doManifestsUseArtifact(APP_ID, INFRA_MAPPING_ID);
      fail("Should not reach here");
    } catch (InvalidRequestException e) {
      assertThat(e.getMessage()).isEqualTo("Infra mapping not found for appId APP_ID infraMappingId INFRA_MAPPING_ID");
    }

    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder().name(INFRA_DEFINITION_NAME).envId(ENV_ID).build();
    when(infrastructureDefinitionService.get(APP_ID, INFRA_DEFINITION_ID)).thenReturn(infrastructureDefinition);
    manifestFile.setFileContent(VALUES_YAML_WITH_ARTIFACT_REFERENCE);
    wingsPersistence.save(manifestFile);
    result = k8sStateHelper.doManifestsUseArtifact(APP_ID, SERVICE_ID, INFRA_DEFINITION_ID);
    assertThat(result).isTrue();

    when(infrastructureDefinitionService.get(APP_ID, INFRA_DEFINITION_ID)).thenReturn(null);
    try {
      k8sStateHelper.doManifestsUseArtifact(APP_ID, SERVICE_ID, INFRA_DEFINITION_ID);
      fail("Should not reach here");
    } catch (InvalidRequestException e) {
      assertThat(e.getMessage())
          .isEqualTo("Infra definition not found for appId APP_ID infraDefinitionId INFRA_DEFINITION_ID");
    }
  }

  private ManifestFile createManifestFile() {
    return ManifestFile.builder()
        .applicationManifestId(APPLICATION_MANIFEST_ID)
        .fileName(values_filename)
        .fileContent(VALUES_YAML_WITH_ARTIFACT_REFERENCE)
        .build();
  }

  @NotNull
  private ApplicationManifest createApplicationManifest() {
    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .kind(AppManifestKind.K8S_MANIFEST)
                                                  .storeType(StoreType.Local)
                                                  .serviceId(SERVICE_ID)
                                                  .build();
    applicationManifest.setAppId(APP_ID);
    applicationManifest.setUuid(APPLICATION_MANIFEST_ID);
    return applicationManifest;
  }

  private InfrastructureMapping createGCPInfraMapping() {
    return aGcpKubernetesInfrastructureMapping()
        .withNamespace("default")
        .withAppId(APP_ID)
        .withEnvId(ENV_ID)
        .withServiceId(SERVICE_ID)
        .withServiceTemplateId(TEMPLATE_ID)
        .withComputeProviderType(GCP.name())
        .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
        .withUuid(INFRA_MAPPING_ID)
        .build();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testValidateK8sV2TypeServiceUsed() {
    PhaseElement phaseElement =
        PhaseElement.builder().serviceElement(ServiceElement.builder().uuid(SERVICE_ID).build()).build();
    context.pushContextElement(phaseElement);

    try {
      when(serviceResourceService.get(SERVICE_ID))
          .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());
      k8sStateHelper.validateK8sV2TypeServiceUsed(context);

      fail("Should not reach here");
    } catch (InvalidRequestException ex) {
      assertThat(ex.getMessage())
          .isEqualTo("Service SERVICE_NAME used in workflow is of incompatible type. Use Kubernetes V2 type service");
    }

    when(serviceResourceService.get(SERVICE_ID))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).isK8sV2(true).build());
    k8sStateHelper.validateK8sV2TypeServiceUsed(context);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testRenderForKustomizeInDelegateManifestConfig() {
    when(settingsService.fetchGitConfigFromConnectorId(anyString())).thenReturn(new GitConfig());
    when(gitFileConfigHelperService.renderGitFileConfig(any(ExecutionContext.class), any(GitFileConfig.class)))
        .thenReturn(new GitFileConfig());

    ApplicationManifest appManifest = buildKustomizeAppManifest();
    K8sDelegateManifestConfig delegateManifestConfig =
        k8sStateHelper.createDelegateManifestConfig(context, appManifest);

    verify(gitFileConfigHelperService, times(1)).renderGitFileConfig(context, appManifest.getGitFileConfig());
    verify(kustomizeHelper, times(1)).renderKustomizeConfig(context, appManifest.getKustomizeConfig());
    assertThat(delegateManifestConfig.getKustomizeConfig()).isEqualTo(appManifest.getKustomizeConfig());
  }

  private ApplicationManifest buildKustomizeAppManifest() {
    GitFileConfig gitFileConfig = GitFileConfig.builder()
                                      .filePath("${filePath}")
                                      .connectorId("connector-id")
                                      .useBranch(true)
                                      .branch("${branch}")
                                      .build();
    return ApplicationManifest.builder()
        .kind(K8S_MANIFEST)
        .gitFileConfig(gitFileConfig)
        .storeType(KustomizeSourceRepo)
        .serviceId("serviceId")
        .build();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testTagsInGetPodList() throws Exception {
    DirectKubernetesInfrastructureMapping infrastructureMapping =
        DirectKubernetesInfrastructureMapping.builder().accountId(ACCOUNT_ID).build();
    infrastructureMapping.setComputeProviderSettingId(SETTING_ID);

    K8sTaskExecutionResponse response = K8sTaskExecutionResponse.builder()
                                            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                            .k8sTaskResponse(K8sInstanceSyncResponse.builder().build())
                                            .build();
    when(delegateService.executeTask(any())).thenReturn(response);

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withUuid(SETTING_ID)
                                            .withAccountId(ACCOUNT_ID)
                                            .withValue(AzureConfig.builder().build())
                                            .build();
    wingsPersistence.save(settingAttribute);

    k8sStateHelper.getPodList(infrastructureMapping, "default", "releaseName");

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).executeTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask.getTags()).isEmpty();

    k8sStateHelper.getPodList(infrastructureMapping, "default", "releaseName");
    captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(2)).executeTask(captor.capture());
    delegateTask = captor.getValue();
    assertThat(delegateTask.getTags()).isEmpty();

    doReturn(K8sClusterConfig.builder().cloudProvider(KubernetesClusterConfig.builder().build()).build())
        .when(containerDeploymentManagerHelper)
        .getK8sClusterConfig(any(), any());
    wingsPersistence.save(settingAttribute);
    k8sStateHelper.getPodList(infrastructureMapping, "default", "releaseName");
    captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(3)).executeTask(captor.capture());
    delegateTask = captor.getValue();
    assertThat(delegateTask.getTags()).isEmpty();

    doReturn(K8sClusterConfig.builder()
                 .cloudProvider(
                     KubernetesClusterConfig.builder().useKubernetesDelegate(true).delegateName("delegateName").build())
                 .build())
        .when(containerDeploymentManagerHelper)
        .getK8sClusterConfig(any(), any());
    wingsPersistence.save(settingAttribute);
    k8sStateHelper.getPodList(infrastructureMapping, "default", "releaseName");
    captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(4)).executeTask(captor.capture());
    delegateTask = captor.getValue();
    assertThat(delegateTask.getTags()).contains("delegateName");

    K8sPod k8sPod = K8sPod.builder().name("podName").namespace("default").build();
    response.setK8sTaskResponse(K8sInstanceSyncResponse.builder().k8sPodInfoList(asList(k8sPod)).build());
    List<K8sPod> k8sPods = k8sStateHelper.tryGetPodList(infrastructureMapping, "default", "releaseName");
    assertThat(k8sPods).contains(k8sPod);

    response.setCommandExecutionStatus(FAILURE);
    k8sPods = k8sStateHelper.tryGetPodList(infrastructureMapping, "default", "releaseName");
    assertThat(k8sPods).isNull();

    when(delegateService.executeTask(any()))
        .thenReturn(ErrorNotifyResponseData.builder().errorMessage("ErrorMessage").build());
    try {
      k8sStateHelper.getPodList(infrastructureMapping, "default", "releaseName");
    } catch (Exception ex) {
      assertThatExceptionOfType(K8sPodSyncException.class);
      assertThat(ex.getMessage()).isEqualTo("Failed to fetch PodList for release releaseName. Error: ErrorMessage");
    }

    when(delegateService.executeTask(any()))
        .thenReturn(RemoteMethodReturnValueData.builder()
                        .returnValue("returnValue")
                        .exception(new K8sPodSyncException("k8sPodSyncException"))
                        .build());
    try {
      k8sStateHelper.getPodList(infrastructureMapping, "default", "releaseName");
    } catch (Exception ex) {
      assertThatExceptionOfType(K8sPodSyncException.class);
      assertThat(ex.getMessage())
          .isEqualTo(
              "Failed to fetch PodList for release releaseName. Exception: RemoteMethodReturnValueData(returnValue=returnValue, exception=io.harness.exception.K8sPodSyncException: k8sPodSyncException)");
    }

    when(delegateService.executeTask(any())).thenReturn(HelmValuesFetchTaskResponse.builder().build());
    try {
      k8sStateHelper.getPodList(infrastructureMapping, "default", "releaseName");
    } catch (Exception ex) {
      assertThatExceptionOfType(K8sPodSyncException.class);
      assertThat(ex.getMessage())
          .isEqualTo(
              "Failed to fetch PodList for release releaseName. Unknown return type software.wings.helpers.ext.helm.response.HelmValuesFetchTaskResponse");
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testTimeoutInQueueK8sDelegateTask() throws Exception {
    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withUuid(SETTING_ID)
                                            .withAccountId(ACCOUNT_ID)
                                            .withValue(KubernetesClusterConfig.builder().build())
                                            .build();
    wingsPersistence.save(settingAttribute);

    DirectKubernetesInfrastructureMapping infrastructureMapping =
        DirectKubernetesInfrastructureMapping.builder().namespace("env").accountId(ACCOUNT_ID).build();
    infrastructureMapping.setComputeProviderSettingId(SETTING_ID);

    K8sTaskExecutionResponse response = K8sTaskExecutionResponse.builder().build();
    when(infrastructureMappingService.get(anyString(), anyString())).thenReturn(infrastructureMapping);
    when(delegateService.executeTask(any())).thenReturn(response);
    when(serviceTemplateHelper.fetchServiceTemplateId(any())).thenReturn(SETTING_ID);
    when(evaluator.substitute(anyString(), any(), any(), anyString())).thenReturn("default");
    when(serviceResourceService.getHelmVersionWithDefault(anyString(), anyString())).thenReturn(HelmVersion.V2);

    K8sRollingDeployTaskParameters taskParameters = K8sRollingDeployTaskParameters.builder().build();
    k8sStateHelper.queueK8sDelegateTask(context, taskParameters);
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask.getData().getTimeout()).isEqualTo(10 * 60 * 1000);

    taskParameters.setTimeoutIntervalInMin(300);
    k8sStateHelper.queueK8sDelegateTask(context, taskParameters);
    captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(2)).queueTask(captor.capture());
    delegateTask = captor.getValue();
    assertThat(delegateTask.getData().getTimeout()).isEqualTo(300 * 60 * 1000);

    taskParameters.setTimeoutIntervalInMin(0);
    k8sStateHelper.queueK8sDelegateTask(context, taskParameters);
    captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(3)).queueTask(captor.capture());
    delegateTask = captor.getValue();
    assertThat(delegateTask.getData().getTimeout()).isEqualTo(60 * 1000);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testTagsInQueueK8sDelegateTask() throws Exception {
    DirectKubernetesInfrastructureMapping infrastructureMapping =
        DirectKubernetesInfrastructureMapping.builder().namespace("env").accountId(ACCOUNT_ID).build();
    infrastructureMapping.setComputeProviderSettingId(SETTING_ID);

    K8sTaskExecutionResponse response = K8sTaskExecutionResponse.builder()
                                            .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                            .k8sTaskResponse(K8sInstanceSyncResponse.builder().build())
                                            .build();
    when(delegateService.executeTask(any())).thenReturn(response);
    when(serviceTemplateHelper.fetchServiceTemplateId(any())).thenReturn(SETTING_ID);
    when(infrastructureMappingService.get(anyString(), anyString())).thenReturn(infrastructureMapping);
    when(evaluator.substitute(anyString(), any(), any(), anyString())).thenReturn("default");
    when(serviceResourceService.getHelmVersionWithDefault(anyString(), anyString())).thenReturn(HelmVersion.V2);
    doReturn(K8sClusterConfig.builder().build())
        .when(containerDeploymentManagerHelper)
        .getK8sClusterConfig(any(ContainerInfrastructureMapping.class), eq(context));

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withUuid(SETTING_ID)
                                            .withAccountId(ACCOUNT_ID)
                                            .withValue(KubernetesClusterConfig.builder().build())
                                            .build();
    wingsPersistence.save(settingAttribute);

    k8sStateHelper.queueK8sDelegateTask(context, K8sRollingDeployTaskParameters.builder().build());

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask.getTags()).isEmpty();

    settingAttribute.setValue(
        KubernetesClusterConfig.builder().useKubernetesDelegate(true).delegateName("delegateName").build());
    wingsPersistence.save(settingAttribute);
    k8sStateHelper.queueK8sDelegateTask(context, K8sRollingDeployTaskParameters.builder().build());
    captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(2)).queueTask(captor.capture());
    delegateTask = captor.getValue();
    assertThat(delegateTask.getTags()).isEmpty();

    doReturn(K8sClusterConfig.builder()
                 .cloudProvider(
                     KubernetesClusterConfig.builder().useKubernetesDelegate(true).delegateName("delegateName").build())
                 .build())
        .when(containerDeploymentManagerHelper)
        .getK8sClusterConfig(any(ContainerInfrastructureMapping.class), eq(context));
    wingsPersistence.save(settingAttribute);
    k8sStateHelper.queueK8sDelegateTask(context, K8sRollingDeployTaskParameters.builder().build());
    captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(3)).queueTask(captor.capture());
    delegateTask = captor.getValue();
    assertThat(delegateTask.getTags()).contains("delegateName");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testFetchTagsFromK8sCloudProvider() {
    List<String> tags = k8sStateHelper.fetchTagsFromK8sCloudProvider(null);
    assertThat(tags).isEmpty();

    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder().build();
    tags = k8sStateHelper.fetchTagsFromK8sCloudProvider(containerServiceParams);
    assertThat(tags).isEmpty();

    SettingAttribute settingAttribute =
        aSettingAttribute().withAccountId(ACCOUNT_ID).withValue(AwsConfig.builder().build()).build();
    containerServiceParams.setSettingAttribute(settingAttribute);
    tags = k8sStateHelper.fetchTagsFromK8sCloudProvider(containerServiceParams);
    assertThat(tags).isEmpty();

    tags = k8sStateHelper.fetchTagsFromK8sCloudProvider(containerServiceParams);
    assertThat(tags).isEmpty();

    settingAttribute.setValue(KubernetesClusterConfig.builder().build());
    containerServiceParams.setSettingAttribute(settingAttribute);
    tags = k8sStateHelper.fetchTagsFromK8sCloudProvider(containerServiceParams);
    assertThat(tags).isEmpty();

    settingAttribute.setValue(KubernetesClusterConfig.builder().useKubernetesDelegate(true).build());
    containerServiceParams.setSettingAttribute(settingAttribute);
    tags = k8sStateHelper.fetchTagsFromK8sCloudProvider(containerServiceParams);
    assertThat(tags).isEmpty();

    settingAttribute.setValue(
        KubernetesClusterConfig.builder().useKubernetesDelegate(true).delegateName("delegateName").build());
    containerServiceParams.setSettingAttribute(settingAttribute);
    tags = k8sStateHelper.fetchTagsFromK8sCloudProvider(containerServiceParams);
    assertThat(tags).isNotEmpty();
    assertThat(tags).contains("delegateName");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetNewPods() {
    assertThat(k8sStateHelper.getNewPods(null)).isEmpty();
    assertThat(k8sStateHelper.getNewPods(emptyList())).isEmpty();

    final List<K8sPod> newPods = k8sStateHelper.getNewPods(
        asList(K8sPod.builder().name("pod-1").build(), K8sPod.builder().name("pod-2").newPod(true).build()));

    assertThat(newPods).hasSize(1);
    assertThat(newPods.get(0).isNewPod()).isTrue();
    assertThat(newPods.get(0).getName()).isEqualTo("pod-2");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetInstanceDetails() {
    assertThat(k8sStateHelper.getInstanceDetails(null, false)).isEmpty();
    assertThat(k8sStateHelper.getInstanceDetails(emptyList(), false)).isEmpty();

    List<InstanceDetails> instanceDetails;
    instanceDetails =
        k8sStateHelper.getInstanceDetails(asList(K8sPod.builder().name("pod-1").podIP("ip-1").build(),
                                              K8sPod.builder().name("pod-2").podIP("ip-2").newPod(true).build()),
            false);

    assertThat(instanceDetails).hasSize(2);
    assertThat(instanceDetails.stream().map(InstanceDetails::getInstanceType).collect(toSet()))
        .hasSize(1)
        .contains(InstanceDetails.InstanceType.K8s);
    assertThat(instanceDetails.stream().map(pod -> pod.getK8s().getPodName()).collect(toList()))
        .containsExactlyInAnyOrder("pod-1", "pod-2");
    assertThat(instanceDetails.stream().map(pod -> pod.getK8s().getIp()).collect(toList()))
        .containsExactlyInAnyOrder("ip-1", "ip-2");
    assertThat(instanceDetails.stream().filter(InstanceDetails::isNewInstance).count()).isEqualTo(1);

    instanceDetails =
        k8sStateHelper.getInstanceDetails(asList(K8sPod.builder().name("pod-1").podIP("ip-1").build(),
                                              K8sPod.builder().name("pod-2").podIP("ip-2").newPod(true).build()),
            true);
    assertThat(instanceDetails).hasSize(2);
    assertThat(instanceDetails.stream().map(InstanceDetails::getInstanceType).collect(toSet()))
        .hasSize(1)
        .contains(InstanceDetails.InstanceType.K8s);
    assertThat(instanceDetails.stream().map(pod -> pod.getK8s().getPodName()).collect(toList()))
        .containsExactlyInAnyOrder("pod-1", "pod-2");
    assertThat(instanceDetails.stream().map(pod -> pod.getK8s().getIp()).collect(toList()))
        .containsExactlyInAnyOrder("ip-1", "ip-2");
    assertThat(instanceDetails.stream().filter(InstanceDetails::isNewInstance).count()).isEqualTo(2);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetInstanceElements() {
    assertThat(k8sStateHelper.getInstanceElementList(null, false)).isEmpty();
    assertThat(k8sStateHelper.getInstanceElementList(emptyList(), false)).isEmpty();

    List<InstanceElement> instanceElements;
    instanceElements =
        k8sStateHelper.getInstanceElementList(asList(K8sPod.builder().name("pod-1").podIP("ip-1").build(),
                                                  K8sPod.builder().name("pod-2").podIP("ip-2").newPod(true).build()),
            false);

    assertThat(instanceElements).hasSize(2);
    assertThat(instanceElements.stream().map(InstanceElement::getPodName).collect(toList()))
        .containsExactlyInAnyOrder("pod-1", "pod-2");
    assertThat(instanceElements.stream().filter(InstanceElement::isNewInstance).count()).isEqualTo(1);

    instanceElements =
        k8sStateHelper.getInstanceElementList(asList(K8sPod.builder().name("pod-1").podIP("ip-1").build(),
                                                  K8sPod.builder().name("pod-2").podIP("ip-2").newPod(true).build()),
            true);
    assertThat(instanceElements).hasSize(2);
    assertThat(instanceElements.stream().map(InstanceElement::getPodName).collect(toList()))
        .containsExactlyInAnyOrder("pod-1", "pod-2");
    assertThat(instanceElements.stream().filter(InstanceElement::isNewInstance).count()).isEqualTo(2);
  }
  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseForHelmFetchTask() {
    HelmValuesFetchTaskResponse valuesFetchTaskResponse =
        HelmValuesFetchTaskResponse.builder().commandExecutionStatus(FAILURE).build();
    Map<String, ResponseData> response = new HashMap<>();
    response.put(ACTIVITY_ID, valuesFetchTaskResponse);

    K8sStateExecutor k8sStateExecutor = mock(K8sStateExecutor.class);
    K8sStateExecutionData k8sStateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    k8sStateExecutionData.setCurrentTaskType(TaskType.HELM_VALUES_FETCH);
    k8sStateExecutionData.setActivityId(ACTIVITY_ID);
    k8sStateExecutionData.setValuesFiles(new HashMap<>());

    ExecutionResponse executionResponse =
        k8sStateHelper.handleAsyncResponseWrapper(k8sStateExecutor, context, response);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    verify(activityService, times(1)).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);

    valuesFetchTaskResponse.setValuesFileContent("VALUES_FILE_CONTENT");
    valuesFetchTaskResponse.setCommandExecutionStatus(SUCCESS);
    k8sStateHelper.handleAsyncResponseWrapper(k8sStateExecutor, context, response);
    k8sStateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();

    ArgumentCaptor<AppManifestKind> argumentCaptor = ArgumentCaptor.forClass(AppManifestKind.class);
    verify(applicationManifestUtils, times(1))
        .getApplicationManifests(any(ExecutionContextImpl.class), argumentCaptor.capture());
    assertThat(argumentCaptor.getValue()).isEqualTo(AppManifestKind.VALUES);
    assertThat(k8sStateExecutionData.getValuesFiles().get(K8sValuesLocation.Service))
        .containsExactly("VALUES_FILE_CONTENT");

    k8sStateExecutionData.setValuesFiles(new HashMap<>());
    valuesFetchTaskResponse.setValuesFileContent("");
    valuesFetchTaskResponse.setCommandExecutionStatus(SUCCESS);
    k8sStateHelper.handleAsyncResponseWrapper(k8sStateExecutor, context, response);
    k8sStateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    assertThat(k8sStateExecutionData.getValuesFiles().containsKey(K8sValuesLocation.Service)).isFalse();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseForGitTaskWrapper() {
    GitCommandExecutionResponse gitCommandExecutionResponse =
        GitCommandExecutionResponse.builder()
            .gitCommandStatus(GitCommandExecutionResponse.GitCommandStatus.FAILURE)
            .build();
    Map<String, ResponseData> response = new HashMap<>();
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    response.put(ACTIVITY_ID, gitCommandExecutionResponse);

    K8sStateExecutor k8sStateExecutor = mock(K8sStateExecutor.class);
    K8sStateExecutionData k8sStateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    k8sStateExecutionData.setCurrentTaskType(TaskType.GIT_COMMAND);
    k8sStateExecutionData.setActivityId(ACTIVITY_ID);
    k8sStateExecutionData.setValuesFiles(new HashMap<>());
    k8sStateExecutionData.setApplicationManifestMap(appManifestMap);

    ExecutionResponse executionResponse =
        k8sStateHelper.handleAsyncResponseWrapper(k8sStateExecutor, context, response);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    verify(activityService, times(1)).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);

    Map<K8sValuesLocation, Collection<String>> valuesMap = new HashMap<>();
    valuesMap.put(K8sValuesLocation.Environment, singletonList("EnvValues"));
    when(applicationManifestUtils.getValuesFilesFromGitFetchFilesResponse(appManifestMap, gitCommandExecutionResponse))
        .thenReturn(valuesMap);
    gitCommandExecutionResponse.setGitCommandStatus(GitCommandExecutionResponse.GitCommandStatus.SUCCESS);
    k8sStateHelper.handleAsyncResponseWrapper(k8sStateExecutor, context, response);
    k8sStateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    assertThat(k8sStateExecutionData.getValuesFiles().get(K8sValuesLocation.Environment)).containsExactly("EnvValues");
    verify(applicationManifestUtils, times(1))
        .getValuesFilesFromGitFetchFilesResponse(appManifestMap, gitCommandExecutionResponse);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExceptionInHandleAsyncResponseWrapper() {
    K8sStateExecutor k8sStateExecutor = mock(K8sStateExecutor.class);
    K8sStateExecutionData k8sStateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    k8sStateExecutionData.setCurrentTaskType(TaskType.GIT_FETCH_FILES_TASK);

    try {
      k8sStateHelper.handleAsyncResponseWrapper(k8sStateExecutor, context, null);
      fail("Should not reach here");
    } catch (WingsException ex) {
      assertThat(ex.getMessage()).isEqualTo("Unhandled task type GIT_FETCH_FILES_TASK");
    }

    k8sStateExecutionData.setCurrentTaskType(TaskType.HELM_VALUES_FETCH);
    try {
      k8sStateHelper.handleAsyncResponseWrapper(k8sStateExecutor, context, null);
      fail("Should not reach here");
    } catch (Exception ex) {
      assertThat(ex).isInstanceOf(InvalidRequestException.class);
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetReleaseName() {
    DirectKubernetesInfrastructureMapping infrastructureMapping =
        DirectKubernetesInfrastructureMapping.builder().build();

    infrastructureMapping.setReleaseName("release-name");
    String releaseName = k8sStateHelper.getReleaseName(context, infrastructureMapping);
    assertThat(releaseName).isEqualTo("release-name");

    infrastructureMapping.setUuid(INFRA_MAPPING_ID);
    infrastructureMapping.setReleaseName(null);
    releaseName = k8sStateHelper.getReleaseName(context, infrastructureMapping);
    assertThat(releaseName).isEqualTo("64317ae8-c2a8-3fd8-af26-68f2e717431a");

    infrastructureMapping.setReleaseName("-release-name");
    try {
      k8sStateHelper.getReleaseName(context, infrastructureMapping);
      fail("Should not reach here");
    } catch (Exception ex) {
      assertThatExceptionOfType(InvalidArgumentsException.class);
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetRenderedValuesFiles() {
    when(openShiftManagerService.isOpenShiftManifestConfig(context)).thenReturn(false);

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    K8sStateExecutionData k8sStateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    k8sStateExecutionData.setValuesFiles(new HashMap<>());
    k8sStateExecutionData.getValuesFiles().put(K8sValuesLocation.Environment, singletonList("envValues"));
    k8sStateExecutionData.getValuesFiles().put(
        K8sValuesLocation.ServiceOverride, singletonList("serviceOverrideValues"));
    k8sStateExecutionData.getValuesFiles().put(K8sValuesLocation.Service, singletonList("serviceValues"));
    k8sStateExecutionData.getValuesFiles().put(K8sValuesLocation.EnvironmentGlobal, singletonList("envGlobalValues"));

    List<String> valuesFiles = k8sStateHelper.getRenderedValuesFiles(appManifestMap, context);
    assertThat(valuesFiles).hasSize(4);
    assertThat(valuesFiles.get(0)).isEqualTo("serviceValues");
    assertThat(valuesFiles.get(1)).isEqualTo("serviceOverrideValues");
    assertThat(valuesFiles.get(2)).isEqualTo("envGlobalValues");
    assertThat(valuesFiles.get(3)).isEqualTo("envValues");

    k8sStateExecutionData.getValuesFiles().put(K8sValuesLocation.ServiceOverride, singletonList(" "));
    k8sStateExecutionData.getValuesFiles().put(K8sValuesLocation.Environment, singletonList(""));
    valuesFiles = k8sStateHelper.getRenderedValuesFiles(appManifestMap, context);
    assertThat(valuesFiles).hasSize(2);
    assertThat(valuesFiles.get(0)).isEqualTo("serviceValues");
    assertThat(valuesFiles.get(1)).isEqualTo("envGlobalValues");

    k8sStateExecutionData.getValuesFiles().remove(K8sValuesLocation.ServiceOverride);
    k8sStateExecutionData.getValuesFiles().remove(K8sValuesLocation.Service);
    k8sStateExecutionData.getValuesFiles().put(K8sValuesLocation.Environment, singletonList("envValues"));
    k8sStateExecutionData.getValuesFiles().put(K8sValuesLocation.EnvironmentGlobal, singletonList("envGlobalValues"));

    valuesFiles = k8sStateHelper.getRenderedValuesFiles(appManifestMap, context);
    assertThat(valuesFiles).hasSize(2);
    assertThat(valuesFiles.get(0)).isEqualTo("envGlobalValues");
    assertThat(valuesFiles.get(1)).isEqualTo("envValues");

    when(openShiftManagerService.isOpenShiftManifestConfig(context)).thenReturn(true);
    k8sStateExecutionData.getValuesFiles().put(
        K8sValuesLocation.ServiceOverride, singletonList("serviceOverrideValues"));
    k8sStateExecutionData.getValuesFiles().put(K8sValuesLocation.Service, singletonList("serviceValues"));
    valuesFiles = k8sStateHelper.getRenderedValuesFiles(appManifestMap, context);
    assertThat(valuesFiles).hasSize(4);
    assertThat(valuesFiles.get(0)).isEqualTo("envValues");
    assertThat(valuesFiles.get(1)).isEqualTo("envGlobalValues");
    assertThat(valuesFiles.get(2)).isEqualTo("serviceOverrideValues");
    assertThat(valuesFiles.get(3)).isEqualTo("serviceValues");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetRenderedValuesFilesWithMultipleFiles() {
    when(openShiftManagerService.isOpenShiftManifestConfig(context)).thenReturn(false);

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    K8sStateExecutionData k8sStateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    k8sStateExecutionData.setValuesFiles(new HashMap<>());

    k8sStateExecutionData.getValuesFiles().put(K8sValuesLocation.Environment, asList("envValues1", "envValues2"));
    k8sStateExecutionData.getValuesFiles().put(K8sValuesLocation.ServiceOverride,
        asList("serviceOverrideValues1", "serviceOverrideValues2", "serviceOverrideValues3"));
    k8sStateExecutionData.getValuesFiles().put(K8sValuesLocation.Service, asList("serviceValues1", "serviceValues2"));
    k8sStateExecutionData.getValuesFiles().put(K8sValuesLocation.EnvironmentGlobal, singletonList("envGlobalValues"));
    List<String> valuesFiles = k8sStateHelper.getRenderedValuesFiles(appManifestMap, context);
    assertThat(valuesFiles).hasSize(8);
    assertThat(valuesFiles)
        .containsExactly("serviceValues1", "serviceValues2", "serviceOverrideValues1", "serviceOverrideValues2",
            "serviceOverrideValues3", "envGlobalValues", "envValues1", "envValues2");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecuteGitTask() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    K8sStateExecutionData k8sStateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    k8sStateExecutionData.setValuesFiles(new HashMap<>());
    GitFetchFilesTaskParams fetchFilesTaskParams = GitFetchFilesTaskParams.builder().build();
    fetchFilesTaskParams.setBindTaskFeatureSet(true);

    DirectKubernetesInfrastructureMapping infrastructureMapping =
        DirectKubernetesInfrastructureMapping.builder().build();
    infrastructureMapping.setUuid(INFRA_MAPPING_ID);

    when(applicationManifestUtils.createGitFetchFilesTaskParams(context, application, appManifestMap))
        .thenReturn(fetchFilesTaskParams);
    when(infrastructureMappingService.get(APP_ID, null)).thenReturn(infrastructureMapping);

    ExecutionResponse executionResponse =
        k8sStateHelper.executeGitTask(context, appManifestMap, ACTIVITY_ID, "commandName");
    assertThat(executionResponse.isAsync()).isTrue();
    K8sStateExecutionData responseStateExecutionData =
        (K8sStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(responseStateExecutionData.getCurrentTaskType()).isEqualTo(TaskType.GIT_COMMAND);
    assertThat(responseStateExecutionData.getActivityId()).isEqualTo(ACTIVITY_ID);
    assertThat(responseStateExecutionData.getCommandName()).isEqualTo("commandName");
    verify(applicationManifestUtils, times(1)).setValuesPathInGitFetchFilesTaskParams(fetchFilesTaskParams);
    verify(applicationManifestUtils, times(1)).populateRemoteGitConfigFilePathList(context, appManifestMap);

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask.getAppId()).isEqualTo(APP_ID);
    assertThat(delegateTask.getEnvId()).isEqualTo(ENV_ID);
    assertThat(delegateTask.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(delegateTask.getInfrastructureMappingId()).isEqualTo(INFRA_MAPPING_ID);
    assertThat(delegateTask.getData().getTaskType()).isEqualTo(TaskType.GIT_FETCH_FILES_TASK.name());
    assertThat(delegateTask.getData().isAsync()).isTrue();
    assertThat(delegateTask.getData().getTimeout())
        .isEqualTo(TimeUnit.MINUTES.toMillis(GIT_FETCH_FILES_TASK_ASYNC_TIMEOUT));
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecuteHelmValuesFetchTask() {
    ApplicationManifest appManifest = ApplicationManifest.builder().storeType(Remote).build();
    appManifest.setStoreType(HelmChartRepo);
    HelmChartConfigParams helmChartConfigParams = HelmChartConfigParams.builder().build();
    DirectKubernetesInfrastructureMapping infrastructureMapping =
        DirectKubernetesInfrastructureMapping.builder().build();
    infrastructureMapping.setUuid(INFRA_MAPPING_ID);

    when(infrastructureMappingService.get(APP_ID, null)).thenReturn(infrastructureMapping);
    when(applicationManifestUtils.getAppManifestByApplyingHelmChartOverride(context)).thenReturn(appManifest);
    when(helmChartConfigHelperService.getHelmChartConfigTaskParams(context, appManifest))
        .thenReturn(helmChartConfigParams);

    ExecutionResponse executionResponse =
        k8sStateHelper.executeHelmValuesFetchTask(context, ACTIVITY_ID, "commandName", 10 * 60 * 1000L);
    assertThat(executionResponse.isAsync()).isTrue();
    K8sStateExecutionData responseStateExecutionData =
        (K8sStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(responseStateExecutionData.getCurrentTaskType()).isEqualTo(TaskType.HELM_VALUES_FETCH);
    assertThat(responseStateExecutionData.getActivityId()).isEqualTo(ACTIVITY_ID);
    assertThat(responseStateExecutionData.getCommandName()).isEqualTo("commandName");
    verify(applicationManifestUtils, times(1)).getAppManifestByApplyingHelmChartOverride(context);
    verify(helmChartConfigHelperService, times(1)).getHelmChartConfigTaskParams(context, appManifest);

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask.getAppId()).isEqualTo(APP_ID);
    assertThat(delegateTask.getEnvId()).isEqualTo(ENV_ID);
    assertThat(delegateTask.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(delegateTask.getInfrastructureMappingId()).isEqualTo(INFRA_MAPPING_ID);
    assertThat(delegateTask.getData().getTaskType()).isEqualTo(TaskType.HELM_VALUES_FETCH.name());
    assertThat(delegateTask.getData().isAsync()).isTrue();
    assertThat(delegateTask.getData().getTimeout()).isEqualTo(TimeUnit.MINUTES.toMillis(10));

    when(infrastructureMappingService.get(APP_ID, null)).thenReturn(null);
    k8sStateHelper.executeHelmValuesFetchTask(context, ACTIVITY_ID, "commandName", 10 * 60 * 1000L);
    captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(2)).queueTask(captor.capture());
    assertThat(captor.getValue().getInfrastructureMappingId()).isEqualTo(null);

    when(applicationManifestUtils.getAppManifestByApplyingHelmChartOverride(context)).thenReturn(null);
    try {
      k8sStateHelper.executeHelmValuesFetchTask(context, ACTIVITY_ID, "commandName", 10 * 60 * 1000L);
      fail("Should not reach here");
    } catch (Exception ex) {
      assertThat(ex.getMessage())
          .isEqualTo("Application Manifest not found while preparing helm values fetch task params");
    }

    appManifest.setStoreType(HelmSourceRepo);
    when(applicationManifestUtils.getAppManifestByApplyingHelmChartOverride(context)).thenReturn(appManifest);
    try {
      k8sStateHelper.executeHelmValuesFetchTask(context, ACTIVITY_ID, "commandName", 10 * 60 * 1000L);
      fail("Should not reach here");
    } catch (Exception ex) {
      assertThat(ex.getMessage())
          .isEqualTo("Application Manifest not found while preparing helm values fetch task params");
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetNamespacesFromK8sPodList() {
    Set<String> namespaces = k8sStateHelper.getNamespacesFromK8sPodList(emptyList());
    assertThat(namespaces).isEmpty();

    namespaces = k8sStateHelper.getNamespacesFromK8sPodList(
        asList(K8sPod.builder().namespace("default").build(), K8sPod.builder().namespace("namespace").build()));
    assertThat(namespaces).contains("default", "namespace");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetInstanceElementListParam() {
    InstanceElementListParam instanceElementListParam = k8sStateHelper.getInstanceElementListParam(emptyList());
    assertThat(instanceElementListParam.getInstanceElements()).isEmpty();

    instanceElementListParam = k8sStateHelper.getInstanceElementListParam(
        asList(K8sPod.builder().namespace("default").name("podName").podIP("127.0.0.1").build()));
    assertThat(instanceElementListParam.getInstanceElements()).isNotEmpty();
    assertThat(instanceElementListParam.getInstanceElements().get(0).getDisplayName()).isEqualTo("podName");
    assertThat(instanceElementListParam.getInstanceElements().get(0).getUuid()).isEqualTo("podName");
    assertThat(instanceElementListParam.getInstanceElements().get(0).getHostName()).isEqualTo("podName");
    assertThat(instanceElementListParam.getInstanceElements().get(0).getHost().getHostName()).isEqualTo("podName");
    assertThat(instanceElementListParam.getInstanceElements().get(0).getHost().getIp()).isEqualTo("127.0.0.1");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetInstanceStatusSummaries() {
    List<InstanceStatusSummary> instanceStatusSummaries =
        k8sStateHelper.getInstanceStatusSummaries(emptyList(), ExecutionStatus.SUCCESS);
    assertThat(instanceStatusSummaries).isEmpty();

    instanceStatusSummaries =
        k8sStateHelper.getInstanceStatusSummaries(asList(anInstanceElement().build()), ExecutionStatus.SUCCESS);
    assertThat(instanceStatusSummaries.get(0).getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testCreateDelegateManifestConfig_Local() {
    ApplicationManifest appManifest = ApplicationManifest.builder().storeType(Local).kind(K8S_MANIFEST).build();
    appManifest.setUuid(APPLICATION_MANIFEST_ID);
    appManifest.setAppId(APP_ID);
    ManifestFile manifestFile = ManifestFile.builder()
                                    .fileContent("fileContent")
                                    .fileName("fileName")
                                    .applicationManifestId(APPLICATION_MANIFEST_ID)
                                    .build();
    manifestFile.setAppId(APP_ID);
    wingsPersistence.save(appManifest);
    wingsPersistence.save(manifestFile);

    K8sDelegateManifestConfig delegateManifestConfig =
        k8sStateHelper.createDelegateManifestConfig(context, appManifest);
    assertThat(delegateManifestConfig.getManifestFiles().get(0).getFileName()).isEqualTo("fileName");
    assertThat(delegateManifestConfig.getManifestFiles().get(0).getFileContent()).isEqualTo("fileContent");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testSaveK8sElement() {
    K8sElement k8sElement = K8sElement.builder().releaseName("releaseName").releaseNumber(12).build();
    k8sStateHelper.saveK8sElement(context, k8sElement);

    K8sElement savedK8sElement = k8sStateHelper.getK8sElement(context);
    assertThat(savedK8sElement).isNull();

    SweepingOutputInstance sweepingOutputInstance =
        context.prepareSweepingOutputBuilder(SweepingOutputInstance.Scope.WORKFLOW)
            .name("k8s")
            .output(kryoSerializer.asDeflatedBytes(k8sElement))
            .build();

    when(sweepingOutputService.find(any())).thenReturn(sweepingOutputInstance);
    savedK8sElement = k8sStateHelper.getK8sElement(context);
    assertThat(savedK8sElement.getReleaseName()).isEqualTo("releaseName");
    assertThat(savedK8sElement.getReleaseNumber()).isEqualTo(12);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testUpdateManifestsArtifactVariableNames() {
    try {
      k8sStateHelper.updateManifestsArtifactVariableNames(APP_ID, INFRA_MAPPING_ID, emptySet());
    } catch (Exception ex) {
      assertThatExceptionOfType(InvalidRequestException.class);
      assertThat(ex.getMessage()).isEqualTo("Infra mapping not found for appId APP_ID infraMappingId INFRA_MAPPING_ID");
    }

    ApplicationManifest applicationManifest = createApplicationManifest();
    ManifestFile manifestFile = createManifestFile();
    wingsPersistence.save(applicationManifest);
    wingsPersistence.save(manifestFile);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(createGCPInfraMapping());

    Set<String> serviceArtifactVariableNames = new HashSet<>();
    k8sStateHelper.updateManifestsArtifactVariableNames(APP_ID, INFRA_MAPPING_ID, serviceArtifactVariableNames);
    assertThat(serviceArtifactVariableNames).contains("artifact");

    try {
      k8sStateHelper.updateManifestsArtifactVariableNamesInfraDefinition(
          APP_ID, INFRA_DEFINITION_ID, emptySet(), SERVICE_ID);
    } catch (Exception ex) {
      assertThatExceptionOfType(InvalidRequestException.class);
      assertThat(ex.getMessage())
          .isEqualTo("Infra Definition not found for appId APP_ID infraDefinitionId INFRA_DEFINITION_ID");
    }

    when(infrastructureDefinitionService.get(APP_ID, INFRA_DEFINITION_ID))
        .thenReturn(InfrastructureDefinition.builder().envId(ENV_ID).build());
    serviceArtifactVariableNames = new HashSet<>();
    k8sStateHelper.updateManifestsArtifactVariableNamesInfraDefinition(
        APP_ID, INFRA_DEFINITION_ID, serviceArtifactVariableNames, SERVICE_ID);
    assertThat(serviceArtifactVariableNames).contains("artifact");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecuteWrapperWithManifest() {
    K8sStateExecutor k8sStateExecutor = mock(K8sStateExecutor.class);
    K8sStateExecutionData k8sStateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
    k8sStateExecutionData.setValuesFiles(new HashMap<>());

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    appManifestMap.put(K8sValuesLocation.Service,
        ApplicationManifest.builder().storeType(Remote).kind(AppManifestKind.K8S_MANIFEST).build());
    when(applicationManifestUtils.getOverrideApplicationManifests(context, AppManifestKind.OC_PARAMS))
        .thenReturn(appManifestMap);
    when(openShiftManagerService.isOpenShiftManifestConfig(context)).thenReturn(true);
    when(activityService.save(any(Activity.class))).thenReturn(Activity.builder().uuid(ACTIVITY_ID).build());
    when(applicationManifestUtils.createGitFetchFilesTaskParams(context, application, appManifestMap))
        .thenReturn(GitFetchFilesTaskParams.builder().build());
    ExecutionResponse executionResponse =
        k8sStateHelper.executeWrapperWithManifest(k8sStateExecutor, context, 10 * 60 * 1000L);
    assertThat(((K8sStateExecutionData) executionResponse.getStateExecutionData()).getCurrentTaskType())
        .isEqualTo(TaskType.GIT_COMMAND);

    when(applicationManifestUtils.getAppManifestByApplyingHelmChartOverride(context))
        .thenReturn(ApplicationManifest.builder().storeType(HelmChartRepo).kind(AppManifestKind.K8S_MANIFEST).build());
    when(applicationManifestUtils.isValuesInHelmChartRepo(context)).thenReturn(true);
    when(openShiftManagerService.isOpenShiftManifestConfig(context)).thenReturn(false);
    executionResponse = k8sStateHelper.executeWrapperWithManifest(k8sStateExecutor, context, 10 * 60 * 1000L);
    assertThat(((K8sStateExecutionData) executionResponse.getStateExecutionData()).getCurrentTaskType())
        .isEqualTo(TaskType.HELM_VALUES_FETCH);

    appManifestMap.put(K8sValuesLocation.Service,
        ApplicationManifest.builder().storeType(HelmSourceRepo).kind(AppManifestKind.K8S_MANIFEST).build());
    when(applicationManifestUtils.getApplicationManifests(context, AppManifestKind.VALUES)).thenReturn(appManifestMap);
    when(openShiftManagerService.isOpenShiftManifestConfig(context)).thenReturn(false);
    when(applicationManifestUtils.isValuesInHelmChartRepo(context)).thenReturn(false);
    executionResponse = k8sStateHelper.executeWrapperWithManifest(k8sStateExecutor, context, 10 * 60 * 1000L);
    assertThat(((K8sStateExecutionData) executionResponse.getStateExecutionData()).getCurrentTaskType())
        .isEqualTo(TaskType.GIT_COMMAND);

    when(k8sStateExecutor.executeK8sTask(context, ACTIVITY_ID))
        .thenThrow(new InvalidRequestException("App not found"))
        .thenThrow(new UnsupportedOperationException("asd"));
    when(applicationManifestUtils.getApplicationManifests(context, AppManifestKind.VALUES)).thenReturn(emptyMap());
    try {
      k8sStateHelper.executeWrapperWithManifest(k8sStateExecutor, context, 10 * 60 * 1000L);
    } catch (Exception ex) {
      assertThatExceptionOfType(InvalidRequestException.class);
      assertThat(ex.getMessage()).isEqualTo("App not found");
    }

    when(applicationManifestUtils.getApplicationManifests(context, AppManifestKind.VALUES)).thenReturn(emptyMap());
    try {
      k8sStateHelper.executeWrapperWithManifest(k8sStateExecutor, context, 10 * 60 * 1000L);
    } catch (Exception ex) {
      assertThatExceptionOfType(InvalidRequestException.class);
      assertThat(ex.getCause()).isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetApplicationManifests() {
    when(openShiftManagerService.isOpenShiftManifestConfig(context)).thenReturn(true);
    k8sStateHelper.getApplicationManifests(context);
    ArgumentCaptor<AppManifestKind> argumentCaptor = ArgumentCaptor.forClass(AppManifestKind.class);
    verify(applicationManifestUtils, times(1)).getApplicationManifests(any(), argumentCaptor.capture());
    assertThat(argumentCaptor.getValue()).isEqualTo(AppManifestKind.OC_PARAMS);

    when(openShiftManagerService.isOpenShiftManifestConfig(context)).thenReturn(false);
    k8sStateHelper.getApplicationManifests(context);
    argumentCaptor = ArgumentCaptor.forClass(AppManifestKind.class);
    verify(applicationManifestUtils, times(2)).getApplicationManifests(any(), argumentCaptor.capture());
    assertThat(argumentCaptor.getValue()).isEqualTo(AppManifestKind.VALUES);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testSaveInstanceInfoToSweepingOutput() {
    k8sStateHelper.saveInstanceInfoToSweepingOutput(context, asList(anInstanceElement().dockerId("dockerId").build()),
        asList(InstanceDetails.builder().hostName("hostName").build()));

    ArgumentCaptor<SweepingOutputInstance> argumentCaptor = ArgumentCaptor.forClass(SweepingOutputInstance.class);
    verify(sweepingOutputService, times(1)).save(argumentCaptor.capture());

    InstanceInfoVariables instanceInfoVariables = (InstanceInfoVariables) argumentCaptor.getValue().getValue();
    assertThat(instanceInfoVariables.getInstanceDetails().get(0).getHostName()).isEqualTo("hostName");
    assertThat(instanceInfoVariables.getInstanceElements().get(0).getDockerId()).isEqualTo("dockerId");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetK8sHelmDeploymentElement() {
    ArgumentCaptor<SweepingOutputInquiry> inquiryCaptor = ArgumentCaptor.forClass(SweepingOutputInquiry.class);
    k8sStateHelper.getK8sHelmDeploymentElement(context);
    verify(sweepingOutputService, times(1)).findSweepingOutput(inquiryCaptor.capture());

    SweepingOutputInquiry inquiry = inquiryCaptor.getValue();
    assertThat(inquiry.getName()).isEqualTo(K8sHelmDeploymentElement.SWEEPING_OUTPUT_NAME);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testStorePreviousHelmDeploymentInfo() {
    long epochNow = Instant.now().toEpochMilli();
    HelmChartInfo chartInfo = HelmChartInfo.builder().name("chart").version("1.1.0").build();
    HelmChartInfo oldChartInfo = HelmChartInfo.builder().name("chart").version("1.0.0").build();
    List<Instance> singleInstanceList = singletonList(instanceWithHelmChartInfoAndDeployedAt(chartInfo, epochNow));
    List<Instance> multipleInstancesWithNulls = asList(instanceWithHelmChartInfoAndDeployedAt(null, epochNow),
        instanceWithHelmChartInfoAndDeployedAt(null, epochNow), instanceWithHelmChartInfoAndDeployedAt(null, epochNow));
    List<Instance> multipleInstancesWithDifferentDeployedTime =
        asList(instanceWithHelmChartInfoAndDeployedAt(oldChartInfo, epochNow - 1000),
            instanceWithHelmChartInfoAndDeployedAt(oldChartInfo, epochNow - 2000),
            instanceWithHelmChartInfoAndDeployedAt(chartInfo, epochNow));

    // Not helm chart deployment
    testStorePreviousHelmDeploymentInfoForNonHelmDeployment();
    // Shouldn't override existing K8sHelmDeploymentElement
    testDoNotOverrideExistingK8sHelmDeploymentElement();
    // With single existing instance
    testStorePreviousHelmDeploymentInfoForHelmDeployment(HelmChartRepo, singleInstanceList, chartInfo);
    // With multiple existing instances and null values
    testStorePreviousHelmDeploymentInfoForHelmDeployment(HelmSourceRepo, multipleInstancesWithNulls, null);
    // With multiple existing instances with different deployed time
    testStorePreviousHelmDeploymentInfoForHelmDeployment(
        HelmChartRepo, multipleInstancesWithDifferentDeployedTime, chartInfo);
    // With empty existing instances
    testStorePreviousHelmDeploymentInfoForHelmDeployment(HelmSourceRepo, emptyList(), null);
  }

  private Instance instanceWithHelmChartInfoAndDeployedAt(HelmChartInfo helmChartInfo, long deployedAt) {
    return Instance.builder()
        .instanceInfo(K8sPodInfo.builder().helmChartInfo(helmChartInfo).build())
        .lastDeployedAt(deployedAt)
        .build();
  }

  private void testStorePreviousHelmDeploymentInfoForNonHelmDeployment() {
    ApplicationManifest applicationManifest = ApplicationManifest.builder().storeType(Local).build();
    k8sStateHelper.storePreviousHelmDeploymentInfo(context, applicationManifest);
    verify(sweepingOutputService, never()).findSweepingOutput(any(SweepingOutputInquiry.class));
    verify(sweepingOutputService, never()).ensure(any(SweepingOutputInstance.class));
    verify(instanceService, never()).getInstancesForAppAndInframapping(anyString(), anyString());
  }

  private void testStorePreviousHelmDeploymentInfoForHelmDeployment(
      StoreType storeType, List<Instance> instances, HelmChartInfo expectedChartInfo) {
    reset(sweepingOutputService);
    ApplicationManifest applicationManifest = ApplicationManifest.builder().storeType(storeType).build();
    doReturn(instances).when(instanceService).getInstancesForAppAndInframapping(anyString(), anyString());
    doReturn(null).when(sweepingOutputService).findSweepingOutput(any(SweepingOutputInquiry.class));

    k8sStateHelper.storePreviousHelmDeploymentInfo(context, applicationManifest);
    ArgumentCaptor<SweepingOutputInstance> instanceCaptor = ArgumentCaptor.forClass(SweepingOutputInstance.class);
    verify(sweepingOutputService, times(1)).ensure(instanceCaptor.capture());

    SweepingOutputInstance instance = instanceCaptor.getValue();
    assertThat(instance.getValue()).isExactlyInstanceOf(K8sHelmDeploymentElement.class);
    K8sHelmDeploymentElement k8sHelmDeploymentElement = (K8sHelmDeploymentElement) instance.getValue();
    assertThat(k8sHelmDeploymentElement.getPreviousDeployedHelmChart()).isEqualTo(expectedChartInfo);
  }

  private void testDoNotOverrideExistingK8sHelmDeploymentElement() {
    reset(sweepingOutputService);
    ApplicationManifest applicationManifest = ApplicationManifest.builder().storeType(HelmChartRepo).build();
    K8sHelmDeploymentElement existingElement = K8sHelmDeploymentElement.builder().build();
    doReturn(existingElement).when(sweepingOutputService).findSweepingOutput(any(SweepingOutputInquiry.class));

    k8sStateHelper.storePreviousHelmDeploymentInfo(context, applicationManifest);
    verify(sweepingOutputService, times(1)).findSweepingOutput(any(SweepingOutputInquiry.class));
    verify(instanceService, never()).getInstancesForAppAndInframapping(anyString(), anyString());
    verify(sweepingOutputService, never()).ensure(any(SweepingOutputInstance.class));
  }
}
