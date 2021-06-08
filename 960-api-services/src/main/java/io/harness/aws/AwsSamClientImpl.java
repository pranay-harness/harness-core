package io.harness.aws;

import static java.lang.String.format;

import io.harness.cli.CliHelper;
import io.harness.cli.CliResponse;
import io.harness.cli.LogCallbackOutputStream;
import io.harness.exception.AwsSamCommandExecutionException;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;

public class AwsSamClientImpl implements AwsSamClient {
  @Inject CliHelper cliHelper;

  @Nonnull
  @Override
  public CliResponse runCommand(String command, long timeoutInMillis, Map<String, String> envVariables,
      String workingDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    String loggingCommand = command.indexOf("&&") != -1 ? command.substring(command.indexOf("&&")+2) : command;
    CliResponse response = cliHelper.executeCliCommand(command, timeoutInMillis, envVariables, workingDirectory,
        executionLogCallback, loggingCommand, new LogCallbackOutputStream(executionLogCallback));
    if (response != null && response.getCommandExecutionStatus() == CommandExecutionStatus.FAILURE) {
      throw new AwsSamCommandExecutionException(
          format("Failed to execute aws sam  Command %s : Reason: %s", command, response.getError()),
          WingsException.SRE);
    }
    return response;
  }
}
