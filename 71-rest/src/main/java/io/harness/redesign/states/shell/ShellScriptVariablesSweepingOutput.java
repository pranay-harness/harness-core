package io.harness.redesign.states.shell;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@OwnedBy(CDC)
@Redesign
@Value
@Builder
public class ShellScriptVariablesSweepingOutput implements SweepingOutput {
  Map<String, String> variables;
}
