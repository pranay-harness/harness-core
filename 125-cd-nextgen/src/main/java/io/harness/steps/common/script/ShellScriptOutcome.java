package io.harness.steps.common.script;

import io.harness.pms.sdk.core.data.Outcome;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShellScriptOutcome implements Outcome {
  Map<String, String> outputVariables;

  @Override
  public String getType() {
    return "shellScriptOutcome";
  }
}
