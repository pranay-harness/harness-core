package io.harness.pcf.cfcli.command;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pcf.cfcli.CfCliCommand;
import io.harness.pcf.cfcli.CfCliCommandType;
import io.harness.pcf.cfcli.option.GlobalOptions;
import io.harness.pcf.model.CfCliVersion;

import java.util.List;
import lombok.Builder;

@OwnedBy(HarnessTeam.CDP)
public class PluginsCliCommand extends CfCliCommand {
  @Builder
  PluginsCliCommand(CfCliVersion cliVersion, String cliPath, GlobalOptions globalOptions, List<String> arguments) {
    super(cliVersion, cliPath, globalOptions, CfCliCommandType.PLUGINS, arguments, null);
  }
}
