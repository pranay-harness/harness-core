package io.harness.cvng.verificationjob.beans;

import io.harness.cvng.verificationjob.entities.TestVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@JsonTypeName("TEST")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TestVerificationJobDTO extends VerificationJobDTO {
  private String sensitivity;
  private String baselineVerificationJobInstanceId;
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.TEST;
  }

  public VerificationJob getVerificationJob() {
    TestVerificationJob testVerificationJob = new TestVerificationJob();
    populateCommonFields(testVerificationJob);
    testVerificationJob.setSensitivity(sensitivity, isRuntimeParam(sensitivity));
    testVerificationJob.setBaselineVerificationJobInstanceId(baselineVerificationJobInstanceId);
    return testVerificationJob;
  }
}
