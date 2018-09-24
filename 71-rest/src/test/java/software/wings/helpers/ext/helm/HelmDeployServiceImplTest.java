package software.wings.helpers.ext.helm;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.TimeLimiter;

import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.delegatetasks.helm.HelmCommandHelper;
import software.wings.helpers.ext.helm.HelmClientImpl.HelmCliResponse;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class HelmDeployServiceImplTest extends WingsBaseTest {
  private static final String HELM_RELEASE_NAME_KEY = "helm-release-name";
  private static final String HELM_KUBE_CONFIG_LOCATION_KEY = "helm-kube-config-location";
  private static final String GIT_FILE_CONTENT_1_KEY = "git-file-content-1";
  private static final String GIT_FILE_CONTENT_2_KEY = "git-file-content-2";
  private static final String GIT_FILE_CONTENT_3_KEY = "git-file-content-3";
  private static final String FILE_PATH_KEY = "file/Path";
  private static final String CHART_NAME_KEY = "chart-name";
  private static final String LIST_RELEASE_RESPONSE_KEY =
      "NAME                                     \tREVISION\tUPDATED                 \tSTATUS  \tCHART         \tNAMESPACE\n"
      + "helm-release-name\t85      \tThu Aug  9 23:19:57 2018\tDEPLOYED\ttodolist-0.1.0\tdefault  ";

  @Mock private HelmClient helmClient;
  @Mock private TimeLimiter timeLimiter;
  @Mock private GitService gitService;
  @Mock private EncryptionService encryptionService;
  @Mock private HelmCommandHelper helmCommandHelper;
  @InjectMocks private HelmDeployServiceImpl helmDeployService;

  private HelmInstallCommandRequest helmInstallCommandRequest;
  private HelmInstallCommandResponse helmInstallCommandResponse;
  private HelmCliResponse helmCliReleaseHistoryResponse;
  private HelmCliResponse helmCliListReleasesResponse;
  private GitFileConfig gitFileConfig;
  ExecutionLogCallback executionLogCallback;

  @Before
  public void setUp() {
    helmInstallCommandRequest = createHelmInstallCommandRequest();
    helmInstallCommandResponse = createHelmInstallCommandResponse();
    helmCliReleaseHistoryResponse = createHelmCliResponse();
    helmCliListReleasesResponse = createHelmCliResponse();
    gitFileConfig = new GitFileConfig();
    gitFileConfig.setFilePath(FILE_PATH_KEY);
    executionLogCallback = mock(ExecutionLogCallback.class);
    doNothing().when(executionLogCallback).saveExecutionLog(anyString());
    when(encryptionService.decrypt(any(), any())).thenReturn(null);
    when(gitService.fetchFilesByPath(any(), any(), any(), any(), any(), anyBoolean()))
        .thenReturn(GitFetchFilesResult.builder()
                        .files(asList(GitFile.builder().fileContent(GIT_FILE_CONTENT_1_KEY).build(),
                            GitFile.builder().fileContent(GIT_FILE_CONTENT_2_KEY).build()))
                        .build());
    when(helmCommandHelper.checkValidChartSpecification(any())).thenReturn(true);
    when(helmCommandHelper.generateHelmDeployChartSpecFromYaml(any())).thenReturn(Optional.empty());
  }

  @Test
  public void testDeployInstall() throws InterruptedException, TimeoutException, IOException, ExecutionException {
    helmCliReleaseHistoryResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
    helmCliListReleasesResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);

    when(helmClient.releaseHistory(HELM_KUBE_CONFIG_LOCATION_KEY, HELM_RELEASE_NAME_KEY))
        .thenReturn(helmCliReleaseHistoryResponse);
    when(helmClient.install(any())).thenReturn(helmInstallCommandResponse);
    when(helmClient.listReleases(any())).thenReturn(helmCliListReleasesResponse);

    HelmCommandResponse helmCommandResponse = helmDeployService.deploy(helmInstallCommandRequest, executionLogCallback);
    assertThat(helmCommandResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(helmClient).install(helmInstallCommandRequest);
  }

  @Test
  public void testDeployUpgrade() throws InterruptedException, TimeoutException, IOException, ExecutionException {
    helmCliReleaseHistoryResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
    helmCliListReleasesResponse.setOutput(LIST_RELEASE_RESPONSE_KEY);
    helmCliListReleasesResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);

    when(helmClient.releaseHistory(HELM_KUBE_CONFIG_LOCATION_KEY, HELM_RELEASE_NAME_KEY))
        .thenReturn(helmCliReleaseHistoryResponse);
    when(helmClient.upgrade(any())).thenReturn(helmInstallCommandResponse);
    when(helmClient.listReleases(any())).thenReturn(helmCliListReleasesResponse);

    HelmCommandResponse helmCommandResponse = helmDeployService.deploy(helmInstallCommandRequest, executionLogCallback);
    assertThat(helmCommandResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(helmClient).upgrade(helmInstallCommandRequest);
  }

  @Test
  public void testAddYamlValuesFromGitRepo()
      throws InterruptedException, TimeoutException, IOException, ExecutionException {
    helmInstallCommandRequest.setGitConfig(GitConfig.builder().build());
    helmInstallCommandRequest.setGitFileConfig(gitFileConfig);
    helmCliReleaseHistoryResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
    helmCliListReleasesResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);

    when(helmClient.releaseHistory(HELM_KUBE_CONFIG_LOCATION_KEY, HELM_RELEASE_NAME_KEY))
        .thenReturn(helmCliReleaseHistoryResponse);
    when(helmClient.listReleases(any())).thenReturn(helmCliListReleasesResponse);
    when(helmClient.install(any())).thenReturn(helmInstallCommandResponse);

    ArgumentCaptor<HelmInstallCommandRequest> argumentCaptor = ArgumentCaptor.forClass(HelmInstallCommandRequest.class);
    HelmCommandResponse helmCommandResponse = helmDeployService.deploy(helmInstallCommandRequest, executionLogCallback);
    assertThat(helmCommandResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(helmClient).install(argumentCaptor.capture());
    HelmInstallCommandRequest commandRequest = argumentCaptor.getAllValues().get(0);
    assertThat(commandRequest.getVariableOverridesYamlFiles().size()).isEqualTo(2);
    assertThat(commandRequest.getVariableOverridesYamlFiles()).contains(GIT_FILE_CONTENT_1_KEY, GIT_FILE_CONTENT_2_KEY);
  }

  @Test
  public void testAddYamlValuesFromGitRepoWithVariableYamlFiles()
      throws InterruptedException, TimeoutException, IOException, ExecutionException {
    helmInstallCommandRequest.setGitConfig(GitConfig.builder().build());
    helmInstallCommandRequest.setGitFileConfig(gitFileConfig);
    helmInstallCommandRequest.setVariableOverridesYamlFiles(asList(GIT_FILE_CONTENT_3_KEY));
    helmCliReleaseHistoryResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
    helmCliListReleasesResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);

    when(helmClient.releaseHistory(HELM_KUBE_CONFIG_LOCATION_KEY, HELM_RELEASE_NAME_KEY))
        .thenReturn(helmCliReleaseHistoryResponse);
    when(helmClient.listReleases(any())).thenReturn(helmCliListReleasesResponse);
    when(helmClient.install(any())).thenReturn(helmInstallCommandResponse);

    ArgumentCaptor<HelmInstallCommandRequest> argumentCaptor = ArgumentCaptor.forClass(HelmInstallCommandRequest.class);
    HelmCommandResponse helmCommandResponse = helmDeployService.deploy(helmInstallCommandRequest, executionLogCallback);
    assertThat(helmCommandResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(helmClient).install(argumentCaptor.capture());
    HelmInstallCommandRequest commandRequest = argumentCaptor.getAllValues().get(0);
    assertThat(commandRequest.getVariableOverridesYamlFiles().size()).isEqualTo(3);
    assertThat(commandRequest.getVariableOverridesYamlFiles())
        .contains(GIT_FILE_CONTENT_1_KEY, GIT_FILE_CONTENT_2_KEY, GIT_FILE_CONTENT_3_KEY);
  }

  @Test(expected = WingsException.class)
  public void testAddValuesYamlFromGitRepoWithException()
      throws InterruptedException, TimeoutException, IOException, ExecutionException {
    helmInstallCommandRequest.setGitConfig(GitConfig.builder().build());
    helmCliListReleasesResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);

    GitFileConfig gitFileConfig = new GitFileConfig();
    gitFileConfig.setFilePath(FILE_PATH_KEY);
    helmInstallCommandRequest.setGitFileConfig(gitFileConfig);
    helmInstallCommandRequest.setVariableOverridesYamlFiles(asList(GIT_FILE_CONTENT_3_KEY));
    helmCliReleaseHistoryResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);

    when(helmClient.releaseHistory(HELM_KUBE_CONFIG_LOCATION_KEY, HELM_RELEASE_NAME_KEY))
        .thenReturn(helmCliReleaseHistoryResponse);
    when(helmClient.install(any())).thenReturn(helmInstallCommandResponse);
    when(gitService.fetchFilesByPath(any(), any(), any(), any(), any(), anyBoolean()))
        .thenThrow(new WingsException("WingsException"));
    when(helmClient.listReleases(any())).thenReturn(helmCliListReleasesResponse);

    helmDeployService.deploy(helmInstallCommandRequest, executionLogCallback);
  }

  private HelmInstallCommandRequest createHelmInstallCommandRequest() {
    return HelmInstallCommandRequest.builder()
        .releaseName(HELM_RELEASE_NAME_KEY)
        .kubeConfigLocation(HELM_KUBE_CONFIG_LOCATION_KEY)
        .chartSpecification(HelmChartSpecification.builder().chartName(CHART_NAME_KEY).build())
        .build();
  }

  private HelmCliResponse createHelmCliResponse() {
    return HelmCliResponse.builder().build();
  }

  private HelmInstallCommandResponse createHelmInstallCommandResponse() {
    return HelmInstallCommandResponse.builder()
        .output("")
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .build();
  }
}
