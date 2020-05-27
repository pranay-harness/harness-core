package software.wings.delegatetasks.helm;

import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion.V2;
import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion.V3;

import io.harness.category.element.UnitTests;
import io.harness.exception.HelmClientException;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.helpers.ext.chartmuseum.ChartMuseumClient;
import software.wings.helpers.ext.chartmuseum.ChartMuseumServer;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeoutException;

public class HelmTaskHelperTest extends WingsBaseTest {
  @Mock private ProcessExecutor processExecutor;
  @Mock K8sGlobalConfigService k8sGlobalConfigService;
  @Mock EncryptionService encryptionService;
  @Mock ChartMuseumClient chartMuseumClient;
  @Spy @InjectMocks private HelmTaskHelper helmTaskHelper;

  @Before
  public void setup() {
    doReturn(processExecutor).when(helmTaskHelper).createProcessExecutor(anyString(), anyString());
    doReturn("v3/helm").when(k8sGlobalConfigService).getHelmPath(V3);
    doReturn("v2/helm").when(k8sGlobalConfigService).getHelmPath(V2);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testExecuteCommand() throws Exception {
    doReturn(new ProcessResult(0, null)).when(processExecutor).execute();
    helmTaskHelper.executeCommand("", ".", "");

    doThrow(new IOException()).when(processExecutor).execute();
    Assertions.assertThatExceptionOfType(HelmClientException.class)
        .isThrownBy(() -> helmTaskHelper.executeCommand("", ".", ""))
        .withMessageContaining("[IO exception]");

    doThrow(new InterruptedException()).when(processExecutor).execute();
    Assertions.assertThatExceptionOfType(HelmClientException.class)
        .isThrownBy(() -> helmTaskHelper.executeCommand("", ".", "foo"))
        .withMessageContaining("[Interrupted] foo");

    doThrow(new TimeoutException()).when(processExecutor).execute();
    Assertions.assertThatExceptionOfType(HelmClientException.class)
        .isThrownBy(() -> helmTaskHelper.executeCommand("", ".", null))
        .withMessageContaining("[Timed out]");

    doThrow(new RuntimeException("test")).when(processExecutor).execute();
    Assertions.assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> helmTaskHelper.executeCommand("", ".", ""))
        .withMessageContaining("test");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testAddRepo() throws Exception {
    testAddRepoSuccess();
    testAddRepoIfProcessExecException();
    testAddRepoIfHelmCommandFailed();
  }

  private void testAddRepoIfHelmCommandFailed() {
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

    doReturn(new ProcessResult(1, new ProcessOutput(new byte[1])))
        .when(helmTaskHelper)
        .executeCommand(anyString(), anyString(), anyString());
    assertThatExceptionOfType(HelmClientException.class)
        .isThrownBy(()
                        -> helmTaskHelper.addRepo(
                            "vault", "vault", "https://helm-server", "admin", "secret-text".toCharArray(), "/home", V3))
        .withMessageContaining(
            "Failed to add helm repo. Executed command v3/helm repo add vault https://helm-server --username admin --password *******");
  }

  private void testAddRepoIfProcessExecException() {
    doThrow(new HelmClientException("ex")).when(helmTaskHelper).executeCommand(anyString(), anyString(), anyString());

    assertThatExceptionOfType(HelmClientException.class)
        .isThrownBy(()
                        -> helmTaskHelper.addRepo("vault", "vault", "https://helm-server", "admin",
                            "secret-text".toCharArray(), "/home", V3));
  }

  private void testAddRepoSuccess() throws IOException, InterruptedException, TimeoutException {
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

    doReturn(new ProcessResult(0, new ProcessOutput(new byte[1])))
        .when(helmTaskHelper)
        .executeCommand(anyString(), anyString(), anyString());
    helmTaskHelper.addRepo("vault", "vault", "https://helm-server", "admin", "secret-text".toCharArray(), "/home", V3);

    verify(helmTaskHelper, times(1)).executeCommand(captor.capture(), captor.capture(), captor.capture());

    assertThat(captor.getAllValues().get(0))
        .isEqualTo("v3/helm repo add vault https://helm-server --username admin --password secret-text");
    assertThat(captor.getAllValues().get(1)).isEqualTo("/home");
    assertThat(captor.getAllValues().get(2))
        .isEqualTo(
            "add helm repo. Executed commandv3/helm repo add vault https://helm-server --username admin --password *******");
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testgetHelmChartInfoFromChartsYamlFile() throws Exception {
    String chartYaml = "apiVersion: v1\n"
        + "appVersion: \"1.0\"\n"
        + "description: A Helm chart for Kubernetes\n"
        + "name: my-chart\n"
        + "version: 0.1.0";
    File file = File.createTempFile("Chart", ".yaml");
    try (OutputStreamWriter outputStreamWriter = new FileWriter(file)) {
      IOUtils.write(chartYaml, outputStreamWriter);
    }

    HelmChartInfo helmChartInfo = helmTaskHelper.getHelmChartInfoFromChartsYamlFile(file.getPath());
    assertThat(helmChartInfo).isNotNull();
    assertThat(helmChartInfo.getVersion()).isEqualTo("0.1.0");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testDownloadChartFilesForGCSHelmRepo() throws Exception {
    ChartMuseumServer chartMuseumServer = ChartMuseumServer.builder().port(1234).build();
    GCSHelmRepoConfig gcsHelmRepoConfig =
        GCSHelmRepoConfig.builder().accountId("accountId").bucketName("bucketName").build();

    HelmChartConfigParams gcsConfigParams = getHelmChartConfigParams(gcsHelmRepoConfig);

    Path outputTemporaryDir = Files.createTempDirectory("chartFile");
    ProcessResult successfulResult = new ProcessResult(0, null);

    doReturn(chartMuseumServer)
        .when(chartMuseumClient)
        .startChartMuseumServer(eq(gcsHelmRepoConfig), any(SettingValue.class), anyString(), anyString());
    doReturn(successfulResult).when(processExecutor).execute();
    helmTaskHelper.downloadChartFiles(gcsConfigParams, outputTemporaryDir.toString());
    verifyFetchChartFilesProcessExecutor(outputTemporaryDir.toString());
    FileUtils.deleteDirectory(outputTemporaryDir.toFile());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testDownloadChartFilesForAwsS3HelmRepo() throws Exception {
    ChartMuseumServer chartMuseumServer = ChartMuseumServer.builder().port(1234).build();
    AmazonS3HelmRepoConfig s3HelmRepoConfig =
        AmazonS3HelmRepoConfig.builder().accountId("accountId").bucketName("bucketName").build();

    HelmChartConfigParams awsConfigParams = getHelmChartConfigParams(s3HelmRepoConfig);

    Path outputTemporaryDir = Files.createTempDirectory("chartFile");
    ProcessResult successfulResult = new ProcessResult(0, null);

    doReturn(chartMuseumServer)
        .when(chartMuseumClient)
        .startChartMuseumServer(eq(s3HelmRepoConfig), any(SettingValue.class), anyString(), anyString());
    doReturn(successfulResult).when(processExecutor).execute();
    helmTaskHelper.downloadChartFiles(awsConfigParams, outputTemporaryDir.toString());
    verifyFetchChartFilesProcessExecutor(outputTemporaryDir.toString());
    FileUtils.deleteDirectory(outputTemporaryDir.toFile());
  }

  private void verifyFetchChartFilesProcessExecutor(String outputDirectory) throws Exception {
    verify(helmTaskHelper, times(1))
        .createProcessExecutor("v3/helm repo add repoName http://127.0.0.1:1234", outputDirectory);
    verify(helmTaskHelper, times(1)).createProcessExecutor("v3/helm pull repoName/chartName --untar ", outputDirectory);
    verify(helmTaskHelper, times(1)).createProcessExecutor("v3/helm repo remove repoName", null);
    verify(processExecutor, times(3)).execute();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testDownloadChartFilesForHttpHelmRepo() throws Exception {
    ChartMuseumServer chartMuseumServer = ChartMuseumServer.builder().port(1234).build();
    HttpHelmRepoConfig httpHelmRepoConfig =
        HttpHelmRepoConfig.builder().accountId("accountId").chartRepoUrl("http://127.0.0.1:1234").build();

    HelmChartConfigParams httpHelmChartConfig = getHelmChartConfigParams(httpHelmRepoConfig);

    Path outputTemporaryDir = Files.createTempDirectory("chartFile");
    ProcessResult successfulResult = new ProcessResult(0, null);

    doReturn(chartMuseumServer)
        .when(chartMuseumClient)
        .startChartMuseumServer(eq(httpHelmRepoConfig), any(SettingValue.class), anyString(), anyString());
    doReturn(successfulResult).when(processExecutor).execute();
    helmTaskHelper.downloadChartFiles(httpHelmChartConfig, outputTemporaryDir.toString());
    verify(helmTaskHelper, times(1))
        .createProcessExecutor("v3/helm repo add repoName http://127.0.0.1:1234  ", outputTemporaryDir.toString());
    verify(helmTaskHelper, times(1))
        .createProcessExecutor("v3/helm pull repoName/chartName --untar ", outputTemporaryDir.toString());
    verify(processExecutor, times(2)).execute();
    FileUtils.deleteDirectory(outputTemporaryDir.toFile());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testDownloadChartFilesForEmptyHelmRepo() throws Exception {
    HelmChartConfigParams emptyHelmRepoConfig = getHelmChartConfigParams(null);
    Path outputTemporaryDir = Files.createTempDirectory("chartFile");

    ProcessResult successfulResult = new ProcessResult(0, null);
    doReturn(successfulResult).when(processExecutor).execute();

    // With empty chart Url
    helmTaskHelper.downloadChartFiles(emptyHelmRepoConfig, outputTemporaryDir.toString());
    verify(helmTaskHelper, times(1))
        .createProcessExecutor("v3/helm pull repoName/chartName --untar ", outputTemporaryDir.toString());

    // With not empty chart Url
    emptyHelmRepoConfig.setChartUrl("http://127.0.0.1:1234/chart");
    helmTaskHelper.downloadChartFiles(emptyHelmRepoConfig, outputTemporaryDir.toString());
    verify(helmTaskHelper, times(1))
        .createProcessExecutor(
            "v3/helm repo add repoName http://127.0.0.1:1234/chart  ", outputTemporaryDir.toString());
    verify(helmTaskHelper, times(2))
        .createProcessExecutor("v3/helm pull repoName/chartName --untar ", outputTemporaryDir.toString());
    verify(helmTaskHelper, times(1)).createProcessExecutor("v3/helm repo remove repoName", null);
    FileUtils.deleteDirectory(outputTemporaryDir.toFile());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testDownloadChartFileFailed() throws Exception {
    HelmChartConfigParams emptyHelmRepoConfig = getHelmChartConfigParams(null);
    Path outputTemporaryDir = Files.createTempDirectory("chartFile");

    ProcessResult failedResult = new ProcessResult(1, null);
    doReturn(failedResult).when(processExecutor).execute();

    helmTaskHelper.downloadChartFiles(emptyHelmRepoConfig, outputTemporaryDir.toString());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testPrintHelmChartInfoInExecutionLogs() {
    ExecutionLogCallback executionLogCallback = mock(ExecutionLogCallback.class);
    HelmChartConfigParams withAmazonS3ConfigRepo = getHelmChartConfigParams(
        AmazonS3HelmRepoConfig.builder().accountId("accountId").bucketName("bucketName").build());
    HelmChartConfigParams withHttpConfigRepo = getHelmChartConfigParams(
        HttpHelmRepoConfig.builder().accountId("accountId").chartRepoUrl("http://127.0.0.1:1234").build());
    withHttpConfigRepo.setChartName(null);
    HelmChartConfigParams emptyHelmChartConfigParams = getHelmChartConfigParams(null);
    emptyHelmChartConfigParams.setHelmVersion(null);

    helmTaskHelper.printHelmChartInfoInExecutionLogs(withAmazonS3ConfigRepo, executionLogCallback);
    verify(executionLogCallback).saveExecutionLog(String.format("Chart bucket: %s", "bucketName"));
    helmTaskHelper.printHelmChartInfoInExecutionLogs(withHttpConfigRepo, executionLogCallback);
    verify(executionLogCallback).saveExecutionLog(String.format("Repo url: %s", "http://127.0.0.1:1234"));

    // with chart name
    emptyHelmChartConfigParams.setChartName("chartName");
    helmTaskHelper.printHelmChartInfoInExecutionLogs(emptyHelmChartConfigParams, executionLogCallback);
    verify(executionLogCallback, atLeastOnce()).saveExecutionLog(String.format("Chart name: %s", "chartName"));

    // with repo display name
    emptyHelmChartConfigParams.setRepoDisplayName("repoDisplayName");
    helmTaskHelper.printHelmChartInfoInExecutionLogs(emptyHelmChartConfigParams, executionLogCallback);
    verify(executionLogCallback, atLeastOnce())
        .saveExecutionLog(String.format("Helm repository: %s", "repoDisplayName"));

    // with base path
    emptyHelmChartConfigParams.setBasePath("basePath");
    helmTaskHelper.printHelmChartInfoInExecutionLogs(emptyHelmChartConfigParams, executionLogCallback);
    verify(executionLogCallback, atLeastOnce()).saveExecutionLog(String.format("Base Path: %s", "basePath"));

    // with chart version
    emptyHelmChartConfigParams.setChartVersion("1.0.0");
    helmTaskHelper.printHelmChartInfoInExecutionLogs(emptyHelmChartConfigParams, executionLogCallback);
    verify(executionLogCallback, atLeastOnce()).saveExecutionLog(String.format("Chart version: %s", "1.0.0"));

    // with chart url
    emptyHelmChartConfigParams.setChartUrl("http://127.0.0.1");
    helmTaskHelper.printHelmChartInfoInExecutionLogs(emptyHelmChartConfigParams, executionLogCallback);
    verify(executionLogCallback, atLeastOnce()).saveExecutionLog(String.format("Chart url: %s", "http://127.0.0.1"));

    // with helm version
    emptyHelmChartConfigParams.setHelmVersion(V2);
    helmTaskHelper.printHelmChartInfoInExecutionLogs(emptyHelmChartConfigParams, executionLogCallback);
    verify(executionLogCallback, atLeastOnce()).saveExecutionLog(String.format("Helm version: %s", V2));
  }

  private HelmChartConfigParams getHelmChartConfigParams(HelmRepoConfig repoConfig) {
    SettingValue connectorConfig = null;
    if (repoConfig instanceof GCSHelmRepoConfig) {
      connectorConfig = GcpConfig.builder().accountId("accountId").build();
    }

    return HelmChartConfigParams.builder()
        .chartName("chartName")
        .helmRepoConfig(repoConfig)
        .connectorConfig(connectorConfig)
        .helmVersion(V3)
        .repoName("repoName")
        .build();
  }
}