package io.harness.aws;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cli.CliResponse;
import io.harness.logging.LogCallback;
import io.harness.terraform.request.TerraformApplyCommandRequest;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;

@OwnedBy(HarnessTeam.CDP)
public interface AwsSamClient {
  @Nonnull
  public CliResponse runCommand(String command, long timeoutInMillis,
      Map<String, String> envVariables, String scriptDirectory, @Nonnull LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException;
}
