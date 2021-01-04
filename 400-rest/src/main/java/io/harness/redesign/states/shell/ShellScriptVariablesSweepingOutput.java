package io.harness.redesign.states.shell;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@JsonTypeName("shellScriptVariablesSweepingOutput")
public class ShellScriptVariablesSweepingOutput implements SweepingOutput {
  Map<String, String> variables;

  @Override
  public String getType() {
    return "shellScriptVariablesSweepingOutput";
  }
}
