package io.harness.delegate.task.helm;

import static io.harness.chartmuseum.ChartMuseumConstants.CHART_MUSEUM_SERVER_URL;
import static io.harness.delegate.task.helm.HelmTaskHelperBase.RESOURCE_DIR_BASE;
import static io.harness.k8s.model.HelmVersion.V3;
import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.chartmuseum.ChartMuseumServer;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthType;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthenticationDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmUsernamePasswordDTO;
import io.harness.delegate.beans.storeconfig.HttpHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.S3HelmStoreDelegateConfig;
import io.harness.delegate.chartmuseum.NGChartMuseumService;
import io.harness.delegate.task.k8s.HelmChartManifestDelegateConfig;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.K8sGlobalConfigService;
import io.harness.k8s.model.HelmVersion;
import io.harness.rule.Owner;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class HelmTaskHelperBaseTest extends CategoryTest {
  private static final String CHART_NAME = "test-helm-chart";
  private static final String CHART_VERSION = "1.0.0";
  private static final String REPO_NAME = "helm_charts";
  private static final String REPO_DISPLAY_NAME = "Helm Charts";

  @Mock K8sGlobalConfigService k8sGlobalConfigService;
  @Mock NGChartMuseumService ngChartMuseumService;

  @InjectMocks HelmTaskHelperBase helmTaskHelperBase;

  final HelmCommandFlag emptyHelmCommandFlag = HelmCommandFlag.builder().build();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    doReturn("v2/helm").when(k8sGlobalConfigService).getHelmPath(HelmVersion.V2);
    doReturn("v3/helm").when(k8sGlobalConfigService).getHelmPath(V3);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetHelmFetchCommandV2() {
    String workingDirectory = "/pwd";
    String expectedCommand = String.format("v2/helm fetch %s/%s --untar --version %s --home %s/helm", REPO_NAME,
        CHART_NAME, CHART_VERSION, workingDirectory);

    String fetchCommand = helmTaskHelperBase.getHelmFetchCommand(
        CHART_NAME, CHART_VERSION, REPO_NAME, workingDirectory, HelmVersion.V2, emptyHelmCommandFlag);
    assertThat(fetchCommand).isEqualTo(expectedCommand);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetHelmFetchCommandV3() {
    String expectedCommand =
        String.format("v3/helm pull %s/%s  --untar --version %s", REPO_NAME, CHART_NAME, CHART_VERSION);

    String fetchCommand =
        helmTaskHelperBase.getHelmFetchCommand(CHART_NAME, CHART_VERSION, REPO_NAME, "/pwd", V3, emptyHelmCommandFlag);
    assertThat(fetchCommand).isEqualTo(expectedCommand);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetHelmFetchCommandEmptyRepoName() {
    String chartName = "chart-empty-repo";
    String chartVersion = "1.0.0";
    String expectedCommand = String.format("v3/helm pull %s  --untar --version %s", chartName, chartVersion);

    String fetchCommand =
        helmTaskHelperBase.getHelmFetchCommand(chartName, chartVersion, "", "/pwd", V3, emptyHelmCommandFlag);
    assertThat(fetchCommand).isEqualTo(expectedCommand);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testAddRepo() {
    HelmTaskHelperBase spyTaskHelperBase = spy(helmTaskHelperBase);
    ArgumentCaptor<String> commandCaptor = ArgumentCaptor.forClass(String.class);

    doReturn(new ProcessResult(0, new ProcessOutput(null)))
        .when(spyTaskHelperBase)
        .executeCommand(anyString(), anyString(), anyString(), anyLong());

    assertThatCode(()
                       -> spyTaskHelperBase.addRepo(REPO_NAME, REPO_DISPLAY_NAME, "https://repo", "user",
                           "password".toCharArray(), "/dir", V3, 90000))
        .doesNotThrowAnyException();

    verify(spyTaskHelperBase, times(1)).executeCommand(commandCaptor.capture(), eq("/dir"), anyString(), eq(90000L));

    assertThat(commandCaptor.getValue()).contains("repo add " + REPO_NAME);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testFetchChartFromRepo() {
    HelmTaskHelperBase spyTaskHelperBase = spy(helmTaskHelperBase);

    doReturn(new ProcessResult(0, new ProcessOutput(null)))
        .when(spyTaskHelperBase)
        .executeCommand(anyString(), anyString(), anyString(), anyLong());

    assertThatCode(()
                       -> spyTaskHelperBase.fetchChartFromRepo(
                           "repo", "repo display", "chart", "1.0.0", "/dir", V3, emptyHelmCommandFlag, 90000))
        .doesNotThrowAnyException();

    verify(spyTaskHelperBase, times(1)).executeCommand(anyString(), anyString(), anyString(), anyLong());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testFetchChartFromRepoFailed() {
    HelmTaskHelperBase spyTaskHelperBase = spy(helmTaskHelperBase);
    String cliError = "some helm command error";

    doReturn(new ProcessResult(123, new ProcessOutput(cliError.getBytes())))
        .when(spyTaskHelperBase)
        .executeCommand(anyString(), anyString(), anyString(), anyLong());

    assertThatThrownBy(()
                           -> spyTaskHelperBase.fetchChartFromRepo(REPO_NAME, REPO_DISPLAY_NAME, CHART_NAME,
                               CHART_VERSION, "/dir", V3, emptyHelmCommandFlag, 90000))
        .isInstanceOf(InvalidRequestException.class);

    verify(spyTaskHelperBase, times(1)).executeCommand(anyString(), anyString(), anyString(), anyLong());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testDownloadChartFilesFromHttpRepo() {
    HelmTaskHelperBase spyTaskHelperBase = spy(helmTaskHelperBase);
    String repoUrl = "https://repo-chart/";
    String username = "username";
    char[] password = "password".toCharArray();
    String chartOutput = "/directory";
    long timeout = 90000L;

    HelmChartManifestDelegateConfig helmChartManifestDelegateConfig =
        HelmChartManifestDelegateConfig.builder()
            .chartName(CHART_NAME)
            .chartVersion(CHART_VERSION)
            .helmVersion(V3)
            .helmCommandFlag(emptyHelmCommandFlag)
            .storeDelegateConfig(
                HttpHelmStoreDelegateConfig.builder()
                    .repoName(REPO_NAME)
                    .repoDisplayName(REPO_DISPLAY_NAME)
                    .httpHelmConnector(
                        HttpHelmConnectorDTO.builder()
                            .helmRepoUrl(repoUrl)
                            .auth(HttpHelmAuthenticationDTO.builder()
                                      .authType(HttpHelmAuthType.USER_PASSWORD)
                                      .credentials(
                                          HttpHelmUsernamePasswordDTO.builder()
                                              .username(username)
                                              .passwordRef(SecretRefData.builder().decryptedValue(password).build())
                                              .build())
                                      .build())
                            .build())
                    .build())
            .build();

    doNothing()
        .when(spyTaskHelperBase)
        .addRepo(REPO_NAME, REPO_DISPLAY_NAME, repoUrl, username, password, chartOutput, V3, timeout);
    doNothing()
        .when(spyTaskHelperBase)
        .fetchChartFromRepo(
            REPO_NAME, REPO_DISPLAY_NAME, CHART_NAME, CHART_VERSION, chartOutput, V3, emptyHelmCommandFlag, timeout);

    spyTaskHelperBase.downloadChartFilesFromHttpRepo(helmChartManifestDelegateConfig, chartOutput, timeout);

    verify(spyTaskHelperBase, times(1))
        .addRepo(REPO_NAME, REPO_DISPLAY_NAME, repoUrl, username, password, chartOutput, V3, timeout);
    verify(spyTaskHelperBase, times(1))
        .fetchChartFromRepo(
            REPO_NAME, REPO_DISPLAY_NAME, CHART_NAME, CHART_VERSION, chartOutput, V3, emptyHelmCommandFlag, timeout);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testDownloadChartFilesFromHttpRepoAnonymousAuth() {
    HelmTaskHelperBase spyTaskHelperBase = spy(helmTaskHelperBase);
    String repoUrl = "http://repo";
    String chartOutput = "/dir";
    long timeout = 90000L;

    HelmChartManifestDelegateConfig helmChartManifestDelegateConfig =
        HelmChartManifestDelegateConfig.builder()
            .chartName(CHART_NAME)
            .chartVersion(CHART_VERSION)
            .helmVersion(V3)
            .helmCommandFlag(emptyHelmCommandFlag)
            .storeDelegateConfig(
                HttpHelmStoreDelegateConfig.builder()
                    .repoName(REPO_NAME)
                    .repoDisplayName(REPO_DISPLAY_NAME)
                    .httpHelmConnector(
                        HttpHelmConnectorDTO.builder()
                            .helmRepoUrl(repoUrl)
                            .auth(HttpHelmAuthenticationDTO.builder().authType(HttpHelmAuthType.ANONYMOUS).build())
                            .build())
                    .build())
            .build();

    doNothing()
        .when(spyTaskHelperBase)
        .addRepo(REPO_NAME, REPO_DISPLAY_NAME, repoUrl, null, null, chartOutput, V3, timeout);
    doNothing()
        .when(spyTaskHelperBase)
        .fetchChartFromRepo(
            REPO_NAME, REPO_DISPLAY_NAME, CHART_NAME, CHART_VERSION, chartOutput, V3, emptyHelmCommandFlag, timeout);

    spyTaskHelperBase.downloadChartFilesFromHttpRepo(helmChartManifestDelegateConfig, chartOutput, timeout);

    verify(spyTaskHelperBase, times(1))
        .addRepo(REPO_NAME, REPO_DISPLAY_NAME, repoUrl, null, null, chartOutput, V3, timeout);
    verify(spyTaskHelperBase, times(1))
        .fetchChartFromRepo(
            REPO_NAME, REPO_DISPLAY_NAME, CHART_NAME, CHART_VERSION, chartOutput, V3, emptyHelmCommandFlag, timeout);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetChartDirectory() {
    String chartName = "chart_name";
    String chartNameWithRepo = "public_repo/chart_name";
    String parentDirectory = "/manifests";

    assertThat(HelmTaskHelperBase.getChartDirectory(parentDirectory, chartName)).isEqualTo("/manifests/chart_name");
    assertThat(HelmTaskHelperBase.getChartDirectory(parentDirectory, chartNameWithRepo))
        .isEqualTo("/manifests/chart_name");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testAddChartMuseumRepo() {
    HelmTaskHelperBase spyTaskHelperBase = spy(helmTaskHelperBase);
    final String chartDirectory = "chart_directory";
    final int port = 1234;
    final long timeoutInMillis = 9000L;

    doReturn(new ProcessResult(0, new ProcessOutput(null)))
        .when(spyTaskHelperBase)
        .executeCommand(anyString(), eq(chartDirectory), anyString(), eq(timeoutInMillis));

    spyTaskHelperBase.addChartMuseumRepo(REPO_NAME, REPO_DISPLAY_NAME, port, chartDirectory, V3, timeoutInMillis);
    ArgumentCaptor<String> commandCaptor = ArgumentCaptor.forClass(String.class);
    verify(spyTaskHelperBase, times(1))
        .executeCommand(commandCaptor.capture(), eq(chartDirectory), anyString(), eq(timeoutInMillis));
    String executedCommand = commandCaptor.getValue();
    assertThat(executedCommand).contains(CHART_MUSEUM_SERVER_URL.replace("${PORT}", Integer.toString(port)));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testDownloadChartFilesUsingChartMuseumS3() throws Exception {
    final HelmTaskHelperBase spyHelmTaskHelperBase = spy(helmTaskHelperBase);
    final String destinationDirectory = "destinationDirectory";
    final String resourceDirectory = "resourceDirectory";
    final long timeoutInMillis = 90000L;
    final int port = 33344;
    final ChartMuseumServer chartMuseumServer = ChartMuseumServer.builder().port(port).build();
    final S3HelmStoreDelegateConfig s3StoreDelegateConfig =
        S3HelmStoreDelegateConfig.builder().repoName(REPO_NAME).repoDisplayName(REPO_DISPLAY_NAME).build();
    final HelmChartManifestDelegateConfig manifest = HelmChartManifestDelegateConfig.builder()
                                                         .chartName(CHART_NAME)
                                                         .chartVersion(CHART_VERSION)
                                                         .storeDelegateConfig(s3StoreDelegateConfig)
                                                         .helmVersion(V3)
                                                         .build();

    doReturn(resourceDirectory).when(spyHelmTaskHelperBase).createNewDirectoryAtPath(RESOURCE_DIR_BASE);
    doReturn(chartMuseumServer)
        .when(ngChartMuseumService)
        .startChartMuseumServer(s3StoreDelegateConfig, resourceDirectory);
    doNothing()
        .when(spyHelmTaskHelperBase)
        .addChartMuseumRepo(REPO_NAME, REPO_DISPLAY_NAME, port, destinationDirectory, V3, timeoutInMillis);
    doNothing()
        .when(spyHelmTaskHelperBase)
        .fetchChartFromRepo(
            REPO_NAME, REPO_DISPLAY_NAME, CHART_NAME, CHART_VERSION, destinationDirectory, V3, null, timeoutInMillis);

    spyHelmTaskHelperBase.downloadChartFilesUsingChartMuseum(manifest, destinationDirectory, timeoutInMillis);

    verify(ngChartMuseumService, times(1)).startChartMuseumServer(s3StoreDelegateConfig, resourceDirectory);
    verify(ngChartMuseumService, times(1)).stopChartMuseumServer(chartMuseumServer);
    verify(spyHelmTaskHelperBase, times(1))
        .addChartMuseumRepo(REPO_NAME, REPO_DISPLAY_NAME, port, destinationDirectory, V3, timeoutInMillis);
    verify(spyHelmTaskHelperBase, times(1))
        .fetchChartFromRepo(
            REPO_NAME, REPO_DISPLAY_NAME, CHART_NAME, CHART_VERSION, destinationDirectory, V3, null, timeoutInMillis);
  }
}