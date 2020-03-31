package software.wings.sm.states.k8s;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.k8s.manifest.ManifestHelper.values_filename;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.FeatureName.DELEGATE_TAGS_EXTENDED;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.appmanifest.AppManifestKind.K8S_MANIFEST;
import static software.wings.beans.appmanifest.StoreType.KustomizeSourceRepo;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;
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
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
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
import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.deployment.InstanceDetails;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.model.K8sPod;
import io.harness.rule.Owner;
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
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Account;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.common.VariableProcessor;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.helpers.ext.helm.HelmConstants;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sInstanceSyncResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.helpers.ext.kustomize.KustomizeHelper;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.EventEmitter;
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
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.ApplicationManifestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

  @Inject @InjectMocks private K8sStateHelper k8sStateHelper;

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
    Application application = anApplication().appId(APP_ID).name(APP_NAME).accountId(ACCOUNT_ID).uuid(APP_ID).build();
    when(appService.getApplicationWithDefaults(APP_ID)).thenReturn(application);
    when(appService.get(any())).thenReturn(application);
    when(environmentService.get(APP_ID, ENV_ID, false))
        .thenReturn(Environment.Builder.anEnvironment()
                        .appId(APP_ID)
                        .environmentType(EnvironmentType.PROD)
                        .uuid(ENV_ID)
                        .build());
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testCreateK8sActivity() {
    List<CommandUnit> commandUnits = new ArrayList<>();
    commandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Init));
    commandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Scale));

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
    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .kind(AppManifestKind.K8S_MANIFEST)
                                                  .storeType(StoreType.Local)
                                                  .serviceId(SERVICE_ID)
                                                  .build();
    applicationManifest.setAppId(APP_ID);
    applicationManifest.setUuid(APPLICATION_MANIFEST_ID);

    ManifestFile manifestFile = ManifestFile.builder()
                                    .applicationManifestId(APPLICATION_MANIFEST_ID)
                                    .fileName(values_filename)
                                    .fileContent(VALUES_YAML_WITH_ARTIFACT_REFERENCE)
                                    .build();

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

    K8sTaskExecutionResponse response =
        K8sTaskExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionResult.CommandExecutionStatus.SUCCESS)
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

    when(featureFlagService.isEnabled(DELEGATE_TAGS_EXTENDED, ACCOUNT_ID)).thenReturn(true);
    k8sStateHelper.getPodList(infrastructureMapping, "default", "releaseName");
    captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(2)).executeTask(captor.capture());
    delegateTask = captor.getValue();
    assertThat(delegateTask.getTags()).isEmpty();

    settingAttribute.setValue(KubernetesClusterConfig.builder().build());
    wingsPersistence.save(settingAttribute);
    k8sStateHelper.getPodList(infrastructureMapping, "default", "releaseName");
    captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(3)).executeTask(captor.capture());
    delegateTask = captor.getValue();
    assertThat(delegateTask.getTags()).isEmpty();

    settingAttribute.setValue(
        KubernetesClusterConfig.builder().useKubernetesDelegate(true).delegateName("delegateName").build());
    wingsPersistence.save(settingAttribute);
    k8sStateHelper.getPodList(infrastructureMapping, "default", "releaseName");
    captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService, times(4)).executeTask(captor.capture());
    delegateTask = captor.getValue();
    assertThat(delegateTask.getTags()).contains("delegateName");
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
    when(serviceResourceService.getHelmVersionWithDefault(anyString(), anyString()))
        .thenReturn(HelmConstants.HelmVersion.V2);

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
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testTagsInQueueK8sDelegateTask() throws Exception {
    DirectKubernetesInfrastructureMapping infrastructureMapping =
        DirectKubernetesInfrastructureMapping.builder().namespace("env").accountId(ACCOUNT_ID).build();
    infrastructureMapping.setComputeProviderSettingId(SETTING_ID);

    K8sTaskExecutionResponse response =
        K8sTaskExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionResult.CommandExecutionStatus.SUCCESS)
            .k8sTaskResponse(K8sInstanceSyncResponse.builder().build())
            .build();
    when(delegateService.executeTask(any())).thenReturn(response);
    when(serviceTemplateHelper.fetchServiceTemplateId(any())).thenReturn(SETTING_ID);
    when(infrastructureMappingService.get(anyString(), anyString())).thenReturn(infrastructureMapping);
    when(evaluator.substitute(anyString(), any(), any(), anyString())).thenReturn("default");
    when(serviceResourceService.getHelmVersionWithDefault(anyString(), anyString()))
        .thenReturn(HelmConstants.HelmVersion.V2);

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

    when(featureFlagService.isEnabled(DELEGATE_TAGS_EXTENDED, ACCOUNT_ID)).thenReturn(true);
    settingAttribute.setValue(
        KubernetesClusterConfig.builder().useKubernetesDelegate(true).delegateName("delegateName").build());
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

    when(featureFlagService.isEnabled(DELEGATE_TAGS_EXTENDED, ACCOUNT_ID)).thenReturn(true);
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
        Arrays.asList(K8sPod.builder().name("pod-1").build(), K8sPod.builder().name("pod-2").newPod(true).build()));

    assertThat(newPods).hasSize(1);
    assertThat(newPods.get(0).isNewPod()).isTrue();
    assertThat(newPods.get(0).getName()).isEqualTo("pod-2");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetInstanceDetails() {
    assertThat(k8sStateHelper.getInstanceDetails(null)).isEmpty();
    assertThat(k8sStateHelper.getInstanceDetails(emptyList())).isEmpty();

    final List<InstanceDetails> instanceDetails =
        k8sStateHelper.getInstanceDetails(Arrays.asList(K8sPod.builder().name("pod-1").podIP("ip-1").build(),
            K8sPod.builder().name("pod-2").podIP("ip-2").newPod(true).build()));

    assertThat(instanceDetails).hasSize(2);
    assertThat(instanceDetails.stream().map(InstanceDetails::getInstanceType).collect(toSet()))
        .hasSize(1)
        .contains(InstanceDetails.InstanceType.K8s);
    assertThat(instanceDetails.stream().map(pod -> pod.getK8s().getPodName()).collect(toList()))
        .containsExactlyInAnyOrder("pod-1", "pod-2");
    assertThat(instanceDetails.stream().map(pod -> pod.getK8s().getIp()).collect(toList()))
        .containsExactlyInAnyOrder("ip-1", "ip-2");
    assertThat(instanceDetails.stream().filter(InstanceDetails::isNewInstance).count()).isEqualTo(1);
  }
}
