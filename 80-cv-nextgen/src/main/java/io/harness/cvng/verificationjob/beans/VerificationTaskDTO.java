package io.harness.cvng.verificationjob.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VerificationTaskDTO {
  String verificationJobIdentifier;
  long deploymentStartTimeMs;

  // TODO: add map to pass runtime values.
}
