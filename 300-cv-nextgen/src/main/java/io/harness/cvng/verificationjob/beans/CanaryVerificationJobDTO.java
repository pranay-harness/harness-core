package io.harness.cvng.verificationjob.beans;

import io.harness.cvng.verificationjob.entities.CanaryVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@JsonTypeName("CANARY")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CanaryVerificationJobDTO extends VerificationJobDTO {
  private String sensitivity;
  private Integer trafficSplitPercentage;
  @Override
  public VerificationJob getVerificationJob() {
    CanaryVerificationJob canaryVerificationJob = new CanaryVerificationJob();
    canaryVerificationJob.setSensitivity(sensitivity, isRuntimeParam(sensitivity));
    canaryVerificationJob.setTrafficSplitPercentage(trafficSplitPercentage);
    populateCommonFields(canaryVerificationJob);
    return canaryVerificationJob;
  }

  @Override
  public VerificationJobType getType() {
    return VerificationJobType.CANARY;
  }
}
