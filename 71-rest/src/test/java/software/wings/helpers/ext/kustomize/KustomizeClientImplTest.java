package software.wings.helpers.ext.kustomize;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.command.ExecutionLogCallback;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class KustomizeClientImplTest {
  private KustomizeClientImpl kustomizeClientImpl = spy(new KustomizeClientImpl());

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testExecuteCliCommand() throws InterruptedException, TimeoutException, IOException {
    CliResponse cliResponse = kustomizeClientImpl.executeCliCommand(
        "echo 1", TimeUnit.MINUTES.toMillis(1), Collections.emptyMap(), ".", new ExecutionLogCallback());
    assertThat(cliResponse.getOutput()).isEqualTo("1\n");
    assertThat(cliResponse.getCommandExecutionStatus()).isEqualTo(SUCCESS);

    cliResponse = kustomizeClientImpl.executeCliCommand(
        "echo1 $abc", TimeUnit.MINUTES.toMillis(1), Collections.emptyMap(), ".", new ExecutionLogCallback());
    assertThat(cliResponse.getCommandExecutionStatus()).isEqualTo(FAILURE);

    assertThatThrownBy(()
                           -> kustomizeClientImpl.executeCliCommand("sleep 4", TimeUnit.MILLISECONDS.toMillis(1),
                               Collections.emptyMap(), ".", new ExecutionLogCallback()))
        .isInstanceOf(TimeoutException.class);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testBuild() throws InterruptedException, IOException, TimeoutException {
    // tests correct parameters are passed to executeCliCommand
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();
    CliResponse cliResponse = CliResponse.builder().build();
    doReturn(cliResponse)
        .when(kustomizeClientImpl)
        .executeCliCommand("KUSTOMIZE_BINARY_PATH build KUSTOMIZE_DIR_PATH",
            KustomizeConstants.KUSTOMIZE_COMMAND_TIMEOUT, Collections.emptyMap(), "MANIFEST_FILES_DIRECTORY",
            executionLogCallback);

    CliResponse actualResponse = kustomizeClientImpl.build(
        "MANIFEST_FILES_DIRECTORY", "KUSTOMIZE_DIR_PATH", "KUSTOMIZE_BINARY_PATH", executionLogCallback);

    assertThat(actualResponse).isEqualTo(cliResponse);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testBuildWithPlugins() throws InterruptedException, IOException, TimeoutException {
    // tests correct parameters are passed to executeCliCommand
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();
    CliResponse cliResponse = CliResponse.builder().build();
    doReturn(cliResponse)
        .when(kustomizeClientImpl)
        .executeCliCommand(
            "XDG_CONFIG_HOME=PLUGIN_PATH KUSTOMIZE_BINARY_PATH build --enable_alpha_plugins KUSTOMIZE_DIR_PATH",
            KustomizeConstants.KUSTOMIZE_COMMAND_TIMEOUT, Collections.emptyMap(), "MANIFEST_FILES_DIRECTORY",
            executionLogCallback);

    CliResponse actualResponse = kustomizeClientImpl.buildWithPlugins(
        "MANIFEST_FILES_DIRECTORY", "KUSTOMIZE_DIR_PATH", "KUSTOMIZE_BINARY_PATH", "PLUGIN_PATH", executionLogCallback);

    assertThat(actualResponse).isEqualTo(cliResponse);
  }
}