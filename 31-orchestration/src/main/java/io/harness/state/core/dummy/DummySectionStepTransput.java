package io.harness.state.core.dummy;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.SweepingOutput;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@OwnedBy(CDC)
@Value
@Builder
public class DummySectionStepTransput implements SweepingOutput {
  Map<String, String> map;
}
