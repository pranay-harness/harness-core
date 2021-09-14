/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
