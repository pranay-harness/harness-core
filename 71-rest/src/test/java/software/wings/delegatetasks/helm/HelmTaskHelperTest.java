package software.wings.delegatetasks.helm;

import static io.harness.exception.WingsException.USER;
import static io.harness.k8s.model.HelmVersion.V2;
import static io.harness.k8s.model.HelmVersion.V3;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.PRABU;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.LONG_TIMEOUT_INTERVAL;
import static software.wings.utils.WingsTestConstants.MANIFEST_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import io.harness.beans.FileData;
import io.harness.category.element.UnitTests;
import io.harness.exception.HelmClientException;
import io.harness.exception.InvalidRequestException;
import io.harness.filesystem.FileIo;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.k8s.model.HelmVersion;
import io.harness.rule.Owner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.appmanifest.HelmChart;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.helpers.ext.chartmuseum.ChartMuseumClient;
import software.wings.helpers.ext.chartmuseum.ChartMuseumServer;
import software.wings.helpers.ext.helm.request.HelmChartCollectionParams;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class HelmTaskHelperTest extends WingsBaseTest {
  @Mock private ProcessExecutor processExecutor;
  @Mock K8sGlobalConfigService k8sGlobalConfigService;
  @Mock EncryptionService encryptionService;
  @Mock ChartMuseumClient chartMuseumClient;
  @Spy @InjectMocks private HelmTaskHelper helmTaskHelper;

  @Before
  public void setup() {
    doReturn(processExecutor).when(helmTaskHelper).createProcessExecutor(anyString(), anyString(), anyLong());
    doReturn("v3/helm").when(k8sGlobalConfigService).getHelmPath(V3);
    doReturn("v2/helm").when(k8sGlobalConfigService).getHelmPath(V2);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testExecuteCommand() throws Exception {
    doReturn(new ProcessResult(0, null)).when(processExecutor).execute();
    helmTaskHelper.executeCommand("", ".", "", LONG_TIMEOUT_INTERVAL);

    doThrow(new IOException()).when(processExecutor).execute();
    assertThatExceptionOfType(HelmClientException.class)
        .isThrownBy(() -> helmTaskHelper.executeCommand("", ".", "", LONG_TIMEOUT_INTERVAL))
        .withNoCause()
        .withMessageContaining("[IO exception]");

    doThrow(new InterruptedException()).when(processExecutor).execute();
    assertThatExceptionOfType(HelmClientException.class)
        .isThrownBy(() -> helmTaskHelper.executeCommand("", ".", "foo", LONG_TIMEOUT_INTERVAL))
        .withNoCause()
        .withMessageContaining("[Interrupted] foo");

    doThrow(new TimeoutException()).when(processExecutor).execute();
    assertThatExceptionOfType(HelmClientException.class)
        .isThrownBy(() -> helmTaskHelper.executeCommand("", ".", null, LONG_TIMEOUT_INTERVAL))
        .withNoCause()
        .withMessageContaining("[Timed out]");

    doThrow(new RuntimeException("test")).when(processExecutor).execute();
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> helmTaskHelper.executeCommand("", ".", "", LONG_TIMEOUT_INTERVAL))
        .withNoCause()
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
        .executeCommand(anyString(), anyString(), anyString(), anyLong());
    assertThatExceptionOfType(HelmClientException.class)
        .isThrownBy(()
                        -> helmTaskHelper.addRepo("vault", "vault", "https://helm-server", "admin",
                            "secret-text".toCharArray(), "/home", V3, LONG_TIMEOUT_INTERVAL))
        .withMessageContaining(
            "Failed to add helm repo. Executed command v3/helm repo add vault https://helm-server --username admin --password *******");
  }

  private void testAddRepoIfProcessExecException() {
    doThrow(new HelmClientException("ex"))
        .when(helmTaskHelper)
        .executeCommand(anyString(), anyString(), anyString(), anyLong());

    assertThatExceptionOfType(HelmClientException.class)
        .isThrownBy(()
                        -> helmTaskHelper.addRepo("vault", "vault", "https://helm-server", "admin",
                            "secret-text".toCharArray(), "/home", V3, LONG_TIMEOUT_INTERVAL));
  }

  private void testAddRepoSuccess() throws IOException, InterruptedException, TimeoutException {
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

    doReturn(new ProcessResult(0, new ProcessOutput(new byte[1])))
        .when(helmTaskHelper)
        .executeCommand(anyString(), anyString(), anyString(), anyLong());
    helmTaskHelper.addRepo("vault", "vault", "https://helm-server", "admin", "secret-text".toCharArray(), "/home", V3,
        LONG_TIMEOUT_INTERVAL);

    verify(helmTaskHelper, times(1))
        .executeCommand(captor.capture(), captor.capture(), captor.capture(), eq(LONG_TIMEOUT_INTERVAL));

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
  public void testGetHelmChartInfoFromChartsYamlFileFromInstallCommandRequest() throws Exception {
    HelmInstallCommandRequest request = HelmInstallCommandRequest.builder().workingDir("working/dir").build();
    HelmChartInfo helmChartInfo = HelmChartInfo.builder().build();

    doReturn(helmChartInfo).when(helmTaskHelper).getHelmChartInfoFromChartsYamlFile("working/dir/Chart.yaml");

    assertThat(helmTaskHelper.getHelmChartInfoFromChartsYamlFile(request)).isSameAs(helmChartInfo);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetHelmChartInfoFromChartDirectory() throws Exception {
    HelmChartInfo helmChartInfo = HelmChartInfo.builder().build();

    doReturn(helmChartInfo).when(helmTaskHelper).getHelmChartInfoFromChartsYamlFile("chart/directory/Chart.yaml");

    assertThat(helmTaskHelper.getHelmChartInfoFromChartDirectory("chart/directory")).isSameAs(helmChartInfo);
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
    helmTaskHelper.downloadChartFiles(gcsConfigParams, outputTemporaryDir.toString(), LONG_TIMEOUT_INTERVAL);
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
    helmTaskHelper.downloadChartFiles(awsConfigParams, outputTemporaryDir.toString(), LONG_TIMEOUT_INTERVAL);
    verifyFetchChartFilesProcessExecutor(outputTemporaryDir.toString());
    FileUtils.deleteDirectory(outputTemporaryDir.toFile());
  }

  private void verifyFetchChartFilesProcessExecutor(String outputDirectory) throws Exception {
    verify(helmTaskHelper, times(1))
        .createProcessExecutor(
            "v3/helm repo add repoName http://127.0.0.1:1234", outputDirectory, LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper, times(1))
        .createProcessExecutor("v3/helm pull repoName/chartName --untar ", outputDirectory, LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper, times(1)).createProcessExecutor("v3/helm repo remove repoName", null, LONG_TIMEOUT_INTERVAL);
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
    helmTaskHelper.downloadChartFiles(httpHelmChartConfig, outputTemporaryDir.toString(), LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper, times(1))
        .createProcessExecutor(
            "v3/helm repo add repoName http://127.0.0.1:1234  ", outputTemporaryDir.toString(), LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper, times(1))
        .createProcessExecutor(
            "v3/helm pull repoName/chartName --untar ", outputTemporaryDir.toString(), LONG_TIMEOUT_INTERVAL);
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
    helmTaskHelper.downloadChartFiles(emptyHelmRepoConfig, outputTemporaryDir.toString(), LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper, times(1))
        .createProcessExecutor(
            "v3/helm pull repoName/chartName --untar ", outputTemporaryDir.toString(), LONG_TIMEOUT_INTERVAL);

    // With not empty chart Url
    emptyHelmRepoConfig.setChartUrl("http://127.0.0.1:1234/chart");
    helmTaskHelper.downloadChartFiles(emptyHelmRepoConfig, outputTemporaryDir.toString(), LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper, times(1))
        .createProcessExecutor("v3/helm repo add repoName http://127.0.0.1:1234/chart  ", outputTemporaryDir.toString(),
            LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper, times(2))
        .createProcessExecutor(
            "v3/helm pull repoName/chartName --untar ", outputTemporaryDir.toString(), LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper, times(1)).createProcessExecutor("v3/helm repo remove repoName", null, LONG_TIMEOUT_INTERVAL);
    FileUtils.deleteDirectory(outputTemporaryDir.toFile());
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testDownloadChartFilesForEmptyHelmRepoBySpec() throws Exception {
    HelmChartSpecification helmChartSpecification = getHelmChartSpecification(null);
    helmChartSpecification.setChartName("stable/chartName1");
    Path outputTemporaryDir = Files.createTempDirectory("chartFile");

    ProcessResult successfulResult = new ProcessResult(0, null);
    doReturn(successfulResult).when(processExecutor).execute();

    HelmCommandRequest helmCommandRequest =
        HelmInstallCommandRequest.builder().helmVersion(V3).repoName("repoName").build();

    // With empty chart Url
    helmTaskHelper.downloadChartFiles(
        helmChartSpecification, outputTemporaryDir.toString(), helmCommandRequest, LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper, times(1))
        .createProcessExecutor(
            "v3/helm pull stable/chartName1 --untar ", outputTemporaryDir.toString(), LONG_TIMEOUT_INTERVAL);

    helmChartSpecification.setChartName("chartName2");
    helmTaskHelper.downloadChartFiles(
        helmChartSpecification, outputTemporaryDir.toString(), helmCommandRequest, LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper, times(1))
        .createProcessExecutor(
            "v3/helm pull chartName2 --untar ", outputTemporaryDir.toString(), LONG_TIMEOUT_INTERVAL);

    // With not empty chart Url
    helmChartSpecification.setChartUrl("http://127.0.0.1:1234/chart");
    helmChartSpecification.setChartName("stable/chartName3");
    helmTaskHelper.downloadChartFiles(
        helmChartSpecification, outputTemporaryDir.toString(), helmCommandRequest, LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper, times(1))
        .createProcessExecutor("v3/helm repo add repoName http://127.0.0.1:1234/chart  ", outputTemporaryDir.toString(),
            LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper, times(1))
        .createProcessExecutor(
            "v3/helm pull repoName/stable/chartName3 --untar ", outputTemporaryDir.toString(), LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper, times(1)).createProcessExecutor("v3/helm repo remove repoName", null, LONG_TIMEOUT_INTERVAL);

    helmChartSpecification.setChartName("chartName4");
    helmTaskHelper.downloadChartFiles(
        helmChartSpecification, outputTemporaryDir.toString(), helmCommandRequest, LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper, times(1))
        .createProcessExecutor(
            "v3/helm pull repoName/chartName4 --untar ", outputTemporaryDir.toString(), LONG_TIMEOUT_INTERVAL);

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

    helmTaskHelper.downloadChartFiles(emptyHelmRepoConfig, outputTemporaryDir.toString(), LONG_TIMEOUT_INTERVAL);
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
    verify(executionLogCallback).saveExecutionLog(format("Chart bucket: %s", "bucketName"));
    helmTaskHelper.printHelmChartInfoInExecutionLogs(withHttpConfigRepo, executionLogCallback);
    verify(executionLogCallback).saveExecutionLog(format("Repo url: %s", "http://127.0.0.1:1234"));

    // with chart name
    emptyHelmChartConfigParams.setChartName("chartName");
    helmTaskHelper.printHelmChartInfoInExecutionLogs(emptyHelmChartConfigParams, executionLogCallback);
    verify(executionLogCallback, atLeastOnce()).saveExecutionLog(format("Chart name: %s", "chartName"));

    // with repo display name
    emptyHelmChartConfigParams.setRepoDisplayName("repoDisplayName");
    helmTaskHelper.printHelmChartInfoInExecutionLogs(emptyHelmChartConfigParams, executionLogCallback);
    verify(executionLogCallback, atLeastOnce()).saveExecutionLog(format("Helm repository: %s", "repoDisplayName"));

    // with base path
    emptyHelmChartConfigParams.setBasePath("basePath");
    helmTaskHelper.printHelmChartInfoInExecutionLogs(emptyHelmChartConfigParams, executionLogCallback);
    verify(executionLogCallback, atLeastOnce()).saveExecutionLog(format("Base Path: %s", "basePath"));

    // with chart version
    emptyHelmChartConfigParams.setChartVersion("1.0.0");
    helmTaskHelper.printHelmChartInfoInExecutionLogs(emptyHelmChartConfigParams, executionLogCallback);
    verify(executionLogCallback, atLeastOnce()).saveExecutionLog(format("Chart version: %s", "1.0.0"));

    // with chart url
    emptyHelmChartConfigParams.setChartUrl("http://127.0.0.1");
    helmTaskHelper.printHelmChartInfoInExecutionLogs(emptyHelmChartConfigParams, executionLogCallback);
    verify(executionLogCallback, atLeastOnce()).saveExecutionLog(format("Chart url: %s", "http://127.0.0.1"));

    // with helm version
    emptyHelmChartConfigParams.setHelmVersion(V2);
    helmTaskHelper.printHelmChartInfoInExecutionLogs(emptyHelmChartConfigParams, executionLogCallback);
    verify(executionLogCallback, atLeastOnce()).saveExecutionLog(format("Helm version: %s", V2));
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

  private HelmChartSpecification getHelmChartSpecification(String url) {
    return HelmChartSpecification.builder().chartName("chartName").chartVersion("").chartUrl(url).build();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCreateProcessExecutor() {
    long timeoutMillis = 9000;
    String command = "v3/helm repo remove repoName";
    String workingDir = "/pwd/wd";
    String emptyWorkingDir = "";
    doCallRealMethod().when(helmTaskHelper).createProcessExecutor(anyString(), anyString(), anyLong());

    ProcessExecutor executor = helmTaskHelper.createProcessExecutor(command, workingDir, timeoutMillis);
    assertThat(executor.getDirectory()).isEqualTo(new File(workingDir));
    assertThat(String.join(" ", executor.getCommand())).isEqualTo(command);

    executor = helmTaskHelper.createProcessExecutor(command, emptyWorkingDir, timeoutMillis);
    assertThat(executor.getDirectory()).isNull();
    assertThat(String.join(" ", executor.getCommand())).isEqualTo(command);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testAddRepoWithEmptyPassword() {
    char[] emptyPassword = new char[0];
    char[] passwordWithWhitespaces = new char[] {' ', ' '};
    doReturn(new ProcessResult(0, new ProcessOutput(new byte[1])))
        .when(helmTaskHelper)
        .executeCommand(anyString(), anyString(), anyString(), anyLong());

    helmTaskHelper.addRepo(
        "repo", "repo", "http://null-password-url", "username", null, "chart", V3, LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper)
        .executeCommand(eq("v3/helm repo add repo http://null-password-url --username username "), anyString(),
            anyString(), anyLong());

    helmTaskHelper.addRepo(
        "repo", "repo", "http://repo-url", "username", emptyPassword, "chart", V3, LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper)
        .executeCommand(
            eq("v3/helm repo add repo http://repo-url --username username "), anyString(), anyString(), anyLong());

    helmTaskHelper.addRepo(
        "repo", "repo", "http://repo-url", " ", passwordWithWhitespaces, "chart", V3, LONG_TIMEOUT_INTERVAL);
    verify(helmTaskHelper)
        .executeCommand(eq("v3/helm repo add repo http://repo-url  "), anyString(), anyString(), anyLong());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetValuesYamlFromChart() throws Exception {
    String valuesFileContent = "var: value";
    String chartName = "chartName";
    HelmChartConfigParams helmChartConfigParams =
        HelmChartConfigParams.builder().chartName(chartName).chartVersion("0.1.0").build();
    String workingDirectory = prepareChartDirectoryWithValuesFileForTest(chartName, valuesFileContent);

    doNothing().when(helmTaskHelper).initHelm(anyString(), any(HelmVersion.class), anyLong());
    doReturn(new ProcessResult(0, new ProcessOutput("success".getBytes())))
        .when(helmTaskHelper)
        .executeCommand(
            contains("helm/path fetch chartName --untar"), anyString(), eq("fetch chart chartName"), anyLong());

    try {
      String result = helmTaskHelper.getValuesYamlFromChart(helmChartConfigParams, LONG_TIMEOUT_INTERVAL);
      assertThat(result).isEqualTo(valuesFileContent);
    } finally {
      FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory);
    }
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetValuesYamlFromChartNoValuesYaml() throws Exception {
    String chartName = "chartName";
    HelmChartConfigParams helmChartConfigParams =
        HelmChartConfigParams.builder().chartName(chartName).chartVersion("0.1.0").build();
    String workingDirectory = prepareChartDirectoryWithValuesFileForTest(chartName, null);

    doNothing().when(helmTaskHelper).initHelm(anyString(), any(HelmVersion.class), anyLong());
    doReturn(new ProcessResult(0, new ProcessOutput("success".getBytes())))
        .when(helmTaskHelper)
        .executeCommand(contains("helm/path fetch"), anyString(), eq("fetch chart chartName"), anyLong());

    try {
      String result = helmTaskHelper.getValuesYamlFromChart(helmChartConfigParams, LONG_TIMEOUT_INTERVAL);
      assertThat(result).isNullOrEmpty();
    } finally {
      FileIo.deleteDirectoryAndItsContentIfExists(workingDirectory);
    }
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetValuesYamlFromChartPopulateChartVersion() throws Exception {
    String chartName = "chartName";
    String chartVersion = "1.0.0";
    HelmChartConfigParams helmChartConfigParams = HelmChartConfigParams.builder().chartName(chartName).build();

    doReturn("working/directory").when(helmTaskHelper).createNewDirectoryAtPath(anyString());
    doReturn("helm/path").when(k8sGlobalConfigService).getHelmPath(any(HelmVersion.class));
    doNothing().when(helmTaskHelper).initHelm(anyString(), any(HelmVersion.class), anyLong());
    doReturn(new ProcessResult(0, new ProcessOutput("success".getBytes())))
        .when(helmTaskHelper)
        .executeCommand(contains("helm/path fetch"), anyString(), eq("fetch chart chartName"), anyLong());
    doReturn(HelmChartInfo.builder().version(chartVersion).build())
        .when(helmTaskHelper)
        .getHelmChartInfoFromChartsYamlFile(anyString());

    helmTaskHelper.getValuesYamlFromChart(helmChartConfigParams, LONG_TIMEOUT_INTERVAL);
    assertThat(helmChartConfigParams.getChartVersion()).isEqualTo(chartVersion);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetValuesYamlFromChartUnableToPopulateChartVersion() throws Exception {
    HelmChartConfigParams helmChartConfigParams = HelmChartConfigParams.builder().chartName("chartName").build();

    doReturn("working/directory").when(helmTaskHelper).createNewDirectoryAtPath(anyString());
    doReturn("helm/path").when(k8sGlobalConfigService).getHelmPath(any(HelmVersion.class));
    doNothing().when(helmTaskHelper).initHelm(anyString(), any(HelmVersion.class), anyLong());
    doReturn(new ProcessResult(0, new ProcessOutput("success".getBytes())))
        .when(helmTaskHelper)
        .executeCommand(contains("helm/path fetch"), anyString(), eq("fetch chart chartName"), anyLong());
    doThrow(new RuntimeException("Unable to fetch version"))
        .when(helmTaskHelper)
        .getHelmChartInfoFromChartsYamlFile(anyString());

    assertThatCode(() -> helmTaskHelper.getValuesYamlFromChart(helmChartConfigParams, LONG_TIMEOUT_INTERVAL))
        .doesNotThrowAnyException();
  }

  private String prepareChartDirectoryWithValuesFileForTest(String chartName, String valuesFileContent)
      throws IOException {
    String workingDirectory = Files.createTempDirectory("get-values-yaml-chart").toString();
    Files.createDirectory(Paths.get(workingDirectory, chartName));
    doReturn(workingDirectory).when(helmTaskHelper).createNewDirectoryAtPath(anyString());
    doReturn("helm/path").when(k8sGlobalConfigService).getHelmPath(any(HelmVersion.class));
    if (valuesFileContent != null) {
      Files.write(Paths.get(workingDirectory, chartName, "values.yaml"), valuesFileContent.getBytes());
    }

    return workingDirectory;
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testAddHelmRepo() {
    StartedProcess chartMuseumProcess = mock(StartedProcess.class);
    ProcessResult success = new ProcessResult(0, new ProcessOutput("output".getBytes()));
    ProcessResult failure = new ProcessResult(1, new ProcessOutput("error".getBytes()));

    assertThatCode(() -> testAddHelmRepo(chartMuseumProcess, success)).doesNotThrowAnyException();
    assertThatThrownBy(() -> testAddHelmRepo(chartMuseumProcess, failure))
        .isInstanceOf(HelmClientException.class)
        .hasMessageContaining("Failed to add helm repo. Executed command");
    assertThatThrownBy(() -> testAddHelmRepo(null, null)).isInstanceOf(InvalidRequestException.class);
  }

  private void testAddHelmRepo(StartedProcess chartMuseumProcess, ProcessResult addRepoResult) throws Exception {
    HelmRepoConfig helmRepoConfig = AmazonS3HelmRepoConfig.builder().bucketName("bucket").build();
    SettingValue connector = AwsConfig.builder().encryptedSecretKey("secretKey").build();
    String workingDir = "workingDir";
    String basePath = "base/path";
    String repo = "repo";
    ChartMuseumServer museumServer = null;
    if (chartMuseumProcess != null) {
      museumServer = ChartMuseumServer.builder().port(8888).startedProcess(chartMuseumProcess).build();
    }

    doReturn(workingDir).when(helmTaskHelper).createNewDirectoryAtPath(anyString());
    doReturn(addRepoResult)
        .when(helmTaskHelper)
        .executeCommand(contains("helm repo add"), anyString(), anyString(), anyLong());
    if (museumServer != null) {
      doReturn(museumServer)
          .when(chartMuseumClient)
          .startChartMuseumServer(helmRepoConfig, connector, workingDir, basePath);
    } else {
      doThrow(new InvalidRequestException("Something went wrong"))
          .when(chartMuseumClient)
          .startChartMuseumServer(helmRepoConfig, connector, workingDir, basePath);
    }

    helmTaskHelper.addHelmRepo(helmRepoConfig, connector, repo, repo, workingDir, basePath, V2);
    if (museumServer != null) {
      verify(chartMuseumClient, times(1)).stopChartMuseumServer(chartMuseumProcess);
    }
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetFilteredFiles() {
    FileData file1 = FileData.builder().filePath("dir/file1").build();
    FileData file2 = FileData.builder().filePath("dir/file2").build();
    FileData file3 = FileData.builder().filePath("dir/file3").build();
    List<FileData> emptyFiles = Collections.emptyList();
    List<FileData> singleFile = Collections.singletonList(file1);
    List<FileData> multipleFiles = Arrays.asList(file1, file2, file3);

    assertThat(helmTaskHelper.getFilteredFiles(emptyFiles, Collections.emptyList())).isEmpty();
    assertThat(helmTaskHelper.getFilteredFiles(emptyFiles, Arrays.asList("file1", "file2"))).isEmpty();
    assertThat(helmTaskHelper.getFilteredFiles(singleFile, Collections.emptyList())).isEmpty();
    assertThat(helmTaskHelper.getFilteredFiles(singleFile, Arrays.asList("missing1", "missing2"))).isEmpty();
    assertThat(helmTaskHelper.getFilteredFiles(singleFile, Collections.singletonList("dir/file1")))
        .containsExactlyInAnyOrder(file1);
    assertThat(helmTaskHelper.getFilteredFiles(singleFile, Arrays.asList("dir/file1", "missing")))
        .containsExactlyInAnyOrder(file1);
    assertThat(helmTaskHelper.getFilteredFiles(multipleFiles, Arrays.asList("missing1", "missing2"))).isEmpty();
    assertThat(helmTaskHelper.getFilteredFiles(multipleFiles, Collections.singletonList("dir/file1")))
        .containsExactlyInAnyOrder(file1);
    assertThat(helmTaskHelper.getFilteredFiles(multipleFiles, Arrays.asList("dir/file1", "dir/file2", "dir/file3")))
        .containsExactlyInAnyOrder(file1, file2, file3);
    assertThat(helmTaskHelper.getFilteredFiles(multipleFiles, Arrays.asList("dir/file1", "dir/file2", "misssing")))
        .containsExactlyInAnyOrder(file1, file2);
  }

  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testReturnEmptyHelmChartsForEmptyResponse() throws Exception {
    HttpHelmRepoConfig httpHelmRepoConfig =
        HttpHelmRepoConfig.builder().accountId(ACCOUNT_ID).chartRepoUrl("http://127.0.0.1:1234").build();
    HelmChartCollectionParams helmChartCollectionParams =
        HelmChartCollectionParams.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .appManifestId(MANIFEST_ID)
            .serviceId(SERVICE_ID)
            .helmChartConfigParams(getHelmChartConfigParams(httpHelmRepoConfig))
            .build();
    ChartMuseumServer chartMuseumServer = ChartMuseumServer.builder().build();

    doReturn(new ProcessResult(0, null)).when(processExecutor).execute();
    doAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, String.class))
        .when(helmTaskHelper)
        .createDirectory("dir");
    doReturn(processExecutor)
        .when(helmTaskHelper)
        .createProcessExecutorWithRedirectOutput(eq("v3/helm search repo repoName/chartName -l"), eq("dir"), any());

    List<HelmChart> helmCharts = helmTaskHelper.fetchChartVersions(helmChartCollectionParams, "dir", 10000);
    assertThat(helmCharts).isEmpty();
    // One command for adding directory and another for fetching versions
    verify(processExecutor, times(2)).execute();
    verify(helmTaskHelper, times(1)).initHelm("dir", V3, 10000);

    doReturn("No results Found")
        .when(helmTaskHelper)
        .executeCommandWithLogOutput(eq("v3/helm search repo repoName/chartName -l"), eq("dir"), anyString());
    helmCharts = helmTaskHelper.fetchChartVersions(helmChartCollectionParams, "dir", 10000);
    assertThat(helmCharts).isEmpty();
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testFetchVersionsFromHttp() throws Exception {
    HttpHelmRepoConfig httpHelmRepoConfig =
        HttpHelmRepoConfig.builder().accountId(ACCOUNT_ID).chartRepoUrl("http://127.0.0.1:1234").build();
    HelmChartCollectionParams helmChartCollectionParams =
        HelmChartCollectionParams.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .appManifestId(MANIFEST_ID)
            .serviceId(SERVICE_ID)
            .helmChartConfigParams(getHelmChartConfigParams(httpHelmRepoConfig))
            .build();

    ChartMuseumServer chartMuseumServer = ChartMuseumServer.builder().build();
    doReturn(new ProcessResult(0, null)).when(processExecutor).execute();
    doAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, String.class))
        .when(helmTaskHelper)
        .createDirectory("dir");
    doReturn("NAME\tCHART VERSION\tAPP VERSION\tDESCRIPTION\n"
        + "repoName/chartName\t1\t0\tdesc\n"
        + "repoName/chartName\t2\t0\tdesc")
        .when(helmTaskHelper)
        .executeCommandWithLogOutput(eq("v3/helm search repo repoName/chartName -l"), eq("dir"), anyString());

    List<HelmChart> helmCharts = helmTaskHelper.fetchChartVersions(helmChartCollectionParams, "dir", 10000);
    assertThat(helmCharts.size()).isEqualTo(2);
    assertThat(helmCharts.get(0).getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(helmCharts.get(0).getAppId()).isEqualTo(APP_ID);
    assertThat(helmCharts.get(0).getApplicationManifestId()).isEqualTo(MANIFEST_ID);
    assertThat(helmCharts.get(0).getVersion()).isEqualTo("1");
    assertThat(helmCharts.get(1).getVersion()).isEqualTo("2");
    verify(processExecutor, times(2)).execute();

    doReturn("WARNING: LONG_SINGLE_LINE_WARNING_MESSAGE.NAME\tCHART VERSION\tAPP VERSION\tDESCRIPTION\n"
        + "chartName\t3\t0\tdesc\n"
        + "chartName\t4\t0\tdesc\nchartNameOne\t5\t0\tdesc")
        .when(helmTaskHelper)
        .executeCommandWithLogOutput(eq("v3/helm search repo repoName/chartName -l"), eq("dir"), anyString());
    helmCharts = helmTaskHelper.fetchChartVersions(helmChartCollectionParams, "dir", 10000);
    assertThat(helmCharts).hasSize(2);
    assertThat(helmCharts.get(0).getDescription()).isEqualTo("desc");
    assertThat(helmCharts.get(0).getAppVersion()).isEqualTo("0");
    assertThat(helmCharts.get(0).getVersion()).isEqualTo("3");
    assertThat(helmCharts.get(1).getVersion()).isEqualTo("4");
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testFetchVersionsFromGcs() throws Exception {
    GCSHelmRepoConfig gcsHelmRepoConfig =
        GCSHelmRepoConfig.builder().accountId(ACCOUNT_ID).bucketName("bucketName").build();
    HelmChartConfigParams helmChartConfigParams = getHelmChartConfigParams(gcsHelmRepoConfig);
    HelmChartCollectionParams helmChartCollectionParams = HelmChartCollectionParams.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .appId(APP_ID)
                                                              .appManifestId(MANIFEST_ID)
                                                              .serviceId(SERVICE_ID)
                                                              .helmChartConfigParams(helmChartConfigParams)
                                                              .build();
    ChartMuseumServer chartMuseumServer = ChartMuseumServer.builder().port(1234).build();

    doReturn(chartMuseumServer)
        .when(chartMuseumClient)
        .startChartMuseumServer(gcsHelmRepoConfig, helmChartConfigParams.getConnectorConfig(),
            HelmTaskHelper.RESOURCE_DIR_BASE, helmChartConfigParams.getBasePath());
    doReturn(new ProcessResult(0, null)).when(processExecutor).execute();
    doAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, String.class))
        .when(helmTaskHelper)
        .createNewDirectoryAtPath(HelmTaskHelper.RESOURCE_DIR_BASE);
    doReturn("NAME\tCHART VERSION\tAPP VERSION\tDESCRIPTION\n"
        + "chartName\t1\t0\tdesc\n"
        + "chartName\t2\t0\tdesc\nsuccess")
        .when(helmTaskHelper)
        .executeCommandWithLogOutput(eq("v3/helm search repo repoName/chartName -l"), eq("dir"), anyString());

    List<HelmChart> helmCharts = helmTaskHelper.fetchChartVersions(helmChartCollectionParams, "dir", 10000);
    assertThat(helmCharts.size()).isEqualTo(2);
    assertThat(helmCharts.get(0).getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(helmCharts.get(0).getAppId()).isEqualTo(APP_ID);
    assertThat(helmCharts.get(0).getApplicationManifestId()).isEqualTo(MANIFEST_ID);
    assertThat(helmCharts.get(0).getVersion()).isEqualTo("1");
    assertThat(helmCharts.get(1).getVersion()).isEqualTo("2");
    verify(processExecutor, times(1)).execute();
    verify(chartMuseumClient, times(1))
        .startChartMuseumServer(gcsHelmRepoConfig, helmChartConfigParams.getConnectorConfig(),
            HelmTaskHelper.RESOURCE_DIR_BASE, helmChartConfigParams.getBasePath());
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testFetchHelmVersionsForV2() throws Exception {
    HttpHelmRepoConfig httpHelmRepoConfig =
        HttpHelmRepoConfig.builder().accountId(ACCOUNT_ID).chartRepoUrl("http://127.0.0.1:1234").build();
    HelmChartConfigParams helmChartConfigParams = getHelmChartConfigParams(httpHelmRepoConfig);
    helmChartConfigParams.setHelmVersion(V2);
    HelmChartCollectionParams helmChartCollectionParams = HelmChartCollectionParams.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .appId(APP_ID)
                                                              .appManifestId(MANIFEST_ID)
                                                              .serviceId(SERVICE_ID)
                                                              .helmChartConfigParams(helmChartConfigParams)
                                                              .build();

    doReturn(new ProcessResult(0, null)).when(processExecutor).execute();
    doAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, String.class))
        .when(helmTaskHelper)
        .createDirectory("dir");
    doAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, String.class))
        .when(helmTaskHelper)
        .applyHelmHomePath("v2/helm search repoName/chartName -l ${HELM_HOME_PATH_FLAG}", "dir");
    doReturn("NAME\tCHART VERSION\tAPP VERSION\tDESCRIPTION\n"
        + "chartName\t1\t0\tdesc\n"
        + "chartName\t2\t0\tdesc")
        .when(helmTaskHelper)
        .executeCommandWithLogOutput(
            eq("v2/helm search repoName/chartName -l ${HELM_HOME_PATH_FLAG}"), eq("dir"), anyString());

    List<HelmChart> helmCharts = helmTaskHelper.fetchChartVersions(helmChartCollectionParams, "dir", 10000);
    assertThat(helmCharts.size()).isEqualTo(2);
    assertThat(helmCharts.get(0).getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(helmCharts.get(0).getAppId()).isEqualTo(APP_ID);
    assertThat(helmCharts.get(0).getApplicationManifestId()).isEqualTo(MANIFEST_ID);
    assertThat(helmCharts.get(0).getVersion()).isEqualTo("1");
    assertThat(helmCharts.get(1).getVersion()).isEqualTo("2");
    // For helm version 2, we execute another command for helm init apart from add repo
    verify(processExecutor, times(3)).execute();
    verify(helmTaskHelper, times(1)).initHelm("dir", V2, 10000);
  }

  @Test
  @Owner(developers = PRABU)
  @Category({UnitTests.class})
  public void testFetchVersionsTimesOut() throws Exception {
    GCSHelmRepoConfig gcsHelmRepoConfig =
        GCSHelmRepoConfig.builder().accountId(ACCOUNT_ID).bucketName("bucketName").build();
    HelmChartConfigParams helmChartConfigParams = getHelmChartConfigParams(gcsHelmRepoConfig);
    HelmChartCollectionParams helmChartCollectionParams = HelmChartCollectionParams.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .appId(APP_ID)
                                                              .appManifestId(MANIFEST_ID)
                                                              .serviceId(SERVICE_ID)
                                                              .helmChartConfigParams(helmChartConfigParams)
                                                              .build();
    ChartMuseumServer chartMuseumServer = ChartMuseumServer.builder().port(1234).build();

    doReturn(chartMuseumServer)
        .when(chartMuseumClient)
        .startChartMuseumServer(gcsHelmRepoConfig, helmChartConfigParams.getConnectorConfig(),
            HelmTaskHelper.RESOURCE_DIR_BASE, helmChartConfigParams.getBasePath());
    doAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, String.class))
        .when(helmTaskHelper)
        .createNewDirectoryAtPath(HelmTaskHelper.RESOURCE_DIR_BASE);
    doAnswer(new Answer() {
      private int count = 0;

      public Object answer(InvocationOnMock invocation) throws TimeoutException {
        if (count++ == 0) {
          return new ProcessResult(0, null);
        }
        throw new TimeoutException();
      }
    })
        .when(processExecutor)
        .execute();
    doReturn(processExecutor)
        .when(helmTaskHelper)
        .createProcessExecutorWithRedirectOutput(eq("v3/helm search repo repoName/chartName -l"), eq("dir"), any());

    assertThatThrownBy(() -> helmTaskHelper.fetchChartVersions(helmChartCollectionParams, "dir", 10000))
        .isInstanceOf(HelmClientException.class)
        .hasMessage("[Timed out] Helm chart fetch versions command failed ");
  }

  @Test
  @Owner(developers = PRABU)
  @Category({UnitTests.class})
  public void testCleanupAfterCollection() throws Exception {
    GCSHelmRepoConfig gcsHelmRepoConfig =
        GCSHelmRepoConfig.builder().accountId(ACCOUNT_ID).bucketName("bucketName").build();
    HelmChartConfigParams helmChartConfigParams = getHelmChartConfigParams(gcsHelmRepoConfig);
    HelmChartCollectionParams helmChartCollectionParams = HelmChartCollectionParams.builder()
                                                              .accountId(ACCOUNT_ID)
                                                              .appId(APP_ID)
                                                              .appManifestId(MANIFEST_ID)
                                                              .serviceId(SERVICE_ID)
                                                              .helmChartConfigParams(helmChartConfigParams)
                                                              .build();
    ChartMuseumServer chartMuseumServer = ChartMuseumServer.builder().port(1234).build();

    doNothing().when(chartMuseumClient).stopChartMuseumServer(chartMuseumServer.getStartedProcess());
    doReturn(new ProcessResult(0, null)).when(processExecutor).execute();
    doNothing().when(helmTaskHelper).cleanup("dir");

    helmTaskHelper.cleanupAfterCollection(helmChartCollectionParams, "dir", 10000);
    verify(helmTaskHelper, times(1)).cleanup("dir");
    verify(processExecutor, times(1)).execute();
    verify(chartMuseumClient, never()).stopChartMuseumServer(chartMuseumServer.getStartedProcess());
  }

  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testInitHelm() throws Exception {
    String workingDirectory = "/working/directory";
    String expectedInitCommand = format("v2/helm init -c --skip-refresh --home %s/helm", workingDirectory);
    doReturn(workingDirectory).when(helmTaskHelper).createNewDirectoryAtPath(anyString());
    doReturn(new ProcessResult(0, new ProcessOutput("success".getBytes())))
        .when(helmTaskHelper)
        .executeCommand(expectedInitCommand, workingDirectory, "Initing helm Command " + expectedInitCommand,
            LONG_TIMEOUT_INTERVAL);
    assertThatCode(() -> helmTaskHelper.initHelm("/working/directory", V2, LONG_TIMEOUT_INTERVAL))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testInitHelmFailed() throws Exception {
    String workingDirectory = "/working/directory";
    String expectedInitCommand = format("v2/helm init -c --skip-refresh --home %s/helm", workingDirectory);
    doReturn(workingDirectory).when(helmTaskHelper).createNewDirectoryAtPath(anyString());
    doReturn(new ProcessResult(1, new ProcessOutput("something went wrong executing command".getBytes())))
        .when(helmTaskHelper)
        .executeCommand(expectedInitCommand, workingDirectory, "Initing helm Command " + expectedInitCommand,
            LONG_TIMEOUT_INTERVAL);
    assertThatThrownBy(() -> helmTaskHelper.initHelm("/working/directory", V2, LONG_TIMEOUT_INTERVAL))
        .isInstanceOf(HelmClientException.class)
        .hasMessageContaining("Failed to init helm")
        .hasMessageContaining("something went wrong executing command");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRemoveRepo() {
    String workingDirectory = "/working/directory";
    String repoName = "repoName";
    String expectedRemoveCommand = format("v2/helm repo remove %s --home %s/helm", repoName, workingDirectory);
    doReturn(new ProcessResult(0, new ProcessOutput("success".getBytes())))
        .when(helmTaskHelper)
        .executeCommand(expectedRemoveCommand, null, format("remove helm repo %s", repoName), LONG_TIMEOUT_INTERVAL);

    assertThatCode(() -> helmTaskHelper.removeRepo(repoName, workingDirectory, V2, LONG_TIMEOUT_INTERVAL))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRemoveRepoFailedWithoutAnyExceptions() {
    String workingDirectory = "/working/directory";
    String repoName = "repoName";
    String expectedRemoveCommand = format("v2/helm repo remove %s --home %s/helm", repoName, workingDirectory);
    doReturn(new ProcessResult(1, new ProcessOutput("something went wrong executing command".getBytes())))
        .when(helmTaskHelper)
        .executeCommand(expectedRemoveCommand, null, format("remove helm repo %s", repoName), LONG_TIMEOUT_INTERVAL);

    assertThatCode(() -> helmTaskHelper.removeRepo(repoName, workingDirectory, V2, LONG_TIMEOUT_INTERVAL))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRemoveRepoFailedWithException() {
    String workingDirectory = "/working/directory";
    String repoName = "repoName";
    String expectedRemoveCommand = format("v2/helm repo remove %s --home %s/helm", repoName, workingDirectory);
    doThrow(new InvalidRequestException("Something went wrong", USER))
        .when(helmTaskHelper)
        .executeCommand(expectedRemoveCommand, null, format("remove helm repo %s", repoName), LONG_TIMEOUT_INTERVAL);

    assertThatCode(() -> helmTaskHelper.removeRepo(repoName, workingDirectory, V2, LONG_TIMEOUT_INTERVAL))
        .doesNotThrowAnyException();
  }
}
