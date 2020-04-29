package software.wings.helpers.ext.cli;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.command.ExecutionLogCallback;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CliHelperTest extends WingsBaseTest {
  @Inject private CliHelper cliHelper;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testExecuteCliCommand() throws InterruptedException, TimeoutException, IOException {
    CliResponse cliResponse = cliHelper.executeCliCommand(
        "echo 1", TimeUnit.MINUTES.toMillis(1), Collections.emptyMap(), ".", new ExecutionLogCallback());
    assertThat(cliResponse.getOutput()).isEqualTo("1\n");
    assertThat(cliResponse.getCommandExecutionStatus()).isEqualTo(SUCCESS);

    cliResponse = cliHelper.executeCliCommand(
        "echo1 $abc", TimeUnit.MINUTES.toMillis(1), Collections.emptyMap(), ".", new ExecutionLogCallback());
    assertThat(cliResponse.getCommandExecutionStatus()).isEqualTo(FAILURE);

    assertThatThrownBy(()
                           -> cliHelper.executeCliCommand("sleep 4", TimeUnit.MILLISECONDS.toMillis(1),
                               Collections.emptyMap(), ".", new ExecutionLogCallback()))
        .isInstanceOf(TimeoutException.class);
  }
}