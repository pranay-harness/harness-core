package software.wings.delegatetasks.helm;

import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion.V2;
import static software.wings.helpers.ext.helm.HelmConstants.HelmVersion.V3;

import io.harness.category.element.UnitTests;
import io.harness.exception.HelmClientException;
import io.harness.rule.Owner;
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
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.TimeoutException;

public class HelmTaskHelperTest extends WingsBaseTest {
  @Mock private ProcessExecutor processExecutor;
  @Mock K8sGlobalConfigService k8sGlobalConfigService;
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
}