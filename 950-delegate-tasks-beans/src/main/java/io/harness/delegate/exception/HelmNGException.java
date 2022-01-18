package io.harness.delegate.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DataException;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@OwnedBy(CDP)
@EqualsAndHashCode(callSuper = false)
public class HelmNGException extends DataException {
  int prevReleaseVersion;

  public HelmNGException(int prevReleaseVersion, Throwable cause) {
    super(cause);
    this.prevReleaseVersion = prevReleaseVersion;
  }
}
