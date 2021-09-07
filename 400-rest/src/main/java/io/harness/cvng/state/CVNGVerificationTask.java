package io.harness.cvng.state;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CV)
public class CVNGVerificationTask {
  // keeping it here to avoid serialization issues. We can remove it after few months.
  public enum Status { IN_PROGRESS, DONE, TIMED_OUT }
}
