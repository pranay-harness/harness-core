package io.harness.steps.common.script;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.Outcome;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDC)
public class ShellScriptOutcome implements Outcome {
  Map<String, String> outputVariables;
}
