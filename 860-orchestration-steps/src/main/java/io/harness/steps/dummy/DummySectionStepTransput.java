package io.harness.steps.dummy;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.SweepingOutput;

import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@TypeAlias("dummySectionStepTransput")
public class DummySectionStepTransput implements SweepingOutput {
  Map<String, String> map;
}
