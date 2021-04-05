package io.harness.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cli.CliResponse;
import io.harness.logging.LogCallback;
import io.harness.logging.PlanJsonLogOutputStream;
import io.harness.terraform.request.TerraformApplyCommandRequest;
import io.harness.terraform.request.TerraformDestroyCommandRequest;
import io.harness.terraform.request.TerraformInitCommandRequest;
import io.harness.terraform.request.TerraformPlanCommandRequest;
import io.harness.terraform.request.TerraformRefreshCommandRequest;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;

@OwnedBy(CDP)
public interface TerraformClient {
  @Nonnull
  CliResponse init(TerraformInitCommandRequest terraformInitCommandRequest, Map<String, String> envVariables,
      String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse destroy(TerraformDestroyCommandRequest terraformDestroyCommandRequest, Map<String, String> envVariables,
      String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse refresh(TerraformRefreshCommandRequest terraformRefreshCommandRequest, Map<String, String> envVariables,
      String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse plan(TerraformPlanCommandRequest terraformPlanCommandRequest, Map<String, String> envVariables,
      String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse apply(TerraformApplyCommandRequest terraformApplyCommandRequest, Map<String, String> envVariables,
      String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse workspace(String workspace, boolean isNew, Map<String, String> envVariables, String scriptDirectory,
      @Nonnull LogCallback executionLogCallback) throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse getWorkspaceList(Map<String, String> envVariables, String scriptDirectory,
      @Nonnull LogCallback executionLogCallback) throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse show(String planName, Map<String, String> envVariables, String scriptDirectory,
      @Nonnull LogCallback executionLogCallback, @Nonnull PlanJsonLogOutputStream planJsonLogOutputStream)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse output(String tfOutputsFile, Map<String, String> envVariables, String scriptDirectory,
      @Nonnull LogCallback executionLogCallback) throws InterruptedException, TimeoutException, IOException;
}