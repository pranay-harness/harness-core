/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.terragrunt;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cli.CliResponse;
import io.harness.logging.LogCallback;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;

@OwnedBy(CDP)
public interface TerragruntClient {
  @Nonnull
  CliResponse init(TerragruntCliCommandRequestParams cliCommandRequestParams, @Nonnull LogCallback logCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse workspace(TerragruntCliCommandRequestParams cliCommandRequestParams, String workspaceCommand,
      String workspace, @Nonnull LogCallback logCallback) throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse runAllNewWorkspace(TerragruntCliCommandRequestParams cliCommandRequestParams, String workspace,
      @Nonnull LogCallback logCallback) throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse runAllSelectWorkspace(TerragruntCliCommandRequestParams cliCommandRequestParams, String workspace,
      @Nonnull LogCallback logCallback) throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse refresh(TerragruntCliCommandRequestParams cliCommandRequestParams, String targetArgs, String varParams,
      String uiLogs, @Nonnull LogCallback logCallback) throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse plan(TerragruntCliCommandRequestParams cliCommandRequestParams, String targetArgs, String varParams,
      String uiLogs, @Nonnull LogCallback logCallback) throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse planDestroy(TerragruntCliCommandRequestParams cliCommandRequestParams, String targetArgs,
      String varParams, String uiLogs, @Nonnull LogCallback logCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse destroy(TerragruntCliCommandRequestParams cliCommandRequestParams, String targetArgs, String varParams,
      String uiLogs, @Nonnull LogCallback logCallback) throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse applyDestroyTfPlan(TerragruntCliCommandRequestParams cliCommandRequestParams,
      @Nonnull LogCallback logCallback) throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse showJson(TerragruntCliCommandRequestParams cliCommandRequestParams, String planName,
      @Nonnull LogCallback logCallback) throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse runAllshowJson(TerragruntCliCommandRequestParams cliCommandRequestParams, String planName,
      @Nonnull LogCallback logCallback) throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse apply(TerragruntCliCommandRequestParams cliCommandRequestParams, @Nonnull LogCallback logCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse output(TerragruntCliCommandRequestParams cliCommandRequestParams, String tfOutputsFilePath,
      @Nonnull LogCallback logCallback) throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse runAllOutput(TerragruntCliCommandRequestParams cliCommandRequestParams, @Nonnull LogCallback logCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse runAllplan(TerragruntCliCommandRequestParams cliCommandRequestParams, String targetArgs, String varParams,
      String uiLogs, @Nonnull LogCallback logCallback) throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse runAllApply(TerragruntCliCommandRequestParams cliCommandRequestParams, String targetArgs,
      String varParams, String uiLogs, @Nonnull LogCallback logCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse runAllDestroy(TerragruntCliCommandRequestParams cliCommandRequestParams, String targetArgs,
      String varParams, String uiLogs, @Nonnull LogCallback logCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse runAllPlanDestroy(TerragruntCliCommandRequestParams cliCommandRequestParams, String targetArgs,
      String varParams, String uiLogs, @Nonnull LogCallback logCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse runAllInit(TerragruntCliCommandRequestParams cliCommandRequestParams, @Nonnull LogCallback logCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse runAllRefresh(TerragruntCliCommandRequestParams cliCommandRequestParams, String targetArgs,
      String varParams, String uiLogs, @Nonnull LogCallback logCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse workspaceList(String directory, long timeoutInMillis)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse terragruntInfo(TerragruntCliCommandRequestParams cliCommandRequestParams, LogCallback logCallback)
      throws InterruptedException, TimeoutException, IOException;

  @Nonnull
  CliResponse version(TerragruntCliCommandRequestParams cliCommandRequestParams, @Nonnull LogCallback logCallback)
      throws InterruptedException, TimeoutException, IOException;
}
