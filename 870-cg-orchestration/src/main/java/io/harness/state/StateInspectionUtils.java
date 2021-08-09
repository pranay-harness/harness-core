package io.harness.state;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.time.Duration;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class StateInspectionUtils {
  public static final Duration TTL = Duration.ofDays(184);
}
