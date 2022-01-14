package io.harness.cvng.verificationjob.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TestVerificationBaselineExecutionDTO {
  String verificationJobInstanceId;
  long createdAt;
}
