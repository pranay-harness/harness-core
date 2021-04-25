package io.harness.pcf.command;

import io.harness.pcf.model.PcfCliVersion;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class CommandArguments {
  private PcfCliVersion cliVersion;
  private String cliPath;
}
