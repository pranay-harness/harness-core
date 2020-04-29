package software.wings.helpers.ext.openshift;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.helpers.ext.openshift.OpenShiftConstants.COMMAND_TIMEOUT;
import static software.wings.helpers.ext.openshift.OpenShiftConstants.OC_BINARY_PATH;
import static software.wings.helpers.ext.openshift.OpenShiftConstants.PROCESS_COMMAND;
import static software.wings.helpers.ext.openshift.OpenShiftConstants.TEMPLATE_FILE_PATH;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.cli.CliHelper;
import software.wings.helpers.ext.cli.CliResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;

public class OpenShiftClientImpl implements OpenShiftClient {
  @Inject CliHelper cliHelper;

  @Override
  @Nonnull
  public CliResponse process(@NotEmpty String ocBinaryPath, @NotEmpty String templateFilePath,
      List<String> paramsFilePaths, @NotEmpty String manifestFilesDirectoryPath,
      ExecutionLogCallback executionLogCallback) {
    StringBuilder processCommand = new StringBuilder(
        PROCESS_COMMAND.replace(OC_BINARY_PATH, ocBinaryPath).replace(TEMPLATE_FILE_PATH, templateFilePath));

    if (isNotEmpty(paramsFilePaths)) {
      for (String paramsFilePath : paramsFilePaths) {
        processCommand.append(" --param-file ").append(paramsFilePath);
      }
    }
    try {
      return cliHelper.executeCliCommand(processCommand.toString(), COMMAND_TIMEOUT, Collections.emptyMap(),
          manifestFilesDirectoryPath, executionLogCallback);
    } catch (IOException e) {
      throw new InvalidRequestException(
          "IO Failure occurred while running oc process. " + e.getMessage(), e, WingsException.USER);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InvalidRequestException(
          "Thread interrupted while running oc process. Try again.", e, WingsException.USER);
    } catch (TimeoutException e) {
      throw new InvalidRequestException("Timed out while running oc process.", e, WingsException.USER);
    }
  }
}
