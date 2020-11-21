package io.harness.redesign.states.shell;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.SweepingOutput;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Redesign
@Value
@Builder
public class ShellScriptVariablesSweepingOutput implements SweepingOutput {
  Map<String, String> variables;
}
