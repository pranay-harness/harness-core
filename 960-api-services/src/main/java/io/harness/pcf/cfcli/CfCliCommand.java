/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pcf.cfcli;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pcf.cfcli.option.GlobalOptions;
import io.harness.pcf.cfcli.option.Options;
import io.harness.pcf.model.CfCliVersion;

import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@OwnedBy(HarnessTeam.CDP)
public abstract class CfCliCommand {
  private CfCliCommand() {}

  CfCliVersion cliVersion;
  String cliPath;
  GlobalOptions globalOptions;
  CfCliCommandType commandType;
  List<String> arguments;
  Options options;

  public String getCommand() {
    return CfCliCommandBuilder.buildCommand(this);
  }
}
