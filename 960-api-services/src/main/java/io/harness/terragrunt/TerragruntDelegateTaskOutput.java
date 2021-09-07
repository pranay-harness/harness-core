package io.harness.terragrunt;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cli.CliResponse;

import lombok.Builder;
import lombok.Value;

@OwnedBy(CDP)
@Value
@Builder
public class TerragruntDelegateTaskOutput {
  PlanJsonLogOutputStream planJsonLogOutputStream;
  String terraformConfigFileDirectoryPath;
  CliResponse cliResponse;
}
