package io.harness.pcf.command;

import io.harness.pcf.model.CfCliVersion;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CommandArguments {
  CfCliVersion cliVersion;
  String cliPath;
}
