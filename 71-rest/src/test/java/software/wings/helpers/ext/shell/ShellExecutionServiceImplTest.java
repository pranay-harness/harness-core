package software.wings.helpers.ext.shell;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.helpers.ext.shell.request.ShellExecutionRequest;
import software.wings.helpers.ext.shell.response.ShellExecutionResponse;
import software.wings.helpers.ext.shell.response.ShellExecutionServiceImpl;

public class ShellExecutionServiceImplTest extends WingsBaseTest {
  @Inject @InjectMocks private ShellExecutionServiceImpl shellExecutionService;

  @Test
  public void testExecuteShellScript() {
    ShellExecutionRequest shellExecutionRequest =
        ShellExecutionRequest.builder()
            .scriptString("curl https://harness.jfrog.io/harness/api/repositories > $ARTIFACT_RESULT_PATH")
            .workingDirectory("/tmp")
            .build();
    ShellExecutionResponse shellExecutionResponse = shellExecutionService.execute(shellExecutionRequest);
    assertThat(shellExecutionResponse).isNotNull();
    assertThat(shellExecutionResponse.getExitValue()).isEqualTo(0);
    assertThat(shellExecutionResponse.getShellExecutionData()).isNotEmpty();
    assertThat(shellExecutionResponse.getShellExecutionData().get("ARTIFACT_RESULT_PATH")).isNotNull();
  }

  @Test
  public void testExecuteScriptTimeout() {
    ShellExecutionRequest shellExecutionRequest =
        ShellExecutionRequest.builder().scriptString("sleep 10").workingDirectory("/tmp").timeoutSeconds(1).build();
    ShellExecutionResponse shellExecutionResponse = shellExecutionService.execute(shellExecutionRequest);
    assertThat(shellExecutionResponse).isNotNull();
    assertThat(shellExecutionResponse.getExitValue()).isNotEqualTo(0);
    assertThat(shellExecutionResponse.getShellExecutionData()).isNull();
  }
}
