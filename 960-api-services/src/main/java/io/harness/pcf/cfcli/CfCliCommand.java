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
