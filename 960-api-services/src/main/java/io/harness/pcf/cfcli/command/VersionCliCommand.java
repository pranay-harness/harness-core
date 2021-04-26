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
import lombok.EqualsAndHashCode;
import lombok.Value;

@OwnedBy(HarnessTeam.CDP)
public class VersionCliCommand extends CfCliCommand {
  @Builder
  VersionCliCommand(CfCliVersion cliVersion, String cliPath, GlobalOptions globalOptions, List<String> arguments,
      VersionOptions options) {
    super(cliVersion, cliPath, globalOptions, CfCliCommandType.VERSION, arguments, options);
  }

  @Value
  @Builder
  @EqualsAndHashCode(callSuper = true)
  public static class VersionOptions extends Options {
    @Flag(value = "--version") boolean version;
  }
}
