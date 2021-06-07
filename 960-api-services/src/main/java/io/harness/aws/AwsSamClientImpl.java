package io.harness.aws;

import com.google.inject.Inject;
import io.harness.cli.CliHelper;
import io.harness.cli.CliResponse;
import io.harness.cli.LogCallbackOutputStream;
import io.harness.exception.TerraformCommandExecutionException;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;

public class AwsSamClientImpl implements AwsSamClient {
  @Inject CliHelper cliHelper;

  @Nonnull
  @Override
  public CliResponse runCommand(String command, long timeoutInMillis, Map<String, String> envVariables,
      String workingDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    CliResponse response = cliHelper.executeCliCommand(
        command, timeoutInMillis, envVariables, workingDirectory, executionLogCallback, command, new LogCallbackOutputStream(executionLogCallback));
    if (response != null && response.getCommandExecutionStatus() == CommandExecutionStatus.FAILURE) {
      throw new TerraformCommandExecutionException(
          format("Failed to execute terraform Command %s : Reason: %s", command, response.getError()),
          WingsException.SRE);
    }
    return response;
  }
}
