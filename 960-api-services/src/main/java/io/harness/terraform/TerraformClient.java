/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
  CliResponse init(TerraformInitCommandRequest terraformInitCommandRequest, long timeoutInMillis,
      Map<String, String> envVariables, String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse destroy(TerraformDestroyCommandRequest terraformDestroyCommandRequest, long timeoutInMillis,
      Map<String, String> envVariables, String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse refresh(TerraformRefreshCommandRequest terraformRefreshCommandRequest, long timeoutInMillis,
      Map<String, String> envVariables, String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse plan(TerraformPlanCommandRequest terraformPlanCommandRequest, long timeoutInMillis,
      Map<String, String> envVariables, String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse apply(TerraformApplyCommandRequest terraformApplyCommandRequest, long timeoutInMillis,
      Map<String, String> envVariables, String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse workspace(String workspace, boolean isNew, long timeoutInMillis, Map<String, String> envVariables,
      String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse getWorkspaceList(long timeoutInMillis, Map<String, String> envVariables, String scriptDirectory,
      @Nonnull LogCallback executionLogCallback) throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse show(String planName, long timeoutInMillis, Map<String, String> envVariables, String scriptDirectory,
      @Nonnull LogCallback executionLogCallback, @Nonnull PlanJsonLogOutputStream planJsonLogOutputStream)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse output(String tfOutputsFile, long timeoutInMillis, Map<String, String> envVariables,
      String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException;
}
