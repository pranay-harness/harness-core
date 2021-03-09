package io.harness.terraform;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.terraform.TerraformConstants.DEFAULT_TERRAFORM_COMMAND_TIMEOUT;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import io.harness.cli.CliHelper;
import io.harness.cli.CliResponse;
import io.harness.logging.LogCallback;
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

@Slf4j
@Singleton
public class TerraformClientImpl implements TerraformClient {
  @Inject CliHelper cliHelper;

  @Override
  public CliResponse init(TerraformInitCommandRequest terraformInitCommandRequest, Map<String, String> envVariables,
      String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    String command = format("terraform init -input=false %s",
        isEmpty(terraformInitCommandRequest.getTfBackendConfigsFilePath())
            ? EMPTY
            : format("-backend-config=%s", terraformInitCommandRequest.getTfBackendConfigsFilePath()));
    return executeTerraformCLICommand(command, envVariables, scriptDirectory, executionLogCallback);
  }

  @Override
  public CliResponse destroy(TerraformDestroyCommandRequest terraformDestroyCommandRequest,
      Map<String, String> envVariables, String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    String command = format("terraform destroy -force %s %s",
        TerraformHelperUtils.generateCommandFlagsString(terraformDestroyCommandRequest.getTargets(), "-target="),
        TerraformHelperUtils.generateCommandFlagsString(
            terraformDestroyCommandRequest.getVarFilePaths(), "-var-file="));
    return executeTerraformCLICommand(command, envVariables, scriptDirectory, executionLogCallback);
  }

  @Override
  public CliResponse plan(TerraformPlanCommandRequest terraformPlanCommandRequest, Map<String, String> envVariables,
      String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    String command;
    if (terraformPlanCommandRequest.isDestroySet()) {
      command = format("terraform plan -input=false -destroy -out=tfdestroyplan %s %s",
          TerraformHelperUtils.generateCommandFlagsString(terraformPlanCommandRequest.getTargets(), "-target="),
          TerraformHelperUtils.generateCommandFlagsString(terraformPlanCommandRequest.getVarFilePaths(), "-var-file="));
    } else {
      command = format("terraform plan -input=false -out=tfplan %s %s",
          TerraformHelperUtils.generateCommandFlagsString(terraformPlanCommandRequest.getTargets(), "-target="),
          TerraformHelperUtils.generateCommandFlagsString(terraformPlanCommandRequest.getVarFilePaths(), "-var-file="));
    }
    return executeTerraformCLICommand(command, envVariables, scriptDirectory, executionLogCallback);
  }

  @Override
  public CliResponse refresh(TerraformRefreshCommandRequest terraformRefreshCommandRequest,
      Map<String, String> envVariables, String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    String command = "terraform refresh -input=false "
        + TerraformHelperUtils.generateCommandFlagsString(terraformRefreshCommandRequest.getTargets(), "-target=")
        + TerraformHelperUtils.generateCommandFlagsString(
            terraformRefreshCommandRequest.getVarFilePaths(), "-var-file=");
    return executeTerraformCLICommand(command, envVariables, scriptDirectory, executionLogCallback);
  }

  @Override
  public CliResponse apply(TerraformApplyCommandRequest terraformApplyCommandRequest, Map<String, String> envVariables,
      String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    return executeTerraformCLICommand(
        "terraform apply -input=false tfplan", envVariables, scriptDirectory, executionLogCallback);
  }

  @Override
  public CliResponse workspace(String workspace, boolean isNew, Map<String, String> envVariables,
      String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    String command = isNew ? "terraform workspace NEW " + workspace : "terraform workspace SELECT " + workspace;
    return executeTerraformCLICommand(command, envVariables, scriptDirectory, executionLogCallback);
  }

  @Override
  public CliResponse getWorkspacelist(Map<String, String> envVariables, String scriptDirectory,
      @Nonnull LogCallback executionLogCallback) throws InterruptedException, TimeoutException, IOException {
    return executeTerraformCLICommand("terraform workspace list", envVariables, scriptDirectory, executionLogCallback);
  }

  @Override
  public CliResponse show(String planName, Map<String, String> envVariables, String scriptDirectory,
      @Nonnull LogCallback executionLogCallback) throws InterruptedException, TimeoutException, IOException {
    String command = "terraform show -json " + planName;
    return executeTerraformCLICommand(command, envVariables, scriptDirectory, executionLogCallback);
  }

  @Override
  public CliResponse output(String tfOutputsFile, Map<String, String> envVariables, String scriptDirectory,
      @Nonnull LogCallback executionLogCallback) throws InterruptedException, TimeoutException, IOException {
    String command = "terraform show -json > " + tfOutputsFile;
    return executeTerraformCLICommand(command, envVariables, scriptDirectory, executionLogCallback);
  }

  @VisibleForTesting
  CliResponse executeTerraformCLICommand(String command, Map<String, String> envVariables, String scriptDirectory,
      LogCallback executionLogCallBack) throws IOException, InterruptedException, TimeoutException {
    return cliHelper.executeCliCommand(
        command, DEFAULT_TERRAFORM_COMMAND_TIMEOUT, envVariables, scriptDirectory, executionLogCallBack);
  }
}
