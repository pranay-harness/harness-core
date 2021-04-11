package io.harness.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.terraform.TerraformConstants.DEFAULT_TERRAFORM_COMMAND_TIMEOUT;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cli.CliHelper;
import io.harness.cli.CliResponse;
import io.harness.cli.LogCallbackOutputStream;
import io.harness.exception.TerraformCommandExecutionException;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.PlanJsonLogOutputStream;
import io.harness.terraform.request.TerraformApplyCommandRequest;
import io.harness.terraform.request.TerraformDestroyCommandRequest;
import io.harness.terraform.request.TerraformInitCommandRequest;
import io.harness.terraform.request.TerraformPlanCommandRequest;
import io.harness.terraform.request.TerraformRefreshCommandRequest;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.stream.LogOutputStream;

@Slf4j
@Singleton
@OwnedBy(CDP)
public class TerraformClientImpl implements TerraformClient {
  public static final String TARGET_PARAM = "-target=";
  public static final String VAR_FILE_PARAM = "-var-file=";

  @Inject CliHelper cliHelper;

  @Nonnull
  @Override
  public CliResponse init(TerraformInitCommandRequest terraformInitCommandRequest, Map<String, String> envVariables,
      String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    String command = format("terraform init -input=false %s",
        isEmpty(terraformInitCommandRequest.getTfBackendConfigsFilePath())
            ? EMPTY
            : format("-backend-config=%s", terraformInitCommandRequest.getTfBackendConfigsFilePath()));

    /**
     * echo "no" is to prevent copying of state from local to remote by suppressing the
     * copy prompt. As of tf version 0.12.3
     * there is no way to provide this as a command line argument
     */
    String executionCommand = format("echo \"no\" | %s", command);
    return executeTerraformCLICommand(executionCommand, envVariables, scriptDirectory, executionLogCallback, command,
        new LogCallbackOutputStream(executionLogCallback));
  }

  @Nonnull
  @Override
  public CliResponse destroy(TerraformDestroyCommandRequest terraformDestroyCommandRequest,
      Map<String, String> envVariables, String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    String command = format("terraform destroy -force %s %s",
        TerraformHelperUtils.generateCommandFlagsString(terraformDestroyCommandRequest.getTargets(), TARGET_PARAM),
        TerraformHelperUtils.generateCommandFlagsString(
            terraformDestroyCommandRequest.getVarFilePaths(), VAR_FILE_PARAM));
    return executeTerraformCLICommand(command, envVariables, scriptDirectory, executionLogCallback, command,
        new LogCallbackOutputStream(executionLogCallback));
  }

  @Nonnull
  @Override
  public CliResponse plan(TerraformPlanCommandRequest terraformPlanCommandRequest, Map<String, String> envVariables,
      String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    String command;
    if (terraformPlanCommandRequest.isDestroySet()) {
      command = format("terraform plan -input=false -destroy -out=tfdestroyplan %s %s",
          TerraformHelperUtils.generateCommandFlagsString(terraformPlanCommandRequest.getTargets(), TARGET_PARAM),
          TerraformHelperUtils.generateCommandFlagsString(
              terraformPlanCommandRequest.getVarFilePaths(), VAR_FILE_PARAM));
    } else {
      command = format("terraform plan -input=false -out=tfplan %s %s",
          TerraformHelperUtils.generateCommandFlagsString(terraformPlanCommandRequest.getTargets(), TARGET_PARAM),
          TerraformHelperUtils.generateCommandFlagsString(
              terraformPlanCommandRequest.getVarFilePaths(), VAR_FILE_PARAM));
    }

    if (isNotEmpty(terraformPlanCommandRequest.getVarParams())) {
      String loggingCommand = command + terraformPlanCommandRequest.getUiLogs();
      command = command + terraformPlanCommandRequest.getVarParams();

      return executeTerraformCLICommand(command, envVariables, scriptDirectory, executionLogCallback, loggingCommand,
          new LogCallbackOutputStream(executionLogCallback));
    }

    return executeTerraformCLICommand(command, envVariables, scriptDirectory, executionLogCallback, command,
        new LogCallbackOutputStream(executionLogCallback));
  }

  @Nonnull
  @Override
  public CliResponse refresh(TerraformRefreshCommandRequest terraformRefreshCommandRequest,
      Map<String, String> envVariables, String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    String command;
    command = "terraform refresh -input=false "
        + TerraformHelperUtils.generateCommandFlagsString(terraformRefreshCommandRequest.getTargets(), TARGET_PARAM)
        + TerraformHelperUtils.generateCommandFlagsString(
            terraformRefreshCommandRequest.getVarFilePaths(), VAR_FILE_PARAM);

    if (isNotEmpty(terraformRefreshCommandRequest.getVarParams())) {
      String loggingCommand = command + terraformRefreshCommandRequest.getUiLogs();
      command = command + terraformRefreshCommandRequest.getVarParams();
      return executeTerraformCLICommand(command, envVariables, scriptDirectory, executionLogCallback, loggingCommand,
          new LogCallbackOutputStream(executionLogCallback));
    }
    return executeTerraformCLICommand(command, envVariables, scriptDirectory, executionLogCallback, command,
        new LogCallbackOutputStream(executionLogCallback));
  }

  @Nonnull
  @Override
  public CliResponse apply(TerraformApplyCommandRequest terraformApplyCommandRequest, Map<String, String> envVariables,
      String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    String command = "terraform apply -input=false " + terraformApplyCommandRequest.getPlanName();
    return executeTerraformCLICommand(command, envVariables, scriptDirectory, executionLogCallback, command,
        new LogCallbackOutputStream(executionLogCallback));
  }

  @Nonnull
  @Override
  public CliResponse workspace(String workspace, boolean isExistingWorkspace, Map<String, String> envVariables,
      String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    String command =
        isExistingWorkspace ? "terraform workspace select " + workspace : "terraform workspace new " + workspace;
    return executeTerraformCLICommand(command, envVariables, scriptDirectory, executionLogCallback, command,
        new LogCallbackOutputStream(executionLogCallback));
  }

  @Nonnull
  @Override
  public CliResponse getWorkspaceList(Map<String, String> envVariables, String scriptDirectory,
      @Nonnull LogCallback executionLogCallback) throws InterruptedException, TimeoutException, IOException {
    String command = "terraform workspace list";
    return executeTerraformCLICommand(command, envVariables, scriptDirectory, executionLogCallback, command,
        new LogCallbackOutputStream(executionLogCallback));
  }

  @Nonnull
  @Override
  public CliResponse show(String planName, Map<String, String> envVariables, String scriptDirectory,
      @Nonnull LogCallback executionLogCallback, @Nonnull PlanJsonLogOutputStream planJsonLogOutputStream)
      throws InterruptedException, TimeoutException, IOException {
    String command = "terraform show -json " + planName;
    return executeTerraformCLICommand(
        command, envVariables, scriptDirectory, executionLogCallback, command, planJsonLogOutputStream);
  }

  @Nonnull
  @Override
  public CliResponse output(String tfOutputsFile, Map<String, String> envVariables, String scriptDirectory,
      @Nonnull LogCallback executionLogCallback) throws InterruptedException, TimeoutException, IOException {
    String command = "terraform output -json > " + tfOutputsFile;
    return executeTerraformCLICommand(command, envVariables, scriptDirectory, executionLogCallback, command,
        new LogCallbackOutputStream(executionLogCallback));
  }

  @VisibleForTesting
  CliResponse executeTerraformCLICommand(String command, Map<String, String> envVariables, String scriptDirectory,
      LogCallback executionLogCallBack, String loggingCommand, LogOutputStream logOutputStream)
      throws IOException, InterruptedException, TimeoutException, TerraformCommandExecutionException {
    CliResponse response = cliHelper.executeCliCommand(command, DEFAULT_TERRAFORM_COMMAND_TIMEOUT, envVariables,
        scriptDirectory, executionLogCallBack, loggingCommand, logOutputStream);
    if (response != null && response.getCommandExecutionStatus() == CommandExecutionStatus.FAILURE) {
      throw new TerraformCommandExecutionException(
          format("Failed to execute terraform Command %s : Reason: %s", command, response.getError()),
          WingsException.SRE);
    }
    return response;
  }
}
