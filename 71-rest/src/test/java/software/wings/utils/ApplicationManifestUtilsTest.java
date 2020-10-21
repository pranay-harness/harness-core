package software.wings.utils;

import static io.harness.k8s.manifest.ManifestHelper.values_filename;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.RAGHVENDRA;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.appmanifest.AppManifestKind.HELM_CHART_OVERRIDE;
import static software.wings.beans.appmanifest.AppManifestKind.K8S_MANIFEST;
import static software.wings.beans.appmanifest.AppManifestKind.VALUES;
import static software.wings.beans.appmanifest.StoreType.HelmChartRepo;
import static software.wings.beans.appmanifest.StoreType.HelmSourceRepo;
import static software.wings.beans.appmanifest.StoreType.KustomizeSourceRepo;
import static software.wings.beans.appmanifest.StoreType.Local;
import static software.wings.beans.appmanifest.StoreType.Remote;
import static software.wings.helpers.ext.k8s.request.K8sValuesLocation.Environment;
import static software.wings.helpers.ext.k8s.request.K8sValuesLocation.EnvironmentGlobal;
import static software.wings.helpers.ext.k8s.request.K8sValuesLocation.ServiceOverride;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.GitFile;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.GitFetchFilesConfig;
import software.wings.beans.GitFetchFilesTaskParams;
import software.wings.beans.GitFileConfig;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.Service;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ApplicationManifest.ApplicationManifestBuilder;
import software.wings.beans.appmanifest.HelmChart;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.yaml.GitCommandExecutionResponse;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.service.impl.GitFileConfigHelperService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ApplicationManifestUtilsTest extends WingsBaseTest {
  @Mock private DeploymentExecutionContext context;
  @Mock private ApplicationManifestService applicationManifestService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private AppService appService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private GitFileConfigHelperService gitFileConfigHelperService;
  @Mock private FeatureFlagService featureFlagService;

  @Inject @InjectMocks private ApplicationManifestUtils applicationManifestUtils;
  private ApplicationManifestUtils applicationManifestUtilsSpy = spy(new ApplicationManifestUtils());

  @Before
  public void setup() {
    when(context.getContextElement(ContextElementType.PARAM, ExecutionContextImpl.PHASE_PARAM))
        .thenReturn(PhaseElement.builder().serviceElement(ServiceElement.builder().uuid(SERVICE_ID).build()).build());
    when(context.getAppId()).thenReturn(APP_ID);
    when(context.fetchInfraMappingId()).thenReturn(INFRA_MAPPING_ID);
    when(context.getAccountId()).thenReturn(ACCOUNT_ID);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(GcpKubernetesInfrastructureMapping.builder().envId(ENV_ID).build());
    when(appService.get(APP_ID)).thenReturn(anApplication().uuid(APP_ID).build());
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false)).thenReturn(Service.builder().uuid(SERVICE_ID).build());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetAppManifestByApplyingHelmChartOverride() {
    ApplicationManifestUtils utils = spy(ApplicationManifestUtils.class);

    ExecutionContext context = mock(ExecutionContext.class);

    ApplicationManifest applicationManifestAtService = ApplicationManifest.builder()
                                                           .serviceId("1")
                                                           .kind(K8S_MANIFEST)
                                                           .helmChartConfig(HelmChartConfig.builder().build())
                                                           .storeType(Local)
                                                           .envId("2")
                                                           .build();

    doReturn(applicationManifestAtService).when(utils).getApplicationManifestForService(context);
    assertThat(utils.getAppManifestByApplyingHelmChartOverride(context)).isNull();

    applicationManifestAtService.setStoreType(HelmChartRepo);
    Map<K8sValuesLocation, ApplicationManifest> manifestMap = new HashMap<>();
    doReturn(manifestMap).when(utils).getOverrideApplicationManifests(context, HELM_CHART_OVERRIDE);
    assertThat(utils.getAppManifestByApplyingHelmChartOverride(context)).isEqualTo(applicationManifestAtService);

    applicationManifestAtService.setStoreType(HelmSourceRepo);
    ApplicationManifest applicationManifestAtEnv =
        ApplicationManifest.builder()
            .serviceId("1")
            .kind(HELM_CHART_OVERRIDE)
            .storeType(HelmSourceRepo)
            .envId("2")
            .helmChartConfig(HelmChartConfig.builder().connectorId("env-connector").chartName("env-chart").build())
            .build();
    manifestMap.put(Environment, applicationManifestAtEnv);
    ApplicationManifest expectedManifest = applicationManifestAtService.cloneInternal();
    expectedManifest.setHelmChartConfig(HelmChartConfig.builder().build());
    assertThat(utils.getAppManifestByApplyingHelmChartOverride(context).getHelmChartConfig())
        .isEqualTo(expectedManifest.getHelmChartConfig());

    applicationManifestAtService.setStoreType(HelmChartRepo);
    applicationManifestAtService.setHelmChartConfig(
        HelmChartConfig.builder().connectorId("service-connector").chartName("service-chart").build());
    expectedManifest = applicationManifestAtService.cloneInternal();
    expectedManifest.setHelmChartConfig(
        HelmChartConfig.builder().connectorId("env-connector").chartName("service-chart").build());
    manifestMap.get(Environment).setStoreType(HelmChartRepo);
    ApplicationManifest resultManifest = utils.getAppManifestByApplyingHelmChartOverride(context);
    assertThat(resultManifest.getHelmChartConfig()).isEqualTo(expectedManifest.getHelmChartConfig());

    manifestMap.get(Environment).setStoreType(HelmSourceRepo);
    applicationManifestAtService.setStoreType(HelmChartRepo);
    try {
      utils.getAppManifestByApplyingHelmChartOverride(context);
      fail("Invalid Request Exception should occur expected");
    } catch (Exception e) {
      assertThat(e instanceof InvalidRequestException).isTrue();
    }
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testIsKustomizeSource() {
    ApplicationManifest applicationManifest = ApplicationManifest.builder().storeType(KustomizeSourceRepo).build();
    doReturn(applicationManifest).when(applicationManifestUtilsSpy).getApplicationManifestForService(context);
    assertThat(applicationManifestUtilsSpy.isKustomizeSource(context)).isTrue();

    applicationManifest = ApplicationManifest.builder().storeType(HelmSourceRepo).build();
    doReturn(applicationManifest).when(applicationManifestUtilsSpy).getApplicationManifestForService(context);
    assertThat(applicationManifestUtilsSpy.isKustomizeSource(context)).isFalse();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testPopulateValuesFilesFromAppManifest() {
    ApplicationManifest appManifest1 = ApplicationManifest.builder().storeType(Local).kind(VALUES).build();
    appManifest1.setUuid("appManifest1");

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    appManifestMap.put(K8sValuesLocation.ServiceOverride, appManifest1);

    when(applicationManifestService.getManifestFileByFileName("appManifest1", values_filename))
        .thenReturn(ManifestFile.builder().build());

    Map<K8sValuesLocation, Collection<String>> valuesFiles = new HashMap<>();
    applicationManifestUtils.populateValuesFilesFromAppManifest(appManifestMap, valuesFiles);
    assertThat(valuesFiles.size()).isEqualTo(0);

    ApplicationManifest appManifest2 = ApplicationManifest.builder().storeType(Local).kind(VALUES).build();
    appManifest2.setUuid("appManifest2");
    appManifestMap.put(K8sValuesLocation.Environment, appManifest2);
    when(applicationManifestService.getManifestFileByFileName("appManifest2", values_filename))
        .thenReturn(ManifestFile.builder().fileContent("fileContent").build());

    applicationManifestUtils.populateValuesFilesFromAppManifest(appManifestMap, valuesFiles);
    assertThat(valuesFiles.size()).isEqualTo(1);
    assertThat(valuesFiles.get(K8sValuesLocation.Environment)).containsExactly("fileContent");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testPopulateMultipleValuesFileFromAppManifest() {
    ApplicationManifest appManifest1 = ApplicationManifest.builder().storeType(Local).kind(VALUES).build();
    appManifest1.setUuid("appManifest1");
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new HashMap<>();
    appManifestMap.put(ServiceOverride, appManifest1);

    Map<K8sValuesLocation, Collection<String>> valuesFiles = new HashMap<>();
    doReturn(ManifestFile.builder().build())
        .when(applicationManifestService)
        .getManifestFileByFileName("appManifest1", values_filename);
    applicationManifestUtils.populateValuesFilesFromAppManifest(appManifestMap, valuesFiles);
    assertThat(valuesFiles).isEmpty();

    valuesFiles = new HashMap<>();
    doReturn(ManifestFile.builder().fileContent("content").build())
        .when(applicationManifestService)
        .getManifestFileByFileName("appManifest1", values_filename);
    applicationManifestUtils.populateValuesFilesFromAppManifest(appManifestMap, valuesFiles);
    assertThat(valuesFiles.keySet()).containsExactlyInAnyOrder(K8sValuesLocation.ServiceOverride);
    assertThat(valuesFiles.values()).containsExactlyInAnyOrder(singletonList("content"));

    valuesFiles = new HashMap<>();
    ApplicationManifest appManifest2 = ApplicationManifest.builder().storeType(Local).kind(VALUES).build();
    appManifest2.setUuid("appManifest2");
    appManifestMap.put(Environment, appManifest2);
    doReturn(ManifestFile.builder().fileContent("content1").build())
        .when(applicationManifestService)
        .getManifestFileByFileName("appManifest1", values_filename);
    doReturn(ManifestFile.builder().fileContent("content2").build())
        .when(applicationManifestService)
        .getManifestFileByFileName("appManifest2", values_filename);
    applicationManifestUtils.populateValuesFilesFromAppManifest(appManifestMap, valuesFiles);
    assertThat(valuesFiles.keySet()).containsExactlyInAnyOrder(ServiceOverride, Environment);
    assertThat(valuesFiles.values()).containsExactlyInAnyOrder(singletonList("content1"), singletonList("content2"));

    valuesFiles = new HashMap<>();
    doReturn(null).when(applicationManifestService).getManifestFileByFileName("appManifest1", values_filename);
    doReturn(ManifestFile.builder().fileContent("content").build())
        .when(applicationManifestService)
        .getManifestFileByFileName("appManifest2", values_filename);
    applicationManifestUtils.populateValuesFilesFromAppManifest(appManifestMap, valuesFiles);
    assertThat(valuesFiles.keySet()).containsExactlyInAnyOrder(Environment);
    assertThat(valuesFiles.values()).containsExactlyInAnyOrder(singletonList("content"));
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testApplyEnvGlobalHelmChartOverrideIfPresent() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new EnumMap<>(K8sValuesLocation.class);
    ApplicationManifest serviceManifest = ApplicationManifest.builder()
                                              .storeType(HelmChartRepo)
                                              .serviceId("serviceId")
                                              .helmChartConfig(HelmChartConfig.builder()
                                                                   .connectorId("service-connector")
                                                                   .chartVersion("1.1")
                                                                   .chartName("service-chart")
                                                                   .basePath("/base")
                                                                   .build())
                                              .build();
    appManifestMap.put(K8sValuesLocation.Service, serviceManifest);
    appManifestMap.put(K8sValuesLocation.EnvironmentGlobal,
        ApplicationManifest.builder()
            .storeType(HelmChartRepo)
            .envId("envId")
            .helmChartConfig(HelmChartConfig.builder().chartName("global-chart").connectorId("env-connector").build())
            .build());
    if (appManifestMap.containsKey(K8sValuesLocation.EnvironmentGlobal)
        && HelmChartRepo == serviceManifest.getStoreType()) {
      applicationManifestUtils.applyK8sValuesLocationBasedHelmChartOverride(
          serviceManifest, appManifestMap, EnvironmentGlobal);
    }

    assertThat(serviceManifest.getHelmChartConfig().getConnectorId()).isEqualTo("env-connector");
    assertThat(serviceManifest.getHelmChartConfig().getChartVersion()).isEqualTo("1.1");
    assertThat(serviceManifest.getHelmChartConfig().getChartName()).isEqualTo("service-chart");
    assertThat(serviceManifest.getHelmChartConfig().getBasePath()).isEqualTo("/base");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testApplyHelmChartOverrideWithManifestEnvPresent() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new EnumMap<>(K8sValuesLocation.class);
    ApplicationManifest serviceManifest = ApplicationManifest.builder()
                                              .storeType(HelmChartRepo)
                                              .serviceId("serviceId")
                                              .helmChartConfig(HelmChartConfig.builder()
                                                                   .connectorId("service-connector")
                                                                   .chartVersion("1.1")
                                                                   .chartName("service-chart")
                                                                   .basePath("/base")
                                                                   .build())
                                              .build();
    appManifestMap.put(K8sValuesLocation.Service, serviceManifest);
    appManifestMap.put(K8sValuesLocation.EnvironmentGlobal,
        ApplicationManifest.builder()
            .storeType(HelmChartRepo)
            .envId("envId")
            .helmChartConfig(
                HelmChartConfig.builder().chartName("global-chart").connectorId("global-connector").build())
            .build());
    appManifestMap.put(Environment,
        ApplicationManifest.builder()
            .storeType(HelmChartRepo)
            .envId("envId")
            .helmChartConfig(HelmChartConfig.builder().chartName("env-chart").connectorId("env-connector").build())
            .build());
    applicationManifestUtils.applyHelmChartOverride(serviceManifest, appManifestMap);

    assertThat(serviceManifest.getHelmChartConfig().getConnectorId()).isEqualTo("env-connector");
    assertThat(serviceManifest.getHelmChartConfig().getChartName()).isEqualTo("service-chart");
    assertThat(serviceManifest.getHelmChartConfig().getChartVersion()).isEqualTo("1.1");
    assertThat(serviceManifest.getHelmChartConfig().getBasePath()).isEqualTo("/base");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testApplyHelmChartOverrideWithAllThreeManifestPresent() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new EnumMap<>(K8sValuesLocation.class);
    ApplicationManifest serviceManifest = ApplicationManifest.builder()
                                              .storeType(HelmChartRepo)
                                              .serviceId("serviceId")
                                              .helmChartConfig(HelmChartConfig.builder()
                                                                   .connectorId("service-connector")
                                                                   .chartVersion("1.1")
                                                                   .chartName("service-chart")
                                                                   .basePath("/base")
                                                                   .build())
                                              .build();
    appManifestMap.put(K8sValuesLocation.Service, serviceManifest);
    appManifestMap.put(K8sValuesLocation.EnvironmentGlobal,
        ApplicationManifest.builder()
            .storeType(HelmChartRepo)
            .envId("envId")
            .helmChartConfig(
                HelmChartConfig.builder().chartName("global-chart").connectorId("global-connector").build())
            .build());
    appManifestMap.put(Environment,
        ApplicationManifest.builder()
            .storeType(HelmChartRepo)
            .envId("envId")
            .helmChartConfig(HelmChartConfig.builder().chartName("env-chart").connectorId("env-connector").build())
            .build());
    applicationManifestUtils.applyHelmChartOverride(serviceManifest, appManifestMap);

    assertThat(serviceManifest.getHelmChartConfig().getConnectorId()).isEqualTo("env-connector");
    assertThat(serviceManifest.getHelmChartConfig().getChartName()).isEqualTo("service-chart");
    assertThat(serviceManifest.getHelmChartConfig().getChartVersion()).isEqualTo("1.1");
    assertThat(serviceManifest.getHelmChartConfig().getBasePath()).isEqualTo("/base");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testApplyHelmChartOverrideWithManifestEnvNotPresentGlobalPresent() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new EnumMap<>(K8sValuesLocation.class);
    ApplicationManifest serviceManifest = ApplicationManifest.builder()
                                              .storeType(HelmChartRepo)
                                              .serviceId("serviceId")
                                              .helmChartConfig(HelmChartConfig.builder()
                                                                   .connectorId("service-connector")
                                                                   .chartVersion("1.1")
                                                                   .chartName("service-chart")
                                                                   .basePath("/base")
                                                                   .build())
                                              .build();
    appManifestMap.put(K8sValuesLocation.Service, serviceManifest);
    appManifestMap.put(K8sValuesLocation.EnvironmentGlobal,
        ApplicationManifest.builder()
            .storeType(HelmChartRepo)
            .envId("envId")
            .helmChartConfig(
                HelmChartConfig.builder().chartName("global-chart").connectorId("global-connector").build())
            .build());
    applicationManifestUtils.applyHelmChartOverride(serviceManifest, appManifestMap);

    assertThat(serviceManifest.getHelmChartConfig().getConnectorId()).isEqualTo("global-connector");
    assertThat(serviceManifest.getHelmChartConfig().getChartName()).isEqualTo("service-chart");
    assertThat(serviceManifest.getHelmChartConfig().getChartVersion()).isEqualTo("1.1");
    assertThat(serviceManifest.getHelmChartConfig().getBasePath()).isEqualTo("/base");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testApplyHelmChartOverrideWithManifestGlobalNotPresentEnvPresent() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new EnumMap<>(K8sValuesLocation.class);
    ApplicationManifest serviceManifest = ApplicationManifest.builder()
                                              .storeType(HelmChartRepo)
                                              .serviceId("serviceId")
                                              .helmChartConfig(HelmChartConfig.builder()
                                                                   .connectorId("service-connector")
                                                                   .chartVersion("1.1")
                                                                   .chartName("service-chart")
                                                                   .build())
                                              .build();
    appManifestMap.put(K8sValuesLocation.Service, serviceManifest);
    appManifestMap.put(Environment,
        ApplicationManifest.builder()
            .storeType(HelmChartRepo)
            .envId("envId")
            .helmChartConfig(HelmChartConfig.builder().chartName("env-chart").connectorId("env-connector").build())
            .build());
    applicationManifestUtils.applyHelmChartOverride(serviceManifest, appManifestMap);

    assertThat(serviceManifest.getHelmChartConfig().getConnectorId()).isEqualTo("env-connector");
    assertThat(serviceManifest.getHelmChartConfig().getChartName()).isEqualTo("service-chart");
    assertThat(serviceManifest.getHelmChartConfig().getChartVersion()).isEqualTo("1.1");
    assertThat(serviceManifest.getHelmChartConfig().getBasePath()).isNullOrEmpty();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testApplyEnvGlobalHelmChartOverrideIfNotPresent() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new EnumMap<>(K8sValuesLocation.class);
    ApplicationManifest serviceManifest = ApplicationManifest.builder()
                                              .storeType(HelmChartRepo)
                                              .serviceId("serviceId")
                                              .helmChartConfig(HelmChartConfig.builder()
                                                                   .connectorId("service-connector")
                                                                   .chartVersion("1.1")
                                                                   .chartName("etcd")
                                                                   .basePath("/base")
                                                                   .build())
                                              .build();
    appManifestMap.put(K8sValuesLocation.Service, serviceManifest);
    if (appManifestMap.containsKey(K8sValuesLocation.EnvironmentGlobal)
        && HelmChartRepo == serviceManifest.getStoreType()) {
      applicationManifestUtils.applyK8sValuesLocationBasedHelmChartOverride(
          serviceManifest, appManifestMap, EnvironmentGlobal);
    }

    assertThat(serviceManifest.getHelmChartConfig().getConnectorId()).isEqualTo("service-connector");
    assertThat(serviceManifest.getHelmChartConfig().getChartVersion()).isEqualTo("1.1");
    assertThat(serviceManifest.getHelmChartConfig().getChartName()).isEqualTo("etcd");
    assertThat(serviceManifest.getHelmChartConfig().getBasePath()).isEqualTo("/base");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testApplyEnvGlobalHelmChartOverrideIfHelmSourceRepoService() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = new EnumMap<>(K8sValuesLocation.class);
    ApplicationManifest serviceManifest = ApplicationManifest.builder()
                                              .storeType(HelmSourceRepo)
                                              .serviceId("serviceId")
                                              .gitFileConfig(GitFileConfig.builder()
                                                                 .connectorName("git")
                                                                 .branch("master")
                                                                 .connectorId("connector-id")
                                                                 .filePath("a/b")
                                                                 .useBranch(true)
                                                                 .build())
                                              .build();
    appManifestMap.put(K8sValuesLocation.Service, serviceManifest);
    if (appManifestMap.containsKey(K8sValuesLocation.EnvironmentGlobal)
        && HelmChartRepo == serviceManifest.getStoreType()) {
      applicationManifestUtils.applyK8sValuesLocationBasedHelmChartOverride(
          serviceManifest, appManifestMap, EnvironmentGlobal);
    }

    assertThat(serviceManifest.getGitFileConfig().getConnectorId()).isEqualTo("connector-id");
    assertThat(serviceManifest.getGitFileConfig().getConnectorName()).isEqualTo("git");
    assertThat(serviceManifest.getGitFileConfig().getBranch()).isEqualTo("master");
    assertThat(serviceManifest.getGitFileConfig().isUseBranch()).isTrue();
    assertThat(serviceManifest.getHelmChartConfig()).isNull();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getOverrideApplicationManifests() {
    ApplicationManifest serviceManifest = ApplicationManifest.builder()
                                              .serviceId(SERVICE_ID)
                                              .storeType(HelmChartRepo)
                                              .kind(K8S_MANIFEST)
                                              .helmChartConfig(HelmChartConfig.builder()
                                                                   .connectorId("svc-connector")
                                                                   .chartVersion("3.1")
                                                                   .chartName("test-chart")
                                                                   .build())
                                              .build();
    ApplicationManifest envServiceManifest = ApplicationManifest.builder()
                                                 .serviceId(SERVICE_ID)
                                                 .envId(ENV_ID)
                                                 .storeType(HelmChartRepo)
                                                 .kind(HELM_CHART_OVERRIDE)
                                                 .helmChartConfig(HelmChartConfig.builder()
                                                                      .connectorId("env-svc-connector")
                                                                      .chartVersion("4.1")
                                                                      .chartName("test-chart")
                                                                      .build())
                                                 .build();
    ApplicationManifest envGlobalManifest =
        ApplicationManifest.builder()
            .envId(ENV_ID)
            .storeType(HelmChartRepo)
            .kind(HELM_CHART_OVERRIDE)
            .helmChartConfig(HelmChartConfig.builder().connectorId("env-connector").build())
            .build();

    when(applicationManifestService.getByServiceId(APP_ID, SERVICE_ID, K8S_MANIFEST)).thenReturn(serviceManifest);
    when(applicationManifestService.getByEnvId(APP_ID, ENV_ID, HELM_CHART_OVERRIDE)).thenReturn(envGlobalManifest);
    when(applicationManifestService.getByEnvAndServiceId(APP_ID, ENV_ID, SERVICE_ID, HELM_CHART_OVERRIDE))
        .thenReturn(envServiceManifest);
    when(applicationManifestService.getManifestByServiceId(APP_ID, SERVICE_ID)).thenReturn(serviceManifest);

    Map<K8sValuesLocation, ApplicationManifest> helmOverrideManifestMap =
        applicationManifestUtils.getOverrideApplicationManifests(context, HELM_CHART_OVERRIDE);
    Map<K8sValuesLocation, ApplicationManifest> k8sManifestMap =
        applicationManifestUtils.getOverrideApplicationManifests(context, K8S_MANIFEST);

    assertThat(helmOverrideManifestMap.get(Environment)).isEqualTo(envServiceManifest);
    assertThat(helmOverrideManifestMap.get(EnvironmentGlobal)).isEqualTo(envGlobalManifest);
    assertThat(k8sManifestMap.get(ServiceOverride)).isEqualTo(serviceManifest);

    Map<K8sValuesLocation, ApplicationManifest> allManifests =
        applicationManifestUtils.getApplicationManifests(context, K8S_MANIFEST);
    assertThat(allManifests.get(K8sValuesLocation.Service)).isEqualTo(serviceManifest);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testIsValuesInGit() {
    Map<K8sValuesLocation, ApplicationManifest> manifestMap = new EnumMap<>(K8sValuesLocation.class);
    assertThat(applicationManifestUtils.isValuesInGit(manifestMap)).isFalse();

    manifestMap.put(ServiceOverride, ApplicationManifest.builder().storeType(HelmChartRepo).build());
    assertThat(applicationManifestUtils.isValuesInGit(manifestMap)).isFalse();

    manifestMap.put(Environment, ApplicationManifest.builder().storeType(HelmSourceRepo).build());
    assertThat(applicationManifestUtils.isValuesInGit(manifestMap)).isTrue();

    manifestMap.put(Environment, ApplicationManifest.builder().storeType(Remote).build());
    assertThat(applicationManifestUtils.isValuesInGit(manifestMap)).isTrue();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testIsValuesInHelmChartRepo() {
    ApplicationManifestUtils manifestUtils = Mockito.spy(applicationManifestUtils);
    doReturn(ApplicationManifest.builder()
                 .storeType(HelmChartRepo)
                 .helmChartConfig(HelmChartConfig.builder().chartName("test").build())
                 .build())
        .when(manifestUtils)
        .getApplicationManifestForService(context);

    assertThat(manifestUtils.isValuesInHelmChartRepo(context)).isTrue();

    doReturn(null).when(manifestUtils).getApplicationManifestForService(context);
    assertThat(manifestUtils.isValuesInHelmChartRepo(context)).isFalse();

    doReturn(ApplicationManifest.builder().storeType(HelmSourceRepo).build())
        .when(manifestUtils)
        .getApplicationManifestForService(context);

    assertThat(manifestUtils.isValuesInHelmChartRepo(context)).isFalse();

    doReturn(ApplicationManifest.builder()
                 .storeType(HelmChartRepo)
                 .helmChartConfig(HelmChartConfig.builder().build())
                 .build())
        .when(manifestUtils)
        .getApplicationManifestForService(context);

    assertThat(manifestUtils.isValuesInHelmChartRepo(context)).isFalse();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetMultiValuesFilesFromGitFetchFilesResponse() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap =
        ImmutableMap.of(Environment, createApplicationManifestWithGitFilePathList("file1"), K8sValuesLocation.Service,
            createApplicationManifestWithGitFile("file2"), EnvironmentGlobal,
            createApplicationManifestWithGitFilePathList(), ServiceOverride,
            createApplicationManifestWithGitFilePathList("file4"));

    GitCommandExecutionResponse executionResponse = gitExecutionResponseWithFilesFromMultipleRepo(ImmutableMap.of(
        "Environment", ImmutableMap.of("file1", "content1"), "Service", ImmutableMap.of("file2", "content2"),
        "EnvironmentGlobal", ImmutableMap.of(), "ServiceOverride", ImmutableMap.of("file4", "content4")));

    Map<K8sValuesLocation, Collection<String>> valuesFiles =
        applicationManifestUtils.getValuesFilesFromGitFetchFilesResponse(appManifestMap, executionResponse);

    assertThat(valuesFiles.get(Environment)).containsExactly("content1");
    assertThat(valuesFiles.get(K8sValuesLocation.Service)).containsExactly("content2");
    assertThat(valuesFiles.get(EnvironmentGlobal)).isNullOrEmpty();
    assertThat(valuesFiles.get(ServiceOverride)).containsExactly("content4");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetMultiValuesFilesFromGitFetchFilesResponseUnordered() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap =
        ImmutableMap.of(Environment, createApplicationManifestWithGitFilePathList("file1", "file2", "file3"),
            K8sValuesLocation.Service, createApplicationManifestWithGitFilePathList(), EnvironmentGlobal,
            createApplicationManifestWithGitFilePathList("file4", "file5"), ServiceOverride,
            createApplicationManifestWithGitFilePathList("file6"));

    GitCommandExecutionResponse executionResponse = gitExecutionResponseWithFilesFromMultipleRepo(
        ImmutableMap.of("Environment", ImmutableMap.of("file3", "content3", "file1", "content1", "file2", "content2"),
            "EnvironmentGlobal", ImmutableMap.of("file5", "content5", "file4", "content4"), "ServiceOverride",
            ImmutableMap.of("file6", "content6")));

    Map<K8sValuesLocation, Collection<String>> valuesFiles =
        applicationManifestUtils.getValuesFilesFromGitFetchFilesResponse(appManifestMap, executionResponse);

    assertThat(valuesFiles.get(Environment)).containsExactly("content1", "content2", "content3");
    assertThat(valuesFiles.get(K8sValuesLocation.Service)).isNullOrEmpty();
    assertThat(valuesFiles.get(EnvironmentGlobal)).containsExactly("content4", "content5");
    assertThat(valuesFiles.get(ServiceOverride)).containsExactly("content6");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetMultiValueFilesFromGitFetchFilesReponseWithFolder() {
    testGetMultiValuesFromGitFetchFilesResponseWith(
        createApplicationManifestWithGitFilePathList("file1", "folder1", "folder2"),
        ImmutableMap.of("file1", "content1", "folder1/file1", "folder1/content1", "folder1/file2", "folder1/content2",
            "folder2/file1", "folder2/content1"),
        asList("content1", "folder1/content1", "folder2/content1"));

    testGetMultiValuesFromGitFetchFilesResponseWith(
        createApplicationManifestWithGitFilePathList("folder1", "folder1/subfolder"),
        ImmutableMap.of("folder1/file1", "folder1/content", "folder1/subfolder/file1", "folder1/subcontent"),
        asList("folder1/content", "folder1/subcontent"));

    testGetMultiValuesFromGitFetchFilesResponseWith(
        createApplicationManifestWithGitFilePathList("folder1", "file1", "folder1/subfolder"),
        ImmutableMap.of("file1", "content", "folder1/subfolder/file1", "folder1/subfolder/content"),
        asList("folder1/subfolder/content", "content", "folder1/subfolder/content"));

    testGetMultiValuesFromGitFetchFilesResponseWith(createApplicationManifestWithGitFilePathList("subfolder", "file1"),
        ImmutableMap.of("folder/subfolder/file1", "folder/subfolder/content", "file1", "file1-content"),
        singletonList("file1-content"));
  }

  private void testGetMultiValuesFromGitFetchFilesResponseWith(
      ApplicationManifest manifest, Map<String, String> response, List<String> expected) {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = ImmutableMap.of(ServiceOverride, manifest);
    GitCommandExecutionResponse executionResponse =
        gitExecutionResponseWithFilesFromMultipleRepo(ImmutableMap.of("ServiceOverride", response));

    Map<K8sValuesLocation, Collection<String>> valuesFiles =
        applicationManifestUtils.getValuesFilesFromGitFetchFilesResponse(appManifestMap, executionResponse);

    assertThat(valuesFiles.get(ServiceOverride)).isEqualTo(expected);
  }

  private ApplicationManifest createApplicationManifestWithGitFilePathList(String... files) {
    return ApplicationManifest.builder()
        .storeType(Remote)
        .gitFileConfig(GitFileConfig.builder().filePathList(asList(files)).build())
        .build();
  }

  private GitCommandExecutionResponse gitExecutionResponseWithFilesFromMultipleRepo(
      Map<String, Map<String, String>> multiRepoFiles) {
    Map<String, GitFetchFilesResult> filesFromMultipleRepo = new HashMap<>();
    multiRepoFiles.forEach((key, value) -> filesFromMultipleRepo.put(key, getGitFetchFilesResult(value)));
    return GitCommandExecutionResponse.builder()
        .gitCommandResult(
            GitFetchFilesFromMultipleRepoResult.builder().filesFromMultipleRepo(filesFromMultipleRepo).build())
        .build();
  }

  private GitFetchFilesResult getGitFetchFilesResult(Map<String, String> filesMap) {
    List<GitFile> fileList = filesMap.entrySet()
                                 .stream()
                                 .map(e -> GitFile.builder().filePath(e.getKey()).fileContent(e.getValue()).build())
                                 .collect(toList());
    return GitFetchFilesResult.builder().files(fileList).build();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testPopulateRemoteGitConfigFilePathList() {
    ApplicationManifest singleFile = createApplicationManifestWithGitFile("file");
    ApplicationManifest multipleFiles = createApplicationManifestWithGitFile("file1, file2, file3");
    ApplicationManifest singleFileExpression = createApplicationManifestWithGitFile("${expression}");
    ApplicationManifest multipleFilesExpression =
        createApplicationManifestWithGitFile("${expression1}, ${expression2}, ${expression3}");
    ApplicationManifest multipleTrailingComma = createApplicationManifestWithGitFile(",file1,file2,file3,");
    ApplicationManifest multipleFilesSingleExpression = createApplicationManifestWithGitFile("${multipleFilePathExpr}");

    testPopulateRemoteGitConfigFilePathListWith(Environment, singleFile, "file");
    testPopulateRemoteGitConfigFilePathListWith(ServiceOverride, multipleFiles, "file1", "file2", "file3");
    testPopulateRemoteGitConfigFilePathListWith(EnvironmentGlobal, singleFileExpression, "file");
    testPopulateRemoteGitConfigFilePathListWith(Environment, multipleFilesExpression, "file1", "file2", "file3");
    testPopulateRemoteGitConfigFilePathListWith(Environment, multipleTrailingComma, "file1", "file2", "file3");
    testPopulateRemoteGitConfigFilePathListWith(
        ServiceOverride, multipleFilesSingleExpression, "file1", "file2", "file3");

    // Should not update file config for Service manifest
    ApplicationManifest serviceManifest = createApplicationManifestWithGitFile("file1, file2, file3");
    Map<K8sValuesLocation, ApplicationManifest> serviceAppManifestMap =
        ImmutableMap.of(K8sValuesLocation.Service, serviceManifest);
    applicationManifestUtils.populateRemoteGitConfigFilePathList(context, serviceAppManifestMap);
    assertThat(serviceAppManifestMap.get(K8sValuesLocation.Service).getGitFileConfig().getFilePath())
        .isEqualTo("file1, file2, file3");
  }

  private void testPopulateRemoteGitConfigFilePathListWith(
      K8sValuesLocation location, ApplicationManifest manifest, String... expectedFiles) {
    doReturn("file").when(context).renderExpression("${expression}");
    doReturn("file1, file2, file3").when(context).renderExpression("${expression1},${expression2},${expression3}");
    doReturn("file3").when(context).renderExpression("${expression3}");
    doReturn("file1, file2, file3").when(context).renderExpression("${multipleFilePathExpr}");
    doReturn("file1, file2, file3").when(context).renderExpression("file1,file2,file3");
    doReturn("file").when(context).renderExpression("file");

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = ImmutableMap.of(location, manifest);
    applicationManifestUtils.populateRemoteGitConfigFilePathList(context, appManifestMap);
    assertThat(appManifestMap.get(location).getGitFileConfig().getFilePathList()).containsExactly(expectedFiles);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testPopulateRemoteGitConfigFilePathListInvalidExpressions() {
    ApplicationManifest emptyExpression = createApplicationManifestWithGitFile("${empty}");
    ApplicationManifest withValidAndEmptyExpr = createApplicationManifestWithGitFile("${valid}, ${empty}");
    ApplicationManifest nullExpression = createApplicationManifestWithGitFile("${null}");
    ApplicationManifest withValidAndNullExpr = createApplicationManifestWithGitFile("${valid}, ${null}");

    testPopulateRemoteGitConfigFilePathListInvalidExpressionsWith(Environment, emptyExpression);
    testPopulateRemoteGitConfigFilePathListInvalidExpressionsWith(ServiceOverride, withValidAndEmptyExpr);
    testPopulateRemoteGitConfigFilePathListInvalidExpressionsWith(EnvironmentGlobal, nullExpression);
    testPopulateRemoteGitConfigFilePathListInvalidExpressionsWith(Environment, withValidAndNullExpr);
  }

  public void testPopulateRemoteGitConfigFilePathListInvalidExpressionsWith(
      K8sValuesLocation location, ApplicationManifest manifest) {
    doReturn("").when(context).renderExpression("${empty}");
    doReturn("file, ").when(context).renderExpression("${valid},${empty}");
    doReturn("null").when(context).renderExpression("${null}");
    doReturn("file,null").when(context).renderExpression("${valid},${null}");

    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = ImmutableMap.of(location, manifest);
    assertThatThrownBy(() -> applicationManifestUtils.populateRemoteGitConfigFilePathList(context, appManifestMap))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Invalid file path");
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetMultiValuesFilesFromGitFetchFilesResponseWithEmptyFileContent() {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap =
        ImmutableMap.of(Environment, createApplicationManifestWithGitFilePathList("file1"), K8sValuesLocation.Service,
            createApplicationManifestWithGitFile("file2"), ServiceOverride,
            createApplicationManifestWithGitFilePathList("file3"), EnvironmentGlobal,
            createApplicationManifestWithGitFilePathList());

    GitCommandExecutionResponse executionResponse =
        gitExecutionResponseWithFilesFromMultipleRepo(ImmutableMap.of("Environment",
            ImmutableMap.of("file1", "content1"), "Service", ImmutableMap.of("file2", "   ", "ignore", "ignore"),
            "EnvironmentGlobal", ImmutableMap.of(), "ServiceOverride", ImmutableMap.of("file3", "  ")));

    Map<K8sValuesLocation, Collection<String>> valuesFiles =
        applicationManifestUtils.getValuesFilesFromGitFetchFilesResponse(appManifestMap, executionResponse);

    assertThat(valuesFiles.get(Environment)).containsExactly("content1");
    assertThat(valuesFiles.get(EnvironmentGlobal)).isNullOrEmpty();
    assertThat(valuesFiles.get(K8sValuesLocation.Service)).containsExactly("   ");

    assertThat(valuesFiles.get(ServiceOverride)).containsExactly("  ");
  }

  private ApplicationManifest createApplicationManifestWithGitFile(String filePath) {
    return ApplicationManifest.builder()
        .storeType(Remote)
        .gitFileConfig(GitFileConfig.builder().filePath(filePath).build())
        .build();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testPopulateRemoteGitConfigWithNonGitConfig() {
    ApplicationManifest inlineManifest =
        ApplicationManifest.builder().storeType(Local).serviceId(SERVICE_ID).envId(ENV_ID).build();
    ApplicationManifest manifestWithGitConfig = createApplicationManifestWithGitFile("file");
    Map<K8sValuesLocation, ApplicationManifest> appManifestOnlyInline = ImmutableMap.of(Environment, inlineManifest);
    Map<K8sValuesLocation, ApplicationManifest> appManifestMixed =
        ImmutableMap.of(Environment, inlineManifest, EnvironmentGlobal, manifestWithGitConfig);

    applicationManifestUtils.populateRemoteGitConfigFilePathList(context, appManifestOnlyInline);
    assertThat(inlineManifest.getGitFileConfig()).isNull();

    doReturn("file").when(context).renderExpression("file");
    applicationManifestUtils.populateRemoteGitConfigFilePathList(context, appManifestMixed);
    assertThat(inlineManifest.getGitFileConfig()).isNull();
    assertThat(manifestWithGitConfig.getGitFileConfig().getFilePathList()).containsExactly("file");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testSetValuesPathInGitFetchFilesTaskParams() {
    GitFetchFilesTaskParams gitFetchFilesTaskParams =
        GitFetchFilesTaskParams.builder()
            .gitFetchFilesConfigMap(ImmutableMap.of("Service", gitFetchFileConfigWithFilePath("file-folder")))
            .build();
    applicationManifestUtils.setValuesPathInGitFetchFilesTaskParams(gitFetchFilesTaskParams);
    GitFetchFilesConfig serviceTaskParams = gitFetchFilesTaskParams.getGitFetchFilesConfigMap().get("Service");
    assertThat(serviceTaskParams.getGitFileConfig().getFilePath()).isEqualTo("file-folder/" + values_filename);

    gitFetchFilesTaskParams =
        GitFetchFilesTaskParams.builder()
            .gitFetchFilesConfigMap(ImmutableMap.of("Service", gitFetchFileConfigWithFilePath("chart-directory/"),
                "Environment", gitFetchFileConfigWithFilePathList("file1", "file2")))
            .build();
    applicationManifestUtils.setValuesPathInGitFetchFilesTaskParams(gitFetchFilesTaskParams);
    serviceTaskParams = gitFetchFilesTaskParams.getGitFetchFilesConfigMap().get("Service");
    GitFetchFilesConfig environmentTaskParams = gitFetchFilesTaskParams.getGitFetchFilesConfigMap().get("Environment");
    assertThat(serviceTaskParams.getGitFileConfig().getFilePath()).isEqualTo("chart-directory/" + values_filename);
    assertThat(environmentTaskParams.getGitFileConfig().getFilePathList()).containsExactly("file1", "file2");

    gitFetchFilesTaskParams =
        GitFetchFilesTaskParams.builder()
            .gitFetchFilesConfigMap(ImmutableMap.of("Service", gitFetchFileConfigWithFilePath(null)))
            .build();

    applicationManifestUtils.setValuesPathInGitFetchFilesTaskParams(gitFetchFilesTaskParams);
    serviceTaskParams = gitFetchFilesTaskParams.getGitFetchFilesConfigMap().get("Service");
    assertThat(serviceTaskParams.getGitFileConfig().getFilePath()).isEqualTo(values_filename);
  }

  private GitFetchFilesConfig gitFetchFileConfigWithFilePath(String filePath) {
    return GitFetchFilesConfig.builder().gitFileConfig(GitFileConfig.builder().filePath(filePath).build()).build();
  }

  private GitFetchFilesConfig gitFetchFileConfigWithFilePathList(String... filePaths) {
    return GitFetchFilesConfig.builder()
        .gitFileConfig(GitFileConfig.builder().filePathList(asList(filePaths)).build())
        .build();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRenderGitConfigForApplicationManifest() {
    GitFileConfig toBeRendered = GitFileConfig.builder().build();
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = ImmutableMap.of(ServiceOverride,
        ApplicationManifest.builder().storeType(Remote).gitFileConfig(toBeRendered).build(), K8sValuesLocation.Service,
        ApplicationManifest.builder().storeType(Remote).gitFileConfig(toBeRendered).build(), Environment,
        ApplicationManifest.builder().storeType(Remote).gitFileConfig(toBeRendered).build(), EnvironmentGlobal,
        ApplicationManifest.builder().storeType(Remote).gitFileConfig(null).build());

    applicationManifestUtils.renderGitConfigForApplicationManifest(context, appManifestMap);
    verify(gitFileConfigHelperService, times(3)).renderGitFileConfig(context, toBeRendered);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetApplicationManifestWithPollForChangesEnabled() {
    HelmChartConfig chartConfig = HelmChartConfig.builder().chartName("chartName").chartUrl("chartUrl").build();
    ApplicationManifest serviceApplicationManifest = ApplicationManifest.builder()
                                                         .storeType(HelmChartRepo)
                                                         .pollForChanges(true)
                                                         .helmChartConfig(chartConfig)
                                                         .build();
    HelmChart helmChartInContextForService = HelmChart.builder().version("contextChartVersion").build();

    doReturn(true).when(featureFlagService).isEnabled(FeatureName.HELM_CHART_AS_ARTIFACT, ACCOUNT_ID);
    doReturn(serviceApplicationManifest).when(applicationManifestService).getManifestByServiceId(APP_ID, SERVICE_ID);
    doReturn(helmChartInContextForService).when(context).getHelmChartForService(SERVICE_ID);

    ApplicationManifest applicationManifest = applicationManifestUtils.getApplicationManifestForService(context);
    assertThat(applicationManifest.getHelmChartConfig().getChartVersion()).isEqualTo("contextChartVersion");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetApplicationManifestWithPollForChangesDisabled() {
    HelmChartConfig chartConfig =
        HelmChartConfig.builder().chartName("chartName").chartVersion("chartVersion").chartUrl("chartUrl").build();
    ApplicationManifest serviceApplicationManifest = ApplicationManifest.builder()
                                                         .storeType(HelmChartRepo)
                                                         .pollForChanges(false)
                                                         .helmChartConfig(chartConfig)
                                                         .build();

    doReturn(true).when(featureFlagService).isEnabled(FeatureName.HELM_CHART_AS_ARTIFACT, ACCOUNT_ID);
    doReturn(serviceApplicationManifest).when(applicationManifestService).getManifestByServiceId(APP_ID, SERVICE_ID);

    ApplicationManifest applicationManifest = applicationManifestUtils.getApplicationManifestForService(context);

    verify(context, never()).getHelmChartForService(SERVICE_ID);
    assertThat(applicationManifest.getHelmChartConfig().getChartVersion()).isEqualTo("chartVersion");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetApplicationManifestWithPollForChangesMissingHelmChart() {
    HelmChartConfig chartConfig = HelmChartConfig.builder().chartName("chartName").chartUrl("chartUrl").build();
    ApplicationManifest serviceApplicationManifest = ApplicationManifest.builder()
                                                         .storeType(HelmChartRepo)
                                                         .pollForChanges(true)
                                                         .helmChartConfig(chartConfig)
                                                         .build();

    doReturn(true).when(featureFlagService).isEnabled(FeatureName.HELM_CHART_AS_ARTIFACT, ACCOUNT_ID);
    doReturn(serviceApplicationManifest).when(applicationManifestService).getManifestByServiceId(APP_ID, SERVICE_ID);

    assertThatThrownBy(() -> applicationManifestUtils.getApplicationManifestForService(context))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining("INVALID_ARGUMENT");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testIsPollForChangesEnabled() {
    ApplicationManifestBuilder builder = ApplicationManifest.builder().storeType(HelmChartRepo);
    ApplicationManifest appManifestNullPollForChanges = builder.pollForChanges(null).build();
    ApplicationManifest appManifestFalsePollForChanges = builder.pollForChanges(false).build();
    ApplicationManifest appManifestTruePollForChanges = builder.pollForChanges(true).build();

    assertThat(applicationManifestUtils.isPollForChangesEnabled(null)).isFalse();
    assertThat(applicationManifestUtils.isPollForChangesEnabled(appManifestNullPollForChanges)).isFalse();
    assertThat(applicationManifestUtils.isPollForChangesEnabled(appManifestFalsePollForChanges)).isFalse();
    assertThat(applicationManifestUtils.isPollForChangesEnabled(appManifestTruePollForChanges)).isTrue();
  }
}
