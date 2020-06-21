package io.harness.state;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.experimental.UtilityClass;

import java.time.Duration;

@OwnedBy(CDC)
@UtilityClass
public class StateInspectionUtils {
  public static final Duration TTL = Duration.ofDays(184);
}
