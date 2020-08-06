package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Value
@Builder
public class TimeoutObtainment {
  @NotNull Dimension type;
  TimeoutParameters parameters;
}
