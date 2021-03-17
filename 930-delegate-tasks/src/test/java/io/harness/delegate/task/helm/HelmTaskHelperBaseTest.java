package io.harness.delegate.task.helm;

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
import io.harness.delegate.beans.connector.helm.HttpHelmAuthType;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthenticationDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmUsernamePasswordDTO;
import io.harness.delegate.beans.storeconfig.HttpHelmStoreDelegateConfig;
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
  private static String CHART_NAME = "test-helm-chart";
  private static String CHART_VERSION = "1.0.0";
  private static String REPO_NAME = "helm_charts";
  private static String REPO_DISPLAY_NAME = "Helm Charts";

  @Mock K8sGlobalConfigService k8sGlobalConfigService;

  @InjectMocks HelmTaskHelperBase helmTaskHelperBase;

  final HelmCommandFlag emptyHelmCommandFlag = HelmCommandFlag.builder().build();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    doReturn("v2/helm").when(k8sGlobalConfigService).getHelmPath(HelmVersion.V2);
    doReturn("v3/helm").when(k8sGlobalConfigService).getHelmPath(HelmVersion.V3);
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

    String fetchCommand = helmTaskHelperBase.getHelmFetchCommand(
        CHART_NAME, CHART_VERSION, REPO_NAME, "/pwd", HelmVersion.V3, emptyHelmCommandFlag);
    assertThat(fetchCommand).isEqualTo(expectedCommand);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetHelmFetchCommandEmptyRepoName() {
    String chartName = "chart-empty-repo";
    String chartVersion = "1.0.0";
    String expectedCommand = String.format("v3/helm pull %s  --untar --version %s", chartName, chartVersion);

    String fetchCommand = helmTaskHelperBase.getHelmFetchCommand(
        chartName, chartVersion, "", "/pwd", HelmVersion.V3, emptyHelmCommandFlag);
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
                           "password".toCharArray(), "/dir", HelmVersion.V3, 90000))
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
                       -> spyTaskHelperBase.fetchChartFromRepo("repo", "repo display", "chart", "1.0.0", "/dir",
                           HelmVersion.V3, emptyHelmCommandFlag, 90000))
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
                               CHART_VERSION, "/dir", HelmVersion.V3, emptyHelmCommandFlag, 90000))
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
            .helmVersion(HelmVersion.V3)
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
        .addRepo(REPO_NAME, REPO_DISPLAY_NAME, repoUrl, username, password, chartOutput, HelmVersion.V3, timeout);
    doNothing()
        .when(spyTaskHelperBase)
        .fetchChartFromRepo(REPO_NAME, REPO_DISPLAY_NAME, CHART_NAME, CHART_VERSION, chartOutput, HelmVersion.V3,
            emptyHelmCommandFlag, timeout);

    spyTaskHelperBase.downloadChartFilesFromHttpRepo(helmChartManifestDelegateConfig, chartOutput, timeout);

    verify(spyTaskHelperBase, times(1))
        .addRepo(REPO_NAME, REPO_DISPLAY_NAME, repoUrl, username, password, chartOutput, HelmVersion.V3, timeout);
    verify(spyTaskHelperBase, times(1))
        .fetchChartFromRepo(REPO_NAME, REPO_DISPLAY_NAME, CHART_NAME, CHART_VERSION, chartOutput, HelmVersion.V3,
            emptyHelmCommandFlag, timeout);
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
            .helmVersion(HelmVersion.V3)
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
        .addRepo(REPO_NAME, REPO_DISPLAY_NAME, repoUrl, null, null, chartOutput, HelmVersion.V3, timeout);
    doNothing()
        .when(spyTaskHelperBase)
        .fetchChartFromRepo(REPO_NAME, REPO_DISPLAY_NAME, CHART_NAME, CHART_VERSION, chartOutput, HelmVersion.V3,
            emptyHelmCommandFlag, timeout);

    spyTaskHelperBase.downloadChartFilesFromHttpRepo(helmChartManifestDelegateConfig, chartOutput, timeout);

    verify(spyTaskHelperBase, times(1))
        .addRepo(REPO_NAME, REPO_DISPLAY_NAME, repoUrl, null, null, chartOutput, HelmVersion.V3, timeout);
    verify(spyTaskHelperBase, times(1))
        .fetchChartFromRepo(REPO_NAME, REPO_DISPLAY_NAME, CHART_NAME, CHART_VERSION, chartOutput, HelmVersion.V3,
            emptyHelmCommandFlag, timeout);
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
}