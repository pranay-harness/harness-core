package io.harness.cvng.verificationjob.entities;

import io.harness.cvng.verificationjob.beans.HealthVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(innerTypeName = "HealthVerificationJobKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HealthVerificationJob extends VerificationJob {
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.HEALTH;
  }

  @Override
  public VerificationJobDTO getVerificationJobDTO() {
    HealthVerificationJobDTO healthVerificationJobDTO = new HealthVerificationJobDTO();
    populateCommonFields(healthVerificationJobDTO);
    return healthVerificationJobDTO;
  }

  @Override
  protected void validateParams() {}
}
