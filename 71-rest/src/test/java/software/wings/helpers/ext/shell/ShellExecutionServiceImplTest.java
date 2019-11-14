package software.wings.helpers.ext.shell;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import io.harness.shell.ShellExecutionRequest;
import io.harness.shell.ShellExecutionResponse;
import io.harness.shell.ShellExecutionServiceImpl;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;

public class ShellExecutionServiceImplTest extends WingsBaseTest {
  @Inject @InjectMocks private ShellExecutionServiceImpl shellExecutionService;

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testExecuteShellScript() {
    ShellExecutionRequest shellExecutionRequest = ShellExecutionRequest.builder()
                                                      .scriptString("echo \"foo\" > $ARTIFACT_RESULT_PATH")
                                                      .workingDirectory("/tmp")
                                                      .build();
    ShellExecutionResponse shellExecutionResponse = shellExecutionService.execute(shellExecutionRequest);
    assertThat(shellExecutionResponse).isNotNull();
    assertThat(shellExecutionResponse.getExitValue()).isEqualTo(0);
    assertThat(shellExecutionResponse.getShellExecutionData()).isNotEmpty();
    assertThat(shellExecutionResponse.getShellExecutionData().get("ARTIFACT_RESULT_PATH")).isNotNull();
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testExecuteScriptTimeout() {
    ShellExecutionRequest shellExecutionRequest =
        ShellExecutionRequest.builder().scriptString("sleep 10").workingDirectory("/tmp").timeoutSeconds(1).build();
    ShellExecutionResponse shellExecutionResponse = shellExecutionService.execute(shellExecutionRequest);
    assertThat(shellExecutionResponse).isNotNull();
    assertThat(shellExecutionResponse.getExitValue()).isNotEqualTo(0);
    assertThat(shellExecutionResponse.getShellExecutionData()).isNull();
  }
}
