package software.wings.helpers.ext.kustomize;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.command.ExecutionLogCallback;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class KustomizeTaskHelperTest extends WingsBaseTest {
  @Mock KustomizeClient kustomizeClient;

  @InjectMocks @Inject KustomizeTaskHelper kustomizeTaskHelper;
  KustomizeTaskHelper spyKustomizeTaskHelper = spy(new KustomizeTaskHelper());

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testBuild() throws InterruptedException, TimeoutException, IOException {
    shouldCallClientBuild();
    shouldCallClientBuildWithPlugins();
    shouldHandleTimeoutException();
    shouldHandleIOException();
    shouldHandleInterrupedException();
    shouldHandleClientBuildFailure();
  }

  private void shouldHandleClientBuildFailure() throws InterruptedException, IOException, TimeoutException {
    final String RANDOM = "RANDOM";
    KustomizeConfig kustomizeConfig = KustomizeConfig.builder().kustomizeDirPath(RANDOM).build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();
    CliResponse cliResponse =
        CliResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).output(RANDOM).build();
    doReturn(cliResponse).when(kustomizeClient).build(RANDOM, RANDOM, RANDOM, executionLogCallback);

    assertThatThrownBy(() -> kustomizeTaskHelper.build(RANDOM, RANDOM, kustomizeConfig, executionLogCallback))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining(RANDOM);
  }

  private void shouldHandleInterrupedException() throws InterruptedException, IOException, TimeoutException {
    final String RANDOM = "RANDOM";
    KustomizeConfig kustomizeConfig = KustomizeConfig.builder().kustomizeDirPath(RANDOM).build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();
    doThrow(InterruptedException.class).when(kustomizeClient).build(RANDOM, RANDOM, RANDOM, executionLogCallback);

    assertThatThrownBy(() -> kustomizeTaskHelper.build(RANDOM, RANDOM, kustomizeConfig, executionLogCallback))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Kustomize build interrupted");
  }

  private void shouldHandleIOException() throws InterruptedException, IOException, TimeoutException {
    final String RANDOM = "RANDOM";
    KustomizeConfig kustomizeConfig = KustomizeConfig.builder().kustomizeDirPath(RANDOM).build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();
    doThrow(IOException.class).when(kustomizeClient).build(RANDOM, RANDOM, RANDOM, executionLogCallback);

    assertThatThrownBy(() -> kustomizeTaskHelper.build(RANDOM, RANDOM, kustomizeConfig, executionLogCallback))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("IO Failure occurred while running kustomize build");
  }

  private void shouldHandleTimeoutException() throws InterruptedException, IOException, TimeoutException {
    final String RANDOM = "RANDOM";
    KustomizeConfig kustomizeConfig = KustomizeConfig.builder().kustomizeDirPath(RANDOM).build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();
    doThrow(TimeoutException.class).when(kustomizeClient).build(RANDOM, RANDOM, RANDOM, executionLogCallback);

    assertThatThrownBy(() -> kustomizeTaskHelper.build(RANDOM, RANDOM, kustomizeConfig, executionLogCallback))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Kustomize build timed out");
  }

  private void shouldCallClientBuildWithPlugins() throws InterruptedException, IOException, TimeoutException {
    final String RANDOM = "RANDOM";
    KustomizeConfig kustomizeConfig = KustomizeConfig.builder().kustomizeDirPath(RANDOM).pluginRootDir(RANDOM).build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();
    CliResponse cliResponse =
        CliResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).output(RANDOM).build();
    doReturn(cliResponse).when(kustomizeClient).buildWithPlugins(RANDOM, RANDOM, RANDOM, RANDOM, executionLogCallback);

    List<ManifestFile> manifestFiles = kustomizeTaskHelper.build(RANDOM, RANDOM, kustomizeConfig, executionLogCallback);
    assertThat(manifestFiles).hasSize(1);
    assertThat(manifestFiles.get(0).getFileContent()).isEqualTo(RANDOM);
  }

  private void shouldCallClientBuild() throws InterruptedException, IOException, TimeoutException {
    final String RANDOM = "RANDOM";
    KustomizeConfig kustomizeConfig = KustomizeConfig.builder().kustomizeDirPath(RANDOM).build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();
    CliResponse cliResponse =
        CliResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).output(RANDOM).build();
    doReturn(cliResponse).when(kustomizeClient).build(RANDOM, RANDOM, RANDOM, executionLogCallback);

    List<ManifestFile> manifestFiles = kustomizeTaskHelper.build(RANDOM, RANDOM, kustomizeConfig, executionLogCallback);
    assertThat(manifestFiles).hasSize(1);
    assertThat(manifestFiles.get(0).getFileContent()).isEqualTo(RANDOM);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testKustomizeBuildForApply() {
    applyFilesSizeShouldBeOne();
    shouldCallKustomizeBuild();
  }

  private void shouldCallKustomizeBuild() {
    String RANDOM = "RANDOM";
    KustomizeConfig kustomizeConfig = KustomizeConfig.builder().pluginRootDir(RANDOM).build();
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();
    List<String> file = Collections.singletonList("file");
    ManifestFile manifestFile = ManifestFile.builder().build();
    List<ManifestFile> manifestFiles = Collections.singletonList(manifestFile);
    ArgumentCaptor<KustomizeConfig> captor = ArgumentCaptor.forClass(KustomizeConfig.class);
    doReturn(manifestFiles)
        .when(spyKustomizeTaskHelper)
        .build(eq(RANDOM), eq(RANDOM), captor.capture(), eq(executionLogCallback));

    List<ManifestFile> actualManifestFiles =
        spyKustomizeTaskHelper.buildForApply(RANDOM, kustomizeConfig, RANDOM, file, executionLogCallback);

    assertThat(actualManifestFiles).isEqualTo(manifestFiles);
    KustomizeConfig configPassed = captor.getValue();
    assertThat(configPassed.getKustomizeDirPath()).isEqualTo("file");
    assertThat(configPassed.getPluginRootDir()).isEqualTo(RANDOM);
  }

  private void applyFilesSizeShouldBeOne() {
    assertThatThrownBy(() -> kustomizeTaskHelper.buildForApply(null, null, null, null, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Apply files can't be empty");

    assertThatThrownBy(() -> kustomizeTaskHelper.buildForApply(null, null, null, Collections.emptyList(), null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Apply files can't be empty");

    List<String> applyFiles = Arrays.asList("file1", "file2");

    assertThatThrownBy(() -> kustomizeTaskHelper.buildForApply(null, null, null, applyFiles, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Apply with Kustomize is supported for single file only");
  }
}