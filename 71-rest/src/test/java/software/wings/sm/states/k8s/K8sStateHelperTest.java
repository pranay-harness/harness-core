package software.wings.sm.states.k8s;

import static io.harness.k8s.manifest.ManifestHelper.values_filename;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static java.util.Collections.emptyList;
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
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.StepType.K8S_SCALE;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.sm.states.k8s.K8sScale.K8S_SCALE_COMMAND_NAME;
import static software.wings.sm.states.k8s.K8sTestConstants.VALUES_YAML_WITH_ARTIFACT_REFERENCE;
import static software.wings.sm.states.k8s.K8sTestConstants.VALUES_YAML_WITH_COMMENTED_ARTIFACT_REFERENCE;
import static software.wings.sm.states.k8s.K8sTestConstants.VALUES_YAML_WITH_NO_ARTIFACT_REFERENCE;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.STATE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
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
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.service.impl.EventEmitter;
import software.wings.service.impl.GitFileConfigHelperService;
import software.wings.service.impl.HelmChartConfigHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.ApplicationManifestUtils;

import java.util.ArrayList;
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

  @Inject @InjectMocks private K8sStateHelper k8sStateHelper;

  private static final String APPLICATION_MANIFEST_ID = "AppManifestId";

  private ExecutionContextImpl context;
  private WorkflowStandardParams workflowStandardParams =
      aWorkflowStandardParams().withAppId(APP_ID).withEnvId(ENV_ID).build();
  private StateExecutionInstance stateExecutionInstance =
      aStateExecutionInstance()
          .displayName(STATE_NAME)
          .addContextElement(workflowStandardParams)
          .addStateExecutionData(K8sStateExecutionData.builder().build())
          .build();

  @Before
  public void setup() {
    context = new ExecutionContextImpl(stateExecutionInstance);
    on(workflowStandardParams).set("appService", appService);
    on(workflowStandardParams).set("environmentService", environmentService);

    when(appService.getApplicationWithDefaults(APP_ID))
        .thenReturn(anApplication().appId(APP_ID).name(APP_NAME).uuid(APP_ID).build());
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
    wingsPersistence.save(createGCPInfraMapping());

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
      wingsPersistence.delete(InfrastructureMapping.class, INFRA_MAPPING_ID);
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
      wingsPersistence.save(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());
      k8sStateHelper.validateK8sV2TypeServiceUsed(context);

      fail("Should not reach here");
    } catch (InvalidRequestException ex) {
      assertThat(ex.getMessage())
          .isEqualTo("Service SERVICE_NAME used in workflow is of incompatible type. Use Kubernetes V2 type service");
    }

    wingsPersistence.save(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).isK8sV2(true).build());
    k8sStateHelper.validateK8sV2TypeServiceUsed(context);
  }
}
