package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Maps account Id to account key.
 */
@OwnedBy(PL)
@ParametersAreNonnullByDefault
public interface KeySource {
  /**
   * Returns the key for the given accountId, or {@code null} if it's not found.
   */
  @Nullable String fetchKey(String accountId);
}
