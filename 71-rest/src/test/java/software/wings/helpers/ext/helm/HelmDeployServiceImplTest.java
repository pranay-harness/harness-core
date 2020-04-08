package software.wings.helpers.ext.helm;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;
import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion.V2;
import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion.V3;

import com.google.common.util.concurrent.FakeTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;

import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.assertj.core.api.Assertions;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.LogCallback;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.delegatetasks.helm.HelmCommandHelper;
import software.wings.delegatetasks.helm.HelmTaskHelper;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.helpers.ext.helm.HelmClientImpl.HelmCliResponse;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;
import software.wings.helpers.ext.helm.response.HelmListReleasesCommandResponse;
import software.wings.helpers.ext.helm.response.HelmReleaseHistoryCommandResponse;
import software.wings.helpers.ext.helm.response.ReleaseInfo;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.HelmTestConstants;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class HelmDeployServiceImplTest extends WingsBaseTest {
  @Mock private HelmClient helmClient;
  @Mock private GitService gitService;
  @Mock private TimeLimiter mockTimeLimiter;
  @Mock private EncryptionService encryptionService;
  @Mock private HelmCommandHelper helmCommandHelper;
  @Mock private LogCallback logCallback;
  @Mock private HelmTaskHelper helmTaskHelper;
  @Mock private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Mock private K8sGlobalConfigService k8sGlobalConfigService;
  @InjectMocks private HelmDeployServiceImpl helmDeployService;

  private HelmDeployServiceImpl spyHelmDeployService = spy(new HelmDeployServiceImpl());

  private TimeLimiter timeLimiter = new FakeTimeLimiter();
  private HelmInstallCommandRequest helmInstallCommandRequest;
  private HelmInstallCommandResponse helmInstallCommandResponse;
  private HelmCliResponse helmCliReleaseHistoryResponse;
  private HelmCliResponse helmCliListReleasesResponse;
  private GitFileConfig gitFileConfig;
  ExecutionLogCallback executionLogCallback;

  @Before
  public void setUp() throws Exception {
    helmInstallCommandRequest = createHelmInstallCommandRequest();
    helmInstallCommandResponse = createHelmInstallCommandResponse();
    helmCliReleaseHistoryResponse = createHelmCliResponse();
    helmCliListReleasesResponse = createHelmCliResponse();
    gitFileConfig = new GitFileConfig();
    gitFileConfig.setFilePath(HelmTestConstants.FILE_PATH_KEY);
    executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString());
    when(encryptionService.decrypt(any(), any())).thenReturn(null);
    when(gitService.fetchFilesByPath(any(), any(), any(), any(), any(), anyBoolean()))
        .thenReturn(GitFetchFilesResult.builder()
                        .files(asList(GitFile.builder().fileContent(HelmTestConstants.GIT_FILE_CONTENT_1_KEY).build(),
                            GitFile.builder().fileContent(HelmTestConstants.GIT_FILE_CONTENT_2_KEY).build()))
                        .build());
    when(helmCommandHelper.isValidChartSpecification(any())).thenReturn(true);
    when(helmCommandHelper.generateHelmDeployChartSpecFromYaml(any())).thenReturn(Optional.empty());
    when(helmClient.repoUpdate(any())).thenReturn(HelmCliResponse.builder().build());
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDeployInstall() throws InterruptedException, TimeoutException, IOException, ExecutionException {
    helmCliReleaseHistoryResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
    helmCliListReleasesResponse.setCommandExecutionStatus(SUCCESS);

    when(helmClient.releaseHistory(any())).thenReturn(helmCliReleaseHistoryResponse);
    when(helmClient.install(any())).thenReturn(helmInstallCommandResponse);
    when(helmClient.listReleases(any())).thenReturn(helmCliListReleasesResponse);

    HelmCommandResponse helmCommandResponse = helmDeployService.deploy(helmInstallCommandRequest);
    assertThat(helmCommandResponse.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    verify(helmClient).install(helmInstallCommandRequest);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testDeployUpgrade() throws InterruptedException, TimeoutException, IOException, ExecutionException {
    helmCliReleaseHistoryResponse.setCommandExecutionStatus(SUCCESS);
    helmCliListReleasesResponse.setOutput(HelmTestConstants.LIST_RELEASE_V2);
    helmCliListReleasesResponse.setCommandExecutionStatus(SUCCESS);

    when(helmClient.releaseHistory(any())).thenReturn(helmCliReleaseHistoryResponse);
    when(helmClient.upgrade(any())).thenReturn(helmInstallCommandResponse);
    when(helmClient.listReleases(any())).thenReturn(helmCliListReleasesResponse);

    HelmCommandResponse helmCommandResponse = helmDeployService.deploy(helmInstallCommandRequest);
    assertThat(helmCommandResponse.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    verify(helmClient).upgrade(helmInstallCommandRequest);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testAddYamlValuesFromGitRepo()
      throws InterruptedException, TimeoutException, IOException, ExecutionException {
    helmInstallCommandRequest.setGitConfig(GitConfig.builder().build());
    helmInstallCommandRequest.setGitFileConfig(gitFileConfig);
    helmCliReleaseHistoryResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
    helmCliListReleasesResponse.setCommandExecutionStatus(SUCCESS);

    when(helmClient.releaseHistory(any())).thenReturn(helmCliReleaseHistoryResponse);
    when(helmClient.listReleases(any())).thenReturn(helmCliListReleasesResponse);
    when(helmClient.install(any())).thenReturn(helmInstallCommandResponse);

    ArgumentCaptor<HelmInstallCommandRequest> argumentCaptor = ArgumentCaptor.forClass(HelmInstallCommandRequest.class);
    HelmCommandResponse helmCommandResponse = helmDeployService.deploy(helmInstallCommandRequest);
    assertThat(helmCommandResponse.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    verify(helmClient).install(argumentCaptor.capture());
    HelmInstallCommandRequest commandRequest = argumentCaptor.getAllValues().get(0);
    assertThat(commandRequest.getVariableOverridesYamlFiles().size()).isEqualTo(2);
    assertThat(commandRequest.getVariableOverridesYamlFiles())
        .contains(HelmTestConstants.GIT_FILE_CONTENT_1_KEY, HelmTestConstants.GIT_FILE_CONTENT_2_KEY);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testAddYamlValuesFromGitRepoWithVariableYamlFiles()
      throws InterruptedException, TimeoutException, IOException, ExecutionException {
    helmInstallCommandRequest.setGitConfig(GitConfig.builder().build());
    helmInstallCommandRequest.setGitFileConfig(gitFileConfig);
    helmInstallCommandRequest.setVariableOverridesYamlFiles(asList(HelmTestConstants.GIT_FILE_CONTENT_3_KEY));
    helmCliReleaseHistoryResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
    helmCliListReleasesResponse.setCommandExecutionStatus(SUCCESS);

    when(helmClient.releaseHistory(any())).thenReturn(helmCliReleaseHistoryResponse);
    when(helmClient.listReleases(any())).thenReturn(helmCliListReleasesResponse);
    when(helmClient.install(any())).thenReturn(helmInstallCommandResponse);

    ArgumentCaptor<HelmInstallCommandRequest> argumentCaptor = ArgumentCaptor.forClass(HelmInstallCommandRequest.class);
    HelmCommandResponse helmCommandResponse = helmDeployService.deploy(helmInstallCommandRequest);
    assertThat(helmCommandResponse.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    verify(helmClient).install(argumentCaptor.capture());
    HelmInstallCommandRequest commandRequest = argumentCaptor.getAllValues().get(0);
    assertThat(commandRequest.getVariableOverridesYamlFiles().size()).isEqualTo(3);
    assertThat(commandRequest.getVariableOverridesYamlFiles())
        .contains(HelmTestConstants.GIT_FILE_CONTENT_1_KEY, HelmTestConstants.GIT_FILE_CONTENT_2_KEY,
            HelmTestConstants.GIT_FILE_CONTENT_3_KEY);
  }

  @Test(expected = WingsException.class)
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testAddValuesYamlFromGitRepoWithException()
      throws InterruptedException, TimeoutException, IOException, ExecutionException {
    helmInstallCommandRequest.setGitConfig(GitConfig.builder().build());
    helmCliListReleasesResponse.setCommandExecutionStatus(SUCCESS);

    GitFileConfig gitFileConfig = new GitFileConfig();
    gitFileConfig.setFilePath(HelmTestConstants.FILE_PATH_KEY);
    helmInstallCommandRequest.setGitFileConfig(gitFileConfig);
    helmInstallCommandRequest.setVariableOverridesYamlFiles(asList(HelmTestConstants.GIT_FILE_CONTENT_3_KEY));
    helmCliReleaseHistoryResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);

    when(helmClient.releaseHistory(any())).thenReturn(helmCliReleaseHistoryResponse);
    when(helmClient.install(any())).thenReturn(helmInstallCommandResponse);
    when(gitService.fetchFilesByPath(any(), any(), any(), any(), any(), anyBoolean()))
        .thenThrow(new WingsException("WingsException"));
    when(helmClient.listReleases(any())).thenReturn(helmCliListReleasesResponse);

    helmDeployService.deploy(helmInstallCommandRequest);
  }

  private HelmInstallCommandRequest createHelmInstallCommandRequest() {
    return HelmInstallCommandRequest.builder()
        .releaseName(HelmTestConstants.HELM_RELEASE_NAME_KEY)
        .kubeConfigLocation(HelmTestConstants.HELM_KUBE_CONFIG_LOCATION_KEY)
        .chartSpecification(HelmChartSpecification.builder().chartName(HelmTestConstants.CHART_NAME_KEY).build())
        .executionLogCallback(logCallback)
        .build();
  }

  private HelmCliResponse createHelmCliResponse() {
    return HelmCliResponse.builder().build();
  }

  private HelmInstallCommandResponse createHelmInstallCommandResponse() {
    return HelmInstallCommandResponse.builder().output("").commandExecutionStatus(SUCCESS).build();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testIsHelm3() {
    assertThat(helmDeployService.isHelm3("v3.1.2+g19e47ee")).isTrue();
    assertThat(
        helmDeployService.isHelm3(
            "Client: &version.Version{SemVer:\"v2.13.1\", GitCommit:\"618447cbf203d147601b4b9bd7f8c37a5d39fbb4\", GitTreeState:\"clean\"}\n"
            + "Server: &version.Version{SemVer:\"v2.14.1\", GitCommit:\"5270352a09c7e8b6e8c9593002a73535276507c0\", GitTreeState:\"clean\"}"))
        .isFalse();
    assertThat(helmDeployService.isHelm3("")).isFalse();
    assertThat(helmDeployService.isHelm3(null)).isFalse();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testEnsureHelm3Installed() throws InterruptedException, TimeoutException, IOException {
    successWhenHelm3PresentInClientTools();
    failureWhenHelm3AbsentInClientTools();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testisFailedStatus() {
    assertThat(helmDeployService.isFailedStatus("failed")).isTrue();
    assertThat(helmDeployService.isFailedStatus("FAILED")).isTrue();
    assertThat(helmDeployService.isFailedStatus("Failed")).isTrue();
    assertThat(helmDeployService.isFailedStatus("FailedAbc")).isFalse();
    assertThat(helmDeployService.isFailedStatus("Success")).isFalse();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testReleaseHistory() throws InterruptedException, IOException, TimeoutException {
    shouldListReleaseHistoryV2();
    shouldListReleaseHistoryV3();
    shouldNotThrowExceptionInReleaseHist();
  }

  private void shouldListReleaseHistoryV2() throws InterruptedException, IOException, TimeoutException {
    HelmReleaseHistoryCommandRequest request = HelmReleaseHistoryCommandRequest.builder().build();

    when(helmClient.releaseHistory(request))
        .thenReturn(HelmCliResponse.builder()
                        .commandExecutionStatus(SUCCESS)
                        .output(HelmTestConstants.RELEASE_HIST_V2)
                        .build());

    HelmReleaseHistoryCommandResponse response = helmDeployService.releaseHistory(request);

    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response.getReleaseInfoList()).hasSize(5);
    assertThat(response.getReleaseInfoList().stream().map(ReleaseInfo::getRevision))
        .hasSameElementsAs(asList("1", "2", "3", "4", "5"));
    assertThat(response.getReleaseInfoList().stream().map(ReleaseInfo::getChart))
        .hasSameElementsAs(asList(
            "chartmuseum-2.3.1", "chartmuseum-2.3.2", "chartmuseum-2.3.3", "chartmuseum-2.3.4", "chartmuseum-2.3.5"));
  }

  private void shouldListReleaseHistoryV3() throws InterruptedException, IOException, TimeoutException {
    HelmReleaseHistoryCommandRequest request = HelmReleaseHistoryCommandRequest.builder().build();

    when(helmClient.releaseHistory(request))
        .thenReturn(HelmCliResponse.builder()
                        .commandExecutionStatus(SUCCESS)
                        .output(HelmTestConstants.RELEASE_HIST_V3)
                        .build());

    HelmReleaseHistoryCommandResponse response = helmDeployService.releaseHistory(request);

    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response.getReleaseInfoList()).hasSize(4);
    assertThat(response.getReleaseInfoList().stream().map(ReleaseInfo::getRevision))
        .hasSameElementsAs(asList("1", "2", "3", "4"));
    assertThat(response.getReleaseInfoList().stream().map(ReleaseInfo::getChart))
        .hasSameElementsAs(asList("zetcd-0.1.4", "zetcd-0.1.9", "zetcd-0.2.9", "chartmuseum-2.7.0"));
  }

  private void shouldNotThrowExceptionInReleaseHist() throws InterruptedException, IOException, TimeoutException {
    HelmReleaseHistoryCommandRequest request = HelmReleaseHistoryCommandRequest.builder().build();

    when(helmClient.releaseHistory(request)).thenThrow(new InterruptedException());

    HelmReleaseHistoryCommandResponse response = helmDeployService.releaseHistory(request);

    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response.getReleaseInfoList()).isEmpty();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testListRelease() throws InterruptedException, TimeoutException, IOException {
    shouldListReleaseV2();
    shouldListReleaseV3();
    shouldNotThrowExceptionInListRelease();
  }

  private void shouldListReleaseV2() throws InterruptedException, IOException, TimeoutException {
    HelmInstallCommandRequest request = HelmInstallCommandRequest.builder().build();

    when(helmClient.listReleases(request))
        .thenReturn(HelmCliResponse.builder()
                        .commandExecutionStatus(SUCCESS)
                        .output(HelmTestConstants.LIST_RELEASE_V2)
                        .build());

    HelmListReleasesCommandResponse response = helmDeployService.listReleases(request);

    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response.getReleaseInfoList()).hasSize(1);
    assertThat(response.getReleaseInfoList().stream().map(ReleaseInfo::getName))
        .hasSameElementsAs(asList("helm-release-name"));
    assertThat(response.getReleaseInfoList().stream().map(ReleaseInfo::getRevision)).hasSameElementsAs(asList("85"));
    assertThat(response.getReleaseInfoList().stream().map(ReleaseInfo::getNamespace))
        .hasSameElementsAs(asList("default"));
  }

  private void shouldListReleaseV3() throws InterruptedException, IOException, TimeoutException {
    HelmInstallCommandRequest request = HelmInstallCommandRequest.builder().build();

    when(helmClient.listReleases(request))
        .thenReturn(HelmCliResponse.builder()
                        .commandExecutionStatus(SUCCESS)
                        .output(HelmTestConstants.LIST_RELEASE_V3)
                        .build());

    HelmListReleasesCommandResponse response = helmDeployService.listReleases(request);

    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response.getReleaseInfoList()).hasSize(3);
    assertThat(response.getReleaseInfoList().stream().map(ReleaseInfo::getName))
        .hasSameElementsAs(asList("ft-test", "ft-test1", "helm2-http"));
    assertThat(response.getReleaseInfoList().stream().map(ReleaseInfo::getRevision))
        .hasSameElementsAs(asList("1", "1", "6"));
    assertThat(response.getReleaseInfoList().stream().map(ReleaseInfo::getNamespace))
        .hasSameElementsAs(asList("default", "harness", "default"));
  }

  private void shouldNotThrowExceptionInListRelease() throws InterruptedException, IOException, TimeoutException {
    HelmInstallCommandRequest request = HelmInstallCommandRequest.builder().build();

    when(helmClient.listReleases(request)).thenThrow(new InterruptedException());

    HelmListReleasesCommandResponse response = helmDeployService.listReleases(request);

    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    assertThat(response.getReleaseInfoList()).isNull();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testEnsureHelmCliAndTillerInstalledIfInstalled()
      throws InterruptedException, IOException, TimeoutException {
    setFakeTimeLimiter();
    HelmInstallCommandRequest request = HelmInstallCommandRequest.builder().build();

    when(helmClient.getClientAndServerVersion(request))
        .thenReturn(
            HelmCliResponse.builder().commandExecutionStatus(SUCCESS).output(HelmTestConstants.VERSION_V2).build());

    HelmCommandResponse response = helmDeployService.ensureHelmCliAndTillerInstalled(request);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response.getOutput()).isNotEmpty();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testEnsureHelmCliAndTillerInstalledIfNotInstalled()
      throws InterruptedException, IOException, TimeoutException {
    setFakeTimeLimiter();
    HelmInstallCommandRequest request = HelmInstallCommandRequest.builder().build();

    when(helmClient.getClientAndServerVersion(request))
        .thenReturn(HelmCliResponse.builder().commandExecutionStatus(FAILURE).build());

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> helmDeployService.ensureHelmCliAndTillerInstalled(request));
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testEnsureHelmCliAndTillerInstalledIfV3Installed()
      throws InterruptedException, IOException, TimeoutException {
    setFakeTimeLimiter();
    HelmInstallCommandRequest request = HelmInstallCommandRequest.builder().build();

    when(helmClient.getClientAndServerVersion(request))
        .thenReturn(
            HelmCliResponse.builder().commandExecutionStatus(SUCCESS).output(HelmTestConstants.VERSION_V3).build());

    HelmCommandResponse response = helmDeployService.ensureHelmCliAndTillerInstalled(request);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    assertThat(response.getOutput()).isNotEmpty();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testEnsureHelmCliAndTillerInstalledIfClusterUnreachable()
      throws InterruptedException, IOException, TimeoutException {
    setFakeTimeLimiter();
    HelmInstallCommandRequest request = HelmInstallCommandRequest.builder().build();

    when(helmClient.getClientAndServerVersion(request)).thenThrow(new UncheckedTimeoutException());

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> helmDeployService.ensureHelmCliAndTillerInstalled(request));
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testAddRepoIfAlreadyAddedV2() throws InterruptedException, IOException, TimeoutException {
    HelmCommandRequest request =
        HelmInstallCommandRequest.builder()
            .executionLogCallback(executionLogCallback)
            .chartSpecification(
                HelmChartSpecification.builder().chartUrl("https://harness.jfrog.io/harness/helm").build())
            .build();

    when(helmClient.getHelmRepoList(request))
        .thenReturn(
            HelmCliResponse.builder().commandExecutionStatus(SUCCESS).output(HelmTestConstants.REPO_LIST_V2).build());

    helmDeployService.addPublicRepo(request);

    verify(helmClient, never()).addPublicRepo(any());
  }
  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testAddRepoIfAlreadyAddedV3() throws InterruptedException, IOException, TimeoutException {
    HelmCommandRequest request =
        HelmInstallCommandRequest.builder()
            .executionLogCallback(executionLogCallback)
            .chartSpecification(HelmChartSpecification.builder().chartUrl("https://charts.bitnami.com/bitnami").build())
            .build();

    when(helmClient.getHelmRepoList(request))
        .thenReturn(
            HelmCliResponse.builder().commandExecutionStatus(SUCCESS).output(HelmTestConstants.REPO_LIST_V3).build());

    helmDeployService.addPublicRepo(request);

    verify(helmClient, never()).addPublicRepo(any());
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testAddNewRepo() throws InterruptedException, IOException, TimeoutException {
    HelmCommandRequest request = HelmInstallCommandRequest.builder()
                                     .executionLogCallback(executionLogCallback)
                                     .chartSpecification(HelmChartSpecification.builder().chartUrl("abc.com").build())
                                     .build();

    when(helmClient.getHelmRepoList(request))
        .thenReturn(
            HelmCliResponse.builder().commandExecutionStatus(SUCCESS).output(HelmTestConstants.REPO_LIST_V2).build());
    when(helmClient.addPublicRepo(any())).thenReturn(HelmCliResponse.builder().commandExecutionStatus(SUCCESS).build());

    HelmCommandResponse response = helmDeployService.addPublicRepo(request);

    verify(helmClient, times(1)).addPublicRepo(any());
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testAddNewRepoIfNotReachable() throws InterruptedException, IOException, TimeoutException {
    HelmCommandRequest request = HelmInstallCommandRequest.builder()
                                     .executionLogCallback(executionLogCallback)
                                     .chartSpecification(HelmChartSpecification.builder().chartUrl("abc.com").build())
                                     .build();

    when(helmClient.getHelmRepoList(request))
        .thenReturn(
            HelmCliResponse.builder().commandExecutionStatus(SUCCESS).output(HelmTestConstants.REPO_LIST_V2).build());
    when(helmClient.addPublicRepo(any())).thenReturn(HelmCliResponse.builder().commandExecutionStatus(FAILURE).build());

    assertThatExceptionOfType(InvalidRequestException.class).isThrownBy(() -> helmDeployService.addPublicRepo(request));
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testAddNewRepoIfRepoListFails() throws InterruptedException, IOException, TimeoutException {
    HelmCommandRequest request = HelmInstallCommandRequest.builder()
                                     .executionLogCallback(executionLogCallback)
                                     .chartSpecification(HelmChartSpecification.builder().chartUrl("abc.com").build())
                                     .build();

    when(helmClient.getHelmRepoList(request))
        .thenReturn(HelmCliResponse.builder().commandExecutionStatus(FAILURE).output("command not found").build());
    when(helmClient.addPublicRepo(any())).thenReturn(HelmCliResponse.builder().commandExecutionStatus(FAILURE).build());

    assertThatExceptionOfType(InvalidRequestException.class).isThrownBy(() -> helmDeployService.addPublicRepo(request));
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testFetchChartRepo() throws Exception {
    ArgumentCaptor<HelmChartConfigParams> chartConfigParamsArgumentCaptor =
        ArgumentCaptor.forClass(HelmChartConfigParams.class);
    ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    HelmInstallCommandRequest request =
        HelmInstallCommandRequest.builder()
            .executionLogCallback(executionLogCallback)
            .sourceRepoConfig(K8sDelegateManifestConfig.builder()
                                  .helmChartConfigParams(HelmChartConfigParams.builder().chartName("foo").build())
                                  .build())
            .build();

    helmDeployService.fetchChartRepo(request);

    verify(helmTaskHelper, times(1))
        .downloadChartFiles(chartConfigParamsArgumentCaptor.capture(), stringArgumentCaptor.capture());

    HelmChartConfigParams helmChartConfigParams = chartConfigParamsArgumentCaptor.getValue();
    String directory = stringArgumentCaptor.getValue();
    assertThat(helmChartConfigParams).isNotNull();
    assertThat(directory).isNotEmpty();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testFetchSourceRepo() {
    ArgumentCaptor<GitConfig> argumentCaptor = ArgumentCaptor.forClass(GitConfig.class);
    HelmInstallCommandRequest request =
        HelmInstallCommandRequest.builder()
            .sourceRepoConfig(K8sDelegateManifestConfig.builder()
                                  .manifestStoreTypes(StoreType.HelmSourceRepo)
                                  .gitConfig(GitConfig.builder().build())
                                  .gitFileConfig(GitFileConfig.builder().branch("master").useBranch(true).build())
                                  .build())
            .executionLogCallback(executionLogCallback)
            .build();

    helmDeployService.fetchSourceRepo(request);

    verify(encryptionService, times(1)).decrypt(argumentCaptor.capture(), anyList());
    verify(gitService, times(1))
        .downloadFiles(
            any(GitConfig.class), anyString(), anyString(), anyString(), anyList(), anyBoolean(), anyString());

    GitConfig gitConfig = argumentCaptor.getValue();
    assertThat(gitConfig.getBranch()).isNotEmpty();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testDeployWithInvalidChartSpec() throws IOException, TimeoutException, InterruptedException {
    when(helmCommandHelper.isValidChartSpecification(any())).thenReturn(false);
    when(helmClient.listReleases(any())).thenReturn(HelmCliResponse.builder().commandExecutionStatus(SUCCESS).build());
    when(helmClient.releaseHistory(any()))
        .thenReturn(HelmCliResponse.builder()
                        .commandExecutionStatus(SUCCESS)
                        .output(HelmTestConstants.RELEASE_HIST_V2)
                        .build());

    HelmInstallCommandRequest request =
        HelmInstallCommandRequest.builder().executionLogCallback(executionLogCallback).releaseName("abc").build();

    assertThatThrownBy(() -> helmDeployService.deploy(request))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Couldn't find valid helm chart specification");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testFetchSourceRepoIfNullRepoConfig() {
    HelmInstallCommandRequest request = HelmInstallCommandRequest.builder().build();

    helmDeployService.fetchSourceRepo(request);

    verify(encryptionService, never()).decrypt(any(), anyList());
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testRollback() throws InterruptedException, IOException, TimeoutException {
    setFakeTimeLimiter();
    HelmRollbackCommandRequest request = HelmRollbackCommandRequest.builder()
                                             .containerServiceParams(ContainerServiceParams.builder().build())
                                             .releaseName("first-release")
                                             .build();
    KubernetesConfig kubernetesConfig = new KubernetesConfig();

    when(helmClient.rollback(request))
        .thenReturn(HelmInstallCommandResponse.builder()
                        .output("Rollback was a success.")
                        .commandExecutionStatus(SUCCESS)
                        .build());
    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(K8sClusterConfig.class)))
        .thenReturn(new KubernetesConfig());
    when(containerDeploymentDelegateHelper.getKubernetesConfig(any(ContainerServiceParams.class)))
        .thenReturn(new KubernetesConfig());
    when(containerDeploymentDelegateHelper.getContainerInfosWhenReadyByLabel(
             anyString(), anyString(), any(), any(), any(), eq(Collections.emptyList())))
        .thenReturn(asList(new ContainerInfo()));
    when(containerDeploymentDelegateHelper.getExistingPodsByLabels(
             any(ContainerServiceParams.class), any(KubernetesConfig.class), any(Map.class)))
        .thenReturn(Collections.emptyList());

    HelmInstallCommandResponse response = (HelmInstallCommandResponse) helmDeployService.rollback(request);
    assertThat(response.getContainerInfoList()).isNotEmpty();
  }

  private void successWhenHelm3PresentInClientTools() throws InterruptedException, IOException, TimeoutException {
    doReturn("/client-tools/helm").when(k8sGlobalConfigService).getHelmPath(V3);

    HelmCommandResponse helmCommandResponse = helmDeployService.ensureHelm3Installed(null);

    assertThat(helmCommandResponse.getCommandExecutionStatus()).isEqualTo(SUCCESS);
  }

  private void failureWhenHelm3AbsentInClientTools() throws InterruptedException, IOException, TimeoutException {
    doReturn("").when(k8sGlobalConfigService).getHelmPath(V3);

    HelmCommandResponse helmCommandResponse = helmDeployService.ensureHelm3Installed(null);

    assertThat(helmCommandResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  private void setFakeTimeLimiter() {
    Reflect.on(helmDeployService).set("timeLimiter", timeLimiter);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testFetchRepo() throws Exception {
    shouldCallFetchChartRepo();
    shouldCallFetchSourceRepo();
    shouldThrowExceptionForUnknownStoreType();
  }

  private void shouldThrowExceptionForUnknownStoreType() {
    HelmInstallCommandRequest helmInstallCommandRequest =
        HelmInstallCommandRequest.builder()
            .sourceRepoConfig(K8sDelegateManifestConfig.builder().manifestStoreTypes(StoreType.Local).build())
            .build();

    Assertions.assertThatThrownBy(() -> spyHelmDeployService.fetchRepo(helmInstallCommandRequest))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Unsupported store type");
  }

  private void shouldCallFetchSourceRepo() throws Exception {
    HelmInstallCommandRequest helmInstallCommandRequest =
        HelmInstallCommandRequest.builder()
            .sourceRepoConfig(K8sDelegateManifestConfig.builder().manifestStoreTypes(StoreType.HelmSourceRepo).build())
            .build();
    doNothing().when(spyHelmDeployService).fetchSourceRepo(helmInstallCommandRequest);

    spyHelmDeployService.fetchRepo(helmInstallCommandRequest);

    verify(spyHelmDeployService, times(1)).fetchSourceRepo(helmInstallCommandRequest);
  }

  private void shouldCallFetchChartRepo() throws Exception {
    HelmInstallCommandRequest helmInstallCommandRequest =
        HelmInstallCommandRequest.builder()
            .sourceRepoConfig(K8sDelegateManifestConfig.builder().manifestStoreTypes(StoreType.HelmChartRepo).build())
            .build();
    doNothing().when(spyHelmDeployService).fetchChartRepo(helmInstallCommandRequest);

    spyHelmDeployService.fetchRepo(helmInstallCommandRequest);

    verify(spyHelmDeployService, times(1)).fetchChartRepo(helmInstallCommandRequest);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testEnsureHelmInstalled() {
    shouldCallEnsureHelm3InstalledWhenVersionV3();
    shouldCallEnsureHelm2InstalledWhenVersionV2();
    shouldCallEnsureHelm2InstalledWhenVersionNull();
  }

  private void shouldCallEnsureHelm2InstalledWhenVersionNull() {
    HelmInstallCommandRequest helmInstallCommandRequest = HelmInstallCommandRequest.builder().build();
    doReturn(null).when(spyHelmDeployService).ensureHelmCliAndTillerInstalled(helmInstallCommandRequest);

    spyHelmDeployService.ensureHelmInstalled(helmInstallCommandRequest);

    verify(spyHelmDeployService, times(2)).ensureHelmCliAndTillerInstalled(helmInstallCommandRequest);
  }

  private void shouldCallEnsureHelm2InstalledWhenVersionV2() {
    HelmInstallCommandRequest helmInstallCommandRequest = HelmInstallCommandRequest.builder().helmVersion(V2).build();
    doReturn(null).when(spyHelmDeployService).ensureHelmCliAndTillerInstalled(helmInstallCommandRequest);

    spyHelmDeployService.ensureHelmInstalled(helmInstallCommandRequest);

    verify(spyHelmDeployService, times(1)).ensureHelmCliAndTillerInstalled(helmInstallCommandRequest);
  }

  private void shouldCallEnsureHelm3InstalledWhenVersionV3() {
    HelmInstallCommandRequest helmInstallCommandRequest = HelmInstallCommandRequest.builder().helmVersion(V3).build();
    doReturn(null).when(spyHelmDeployService).ensureHelm3Installed(helmInstallCommandRequest);

    spyHelmDeployService.ensureHelmInstalled(helmInstallCommandRequest);

    verify(spyHelmDeployService, times(1)).ensureHelm3Installed(helmInstallCommandRequest);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testDeleteAndPurgeHelmReleaseName() throws InterruptedException, IOException, TimeoutException {
    HelmInstallCommandRequest helmInstallCommandRequest = HelmInstallCommandRequest.builder().build();
    HelmCliResponse helmCliResponse = HelmCliResponse.builder().build();
    doReturn(helmCliResponse).when(helmClient).deleteHelmRelease(helmInstallCommandRequest);

    helmDeployService.deleteAndPurgeHelmRelease(helmInstallCommandRequest, new ExecutionLogCallback());

    verify(helmClient, times(1)).deleteHelmRelease(helmInstallCommandRequest);
  }
}
