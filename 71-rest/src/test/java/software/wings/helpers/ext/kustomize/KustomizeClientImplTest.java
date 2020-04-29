package software.wings.helpers.ext.kustomize;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.doReturn;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.cli.CliHelper;
import software.wings.helpers.ext.cli.CliResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

public class KustomizeClientImplTest extends WingsBaseTest {
  @Mock private CliHelper cliHelper;
  @Inject @InjectMocks private KustomizeClientImpl kustomizeClientImpl;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testBuild() throws InterruptedException, IOException, TimeoutException {
    // tests correct parameters are passed to executeCliCommand
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback();
    CliResponse cliResponse = CliResponse.builder().build();
    doReturn(cliResponse)
        .when(cliHelper)
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
        .when(cliHelper)
        .executeCliCommand(
            "XDG_CONFIG_HOME=PLUGIN_PATH KUSTOMIZE_BINARY_PATH build --enable_alpha_plugins KUSTOMIZE_DIR_PATH",
            KustomizeConstants.KUSTOMIZE_COMMAND_TIMEOUT, Collections.emptyMap(), "MANIFEST_FILES_DIRECTORY",
            executionLogCallback);

    CliResponse actualResponse = kustomizeClientImpl.buildWithPlugins(
        "MANIFEST_FILES_DIRECTORY", "KUSTOMIZE_DIR_PATH", "KUSTOMIZE_BINARY_PATH", "PLUGIN_PATH", executionLogCallback);

    assertThat(actualResponse).isEqualTo(cliResponse);
  }
}