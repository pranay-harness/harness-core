/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pcf.cfcli.command;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pcf.cfcli.CfCliCommand;
import io.harness.pcf.cfcli.CfCliCommandType;
import io.harness.pcf.cfcli.option.Flag;
import io.harness.pcf.cfcli.option.GlobalOptions;
import io.harness.pcf.cfcli.option.Options;
import io.harness.pcf.model.CfCliVersion;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDP)
public class ApiCliCommand extends CfCliCommand {
  @Builder
  ApiCliCommand(CfCliVersion cliVersion, String cliPath, GlobalOptions globalOptions, List<String> arguments,
      ApiOptions options) {
    super(cliVersion, cliPath, globalOptions, CfCliCommandType.API, arguments, options);
  }

  @Value
  @Builder
  public static class ApiOptions implements Options {
    @Flag(value = "--unset") boolean unset;
    @Flag(value = "--skip-ssl-validation") boolean skipSslValidation;
  }
}
