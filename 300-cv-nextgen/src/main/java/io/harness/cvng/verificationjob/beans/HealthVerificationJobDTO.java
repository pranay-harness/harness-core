package io.harness.cvng.verificationjob.beans;

import io.harness.cvng.verificationjob.entities.HealthVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@JsonTypeName("HEALTH")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HealthVerificationJobDTO extends VerificationJobDTO {
  @Override
  public VerificationJob getVerificationJob() {
    HealthVerificationJob healthVerificationJob = new HealthVerificationJob();
    populateCommonFields(healthVerificationJob);
    return healthVerificationJob;
  }

  @Override
  public VerificationJobType getType() {
    return VerificationJobType.HEALTH;
  }
}
