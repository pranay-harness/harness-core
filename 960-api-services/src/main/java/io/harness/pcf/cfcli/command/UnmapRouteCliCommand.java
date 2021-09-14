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
import io.harness.pcf.cfcli.option.GlobalOptions;
import io.harness.pcf.cfcli.option.Option;
import io.harness.pcf.cfcli.option.Options;
import io.harness.pcf.model.CfCliVersion;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDP)
public class UnmapRouteCliCommand extends CfCliCommand {
  @Builder
  UnmapRouteCliCommand(CfCliVersion cliVersion, String cliPath, GlobalOptions globalOptions, List<String> arguments,
      UnmapRouteOptions options) {
    super(cliVersion, cliPath, globalOptions, CfCliCommandType.UNMAP_ROUTE, arguments, options);
  }

  @Value
  @Builder
  public static class UnmapRouteOptions implements Options {
    @Option(value = "--hostname") String hostname;
    @Option(value = "--path") String path;
    @Option(value = "--port") String port;
  }
}
