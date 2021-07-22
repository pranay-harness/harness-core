package io.harness.exceptions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class RecastMetadataException extends RuntimeException {
  public RecastMetadataException(final String message) {
    super(message);
  }
  public RecastMetadataException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
