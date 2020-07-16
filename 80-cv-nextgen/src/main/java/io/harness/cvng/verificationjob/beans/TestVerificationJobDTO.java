package io.harness.cvng.verificationjob.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cvng.verificationjob.entities.TestVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@JsonTypeName("TEST")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TestVerificationJobDTO extends VerificationJobDTO {
  private Sensitivity sensitivity;
  private String baselineVerificationTaskIdentifier; // Define it in a better way once verification task is implemented.
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.TEST;
  }

  public VerificationJob getVerificationJob() {
    TestVerificationJob testVerificationJob = new TestVerificationJob();
    populateCommonFields(testVerificationJob);
    testVerificationJob.setSensitivity(sensitivity);
    testVerificationJob.setBaseLineVerificationTaskIdentifier(baselineVerificationTaskIdentifier);
    return testVerificationJob;
  }
}
