package io.harness.delegate.task.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cli.CliResponse;
import io.harness.terraform.request.TerraformExecuteStepRequest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@OwnedBy(CDP)
public interface TerraformBaseHelper {
  void downloadTfStateFile(String workspace, String accountId, String currentStateFileId, String scriptDirectory)
      throws IOException;
  List<String> parseOutput(String workspaceOutput);

  CliResponse executeTerraformApplyStep(TerraformExecuteStepRequest terraformExecuteStepRequest)
      throws InterruptedException, IOException, TimeoutException;

  CliResponse executeTerraformPlanStep(TerraformExecuteStepRequest terraformExecuteStepRequest)
      throws InterruptedException, IOException, TimeoutException;

  CliResponse executeTerraformDestroyStep(TerraformExecuteStepRequest terraformExecuteStepRequest)
      throws InterruptedException, IOException, TimeoutException;

  String resolveBaseDir(String accountId, String provisionerId);

  String resolveScriptDirectory(String workingDir, String scriptPath);
}
