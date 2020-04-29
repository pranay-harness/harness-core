package software.wings.service.impl;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.appmanifest.AppManifestKind.K8S_MANIFEST;
import static software.wings.beans.appmanifest.StoreType.HelmChartRepo;
import static software.wings.beans.appmanifest.StoreType.HelmSourceRepo;
import static software.wings.beans.appmanifest.StoreType.KustomizeSourceRepo;
import static software.wings.beans.appmanifest.StoreType.Local;
import static software.wings.beans.appmanifest.StoreType.OC_TEMPLATES;
import static software.wings.beans.appmanifest.StoreType.Remote;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.assertj.core.api.Assertions;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.Event;
import software.wings.beans.GitFileConfig;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.HelmChartConfig.HelmChartConfigBuilder;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.kustomize.KustomizeConfig;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.yaml.YamlPushService;

public class ApplicationManifestServiceImplTest extends WingsBaseTest {
  @Mock private AppService appService;
  @Mock private YamlPushService yamlPushService;
  @Inject private WingsPersistence wingsPersistence;
  @Spy @InjectMocks ApplicationManifestServiceImpl applicationManifestServiceImpl;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    Reflect.on(applicationManifestServiceImpl).set("wingsPersistence", wingsPersistence);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidateAppManifestForEnvironment() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().kind(AppManifestKind.HELM_CHART_OVERRIDE).storeType(Local).build();
    verifyExceptionForValidateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setStoreType(Remote);
    verifyExceptionForValidateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setStoreType(HelmChartRepo);
    applicationManifestServiceImpl.validateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setStoreType(HelmSourceRepo);
    applicationManifestServiceImpl.validateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setKind(K8S_MANIFEST);
    applicationManifest.setStoreType(HelmChartRepo);
    verifyExceptionForValidateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setKind(K8S_MANIFEST);
    applicationManifest.setStoreType(HelmSourceRepo);
    verifyExceptionForValidateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setStoreType(Remote);
    applicationManifestServiceImpl.validateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setStoreType(Local);
    applicationManifestServiceImpl.validateAppManifestForEnvironment(applicationManifest);

    verifyAppManifestHelmChartOverrideForEnv();
  }

  private void verifyAppManifestHelmChartOverrideForEnv() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().kind(AppManifestKind.HELM_CHART_OVERRIDE).storeType(HelmChartRepo).build();

    applicationManifest.setServiceId(null);
    applicationManifest.setEnvId("envId");
    applicationManifestServiceImpl.validateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setStoreType(Local);
    verifyExceptionForValidateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setStoreType(Remote);
    verifyExceptionForValidateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setStoreType(HelmSourceRepo);
    verifyExceptionForValidateAppManifestForEnvironment(applicationManifest);

    applicationManifest.setStoreType(KustomizeSourceRepo);
    verifyExceptionForValidateAppManifestForEnvironment(applicationManifest);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidateApplicationManifest() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder()
            .kind(AppManifestKind.HELM_CHART_OVERRIDE)
            .helmChartConfig(HelmChartConfig.builder().chartName("n").connectorId("c1").build())
            .envId("ENVID")
            .storeType(HelmChartRepo)
            .build();

    try {
      applicationManifestServiceImpl.validateApplicationManifest(applicationManifest);
    } catch (Exception e) {
      assertThat(e instanceof InvalidRequestException).isTrue();
    }

    applicationManifest.setServiceId("s1");
    applicationManifestServiceImpl.validateApplicationManifest(applicationManifest);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testValidateHelmChartRepoAppManifestForAllServiceOverride() {
    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .kind(AppManifestKind.HELM_CHART_OVERRIDE)
                                                  .storeType(HelmChartRepo)
                                                  .envId("envId")
                                                  .build();

    applicationManifest.setGitFileConfig(GitFileConfig.builder().build());
    // No GitConfig
    verifyInvalidRequestExceptionWithMessage(applicationManifest, "gitFileConfig cannot be used with HelmChartRepo");

    HelmChartConfig helmChartConfig = helmChartConfigWithConnector().build();
    applicationManifest.setGitFileConfig(null);
    applicationManifest.setHelmChartConfig(helmChartConfig);

    applicationManifestServiceImpl.validateApplicationManifest(applicationManifest);

    helmChartConfig.setConnectorId(null);
    verifyInvalidRequestExceptionWithMessage(applicationManifest, "Helm repository cannot be empty");

    helmChartConfig = helmChartConfigWithConnector().chartName("stable").build();
    applicationManifest.setHelmChartConfig(helmChartConfig);
    verifyInvalidRequestExceptionWithMessage(applicationManifest, "Helm chart name cannot be given");

    helmChartConfig = helmChartConfigWithConnector().chartUrl("http://helm-repo").build();
    applicationManifest.setHelmChartConfig(helmChartConfig);
    verifyInvalidRequestExceptionWithMessage(applicationManifest, "Helm chart url cannot be given");

    helmChartConfig = helmChartConfigWithConnector().chartVersion("1.1").build();
    applicationManifest.setHelmChartConfig(helmChartConfig);
    verifyInvalidRequestExceptionWithMessage(applicationManifest, "Helm chart version cannot be given");
  }

  private HelmChartConfigBuilder helmChartConfigWithConnector() {
    return HelmChartConfig.builder().connectorId("foo");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidateHelmChartRepoAppManifest() {
    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .kind(AppManifestKind.HELM_CHART_OVERRIDE)
                                                  .storeType(HelmChartRepo)
                                                  .serviceId("serviceId")
                                                  .envId("envId")
                                                  .build();
    applicationManifest.setGitFileConfig(GitFileConfig.builder().build());
    // No GitConfig
    verifyExceptionForValidateHelmChartRepoAppManifest(applicationManifest);

    HelmChartConfig helmChartConfig = HelmChartConfig.builder().build();
    applicationManifest.setGitFileConfig(null);
    applicationManifest.setHelmChartConfig(helmChartConfig);
    // Empty connectorId and chartName
    verifyExceptionForValidateHelmChartRepoAppManifest(applicationManifest);

    // Empty chartName
    helmChartConfig.setConnectorId("1");
    verifyExceptionForValidateHelmChartRepoAppManifest(applicationManifest);

    // Empty connectorId
    helmChartConfig.setConnectorId(null);
    helmChartConfig.setChartName("Name");
    verifyExceptionForValidateHelmChartRepoAppManifest(applicationManifest);

    // non-empty url (this is invalid, url needs to be empty)
    helmChartConfig.setConnectorId("con1");
    helmChartConfig.setChartName("Name");
    helmChartConfig.setChartUrl("url");
    verifyExceptionForValidateHelmChartRepoAppManifest(applicationManifest);

    helmChartConfig.setChartUrl(null);
    applicationManifestServiceImpl.validateHelmChartRepoAppManifest(applicationManifest);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testValidateKustomizeApplicationManifest() {
    applicationManifestServiceImpl.validateApplicationManifest(buildKustomizeAppManifest());
    testEmptyConnectorInRemoteAppManifest(buildKustomizeAppManifest());
    testEmptyCommitInRemoteAppManifest(buildKustomizeAppManifest());
    testEmptyBranchInRemoteAppManifest(buildKustomizeAppManifest());
    testNonEmptyFilePathInGitFileConfig(buildKustomizeAppManifest());
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testCreateAppManifest() {
    doNothing().when(applicationManifestServiceImpl).validateApplicationManifest(any(ApplicationManifest.class));
    doNothing().when(applicationManifestServiceImpl).sanitizeApplicationManifestConfigs(any(ApplicationManifest.class));
    doReturn(false).when(applicationManifestServiceImpl).exists(any(ApplicationManifest.class));
    doReturn("accountId").when(appService).getAccountIdByAppId(anyString());

    ApplicationManifest manifest = buildKustomizeAppManifest();
    ApplicationManifest savedApplicationManifest = applicationManifestServiceImpl.create(manifest);

    assertThat(savedApplicationManifest).isNotNull();
    assertThat(manifest).isEqualTo(savedApplicationManifest);
    verify(yamlPushService, times(1))
        .pushYamlChangeSet(
            anyString(), eq(null), eq(savedApplicationManifest), eq(Event.Type.CREATE), eq(false), eq(false));
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testUpdateAppManifest() {
    doNothing().when(applicationManifestServiceImpl).validateApplicationManifest(any(ApplicationManifest.class));
    doNothing().when(applicationManifestServiceImpl).sanitizeApplicationManifestConfigs(any(ApplicationManifest.class));
    doNothing().when(applicationManifestServiceImpl).resetReadOnlyProperties(any(ApplicationManifest.class));
    doReturn(true).when(applicationManifestServiceImpl).exists(any(ApplicationManifest.class));
    doReturn("accountId").when(appService).getAccountIdByAppId(anyString());

    ApplicationManifest manifest = buildKustomizeAppManifest();
    ApplicationManifest savedApplicationManifest = applicationManifestServiceImpl.update(manifest);

    assertThat(savedApplicationManifest).isNotNull();
    assertThat(manifest).isEqualTo(savedApplicationManifest);
    verify(yamlPushService, times(1))
        .pushYamlChangeSet(anyString(), eq(savedApplicationManifest), eq(savedApplicationManifest),
            eq(Event.Type.UPDATE), eq(false), eq(false));
    verify(applicationManifestServiceImpl, times(1)).resetReadOnlyProperties(manifest);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testSanitizeKustomizeManifest() {
    verifySanitizeIfNullKustomizeDirPath();
    verifySanitizeIfEmptyKustomizeDirPath();
    verifySanitizeIfNonEmptyKustomizeDirPath();
  }

  private void verifySanitizeIfNonEmptyKustomizeDirPath() {
    ApplicationManifest manifest = buildKustomizeAppManifest();
    String kustomizeDirPath = manifest.getKustomizeConfig().getKustomizeDirPath();
    applicationManifestServiceImpl.sanitizeApplicationManifestConfigs(manifest);
    assertThat(manifest.getKustomizeConfig().getKustomizeDirPath()).isNotNull();
    assertThat(manifest.getKustomizeConfig().getKustomizeDirPath()).isEqualTo(kustomizeDirPath);
  }

  private void verifySanitizeIfEmptyKustomizeDirPath() {
    ApplicationManifest manifest = buildKustomizeAppManifest();
    manifest.getKustomizeConfig().setKustomizeDirPath("");
    applicationManifestServiceImpl.sanitizeApplicationManifestConfigs(manifest);
    assertThat(manifest.getKustomizeConfig().getKustomizeDirPath()).isNotNull();
    assertThat(manifest.getKustomizeConfig().getKustomizeDirPath()).isEmpty();
  }

  private void verifySanitizeIfNullKustomizeDirPath() {
    ApplicationManifest manifest = buildKustomizeAppManifest();
    manifest.getKustomizeConfig().setKustomizeDirPath(null);
    applicationManifestServiceImpl.sanitizeApplicationManifestConfigs(manifest);
    assertThat(manifest.getKustomizeConfig().getKustomizeDirPath()).isNotNull();
    assertThat(manifest.getKustomizeConfig().getKustomizeDirPath()).isEmpty();
  }

  private ApplicationManifest buildKustomizeAppManifest() {
    GitFileConfig gitFileConfig =
        GitFileConfig.builder().connectorId("connector-id").useBranch(true).branch("master").build();
    return ApplicationManifest.builder()
        .kind(K8S_MANIFEST)
        .gitFileConfig(gitFileConfig)
        .storeType(KustomizeSourceRepo)
        .kustomizeConfig(KustomizeConfig.builder().kustomizeDirPath("/root").build())
        .serviceId("serviceId")
        .build();
  }

  private void testEmptyConnectorInRemoteAppManifest(ApplicationManifest applicationManifest) {
    applicationManifest.getGitFileConfig().setConnectorId(null);
    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> applicationManifestServiceImpl.validateApplicationManifest(applicationManifest))
        .withMessageContaining("Connector");
  }

  private void testEmptyCommitInRemoteAppManifest(ApplicationManifest applicationManifest) {
    applicationManifest.getGitFileConfig().setCommitId(null);
    applicationManifest.getGitFileConfig().setUseBranch(false);
    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> applicationManifestServiceImpl.validateApplicationManifest(applicationManifest))
        .withMessageContaining("CommitId");
  }

  private void testEmptyBranchInRemoteAppManifest(ApplicationManifest applicationManifest) {
    applicationManifest.getGitFileConfig().setBranch(null);
    applicationManifest.getGitFileConfig().setUseBranch(true);
    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> applicationManifestServiceImpl.validateApplicationManifest(applicationManifest))
        .withMessageContaining("Branch");
  }

  private void testNonEmptyFilePathInGitFileConfig(ApplicationManifest applicationManifest) {
    applicationManifestServiceImpl.validateApplicationManifest(applicationManifest);
    applicationManifest.getGitFileConfig().setFilePath("foo");
    Assertions.assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> applicationManifestServiceImpl.validateApplicationManifest(applicationManifest))
        .withMessageContaining("File Path");
  }

  private void verifyExceptionForValidateHelmChartRepoAppManifest(ApplicationManifest applicationManifest) {
    try {
      applicationManifestServiceImpl.validateHelmChartRepoAppManifest(applicationManifest);
    } catch (Exception e) {
      assertThat(e instanceof InvalidRequestException).isTrue();
    }
  }

  private void verifyInvalidRequestExceptionWithMessage(ApplicationManifest applicationManifest, String msg) {
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> applicationManifestServiceImpl.validateApplicationManifest(applicationManifest))
        .withMessageContaining(msg);
  }

  private void verifyExceptionForValidateAppManifestForEnvironment(ApplicationManifest applicationManifest) {
    try {
      applicationManifestServiceImpl.validateAppManifestForEnvironment(applicationManifest);
    } catch (Exception e) {
      assertThat(e instanceof InvalidRequestException).isTrue();
    }
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testValidateOpenShiftSourceRepoAppManifest() {
    // success validation
    GitFileConfig gitFileConfig =
        GitFileConfig.builder().branch("master").useBranch(true).connectorId("id").filePath("filepath").build();
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().storeType(OC_TEMPLATES).gitFileConfig(gitFileConfig).build();

    applicationManifestServiceImpl.validateOpenShiftSourceRepoAppManifest(applicationManifest);

    // missing params
    applicationManifest.setGitFileConfig(null);
    assertThatThrownBy(() -> applicationManifestServiceImpl.validateOpenShiftSourceRepoAppManifest(applicationManifest))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Git File Config is mandatory for OpenShift Source Repository Type");
    applicationManifest.setGitFileConfig(gitFileConfig);

    gitFileConfig.setBranch("");
    assertThatThrownBy(() -> applicationManifestServiceImpl.validateOpenShiftSourceRepoAppManifest(applicationManifest))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Branch cannot be empty if useBranch is selected.");
    gitFileConfig.setBranch("master");

    gitFileConfig.setFilePath("");
    assertThatThrownBy(() -> applicationManifestServiceImpl.validateOpenShiftSourceRepoAppManifest(applicationManifest))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Template File Path can't be empty");
    gitFileConfig.setFilePath("filepath");
  }
}
