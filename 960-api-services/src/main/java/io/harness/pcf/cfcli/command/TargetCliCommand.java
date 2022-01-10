/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
public class TargetCliCommand extends CfCliCommand {
  @Builder
  TargetCliCommand(CfCliVersion cliVersion, String cliPath, GlobalOptions globalOptions, List<String> arguments,
      TargetOptions options) {
    super(cliVersion, cliPath, globalOptions, CfCliCommandType.TARGET, arguments, options);
  }

  @Value
  @Builder
  public static class TargetOptions implements Options {
    @Option(value = "-o") String org;
    @Option(value = "-s") String space;
  }
}
