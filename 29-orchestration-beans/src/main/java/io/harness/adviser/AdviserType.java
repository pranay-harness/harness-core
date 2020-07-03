package io.harness.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.RegistryKey;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
@EqualsAndHashCode
public class AdviserType implements RegistryKey {
  // Provided From the orchestration layer system advisers

  // SUCCESS
  public static final String ON_SUCCESS = "ON_SUCCESS";

  // FAILURES
  public static final String ON_FAIL = "ON_FAIL";
  public static final String IGNORE = "IGNORE";
  public static final String RETRY = "RETRY";

  // Interrupts
  public static final String ABORT = "ABORT";
  public static final String PAUSE = "PAUSE";
  public static final String RESUME = "RESUME";

  @NonNull String type;
}
