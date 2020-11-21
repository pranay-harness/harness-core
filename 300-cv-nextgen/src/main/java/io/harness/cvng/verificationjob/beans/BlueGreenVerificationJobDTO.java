package io.harness.cvng.verificationjob.beans;

import io.harness.cvng.verificationjob.entities.BlueGreenVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@JsonTypeName("BLUE_GREEN")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BlueGreenVerificationJobDTO extends VerificationJobDTO {
  private Sensitivity sensitivity;
  private Integer trafficSplitPercentage;
  @Override
  public VerificationJob getVerificationJob() {
    BlueGreenVerificationJob blueGreenVerificationJob = new BlueGreenVerificationJob();
    blueGreenVerificationJob.setSensitivity(sensitivity);
    blueGreenVerificationJob.setTrafficSplitPercentage(trafficSplitPercentage);
    populateCommonFields(blueGreenVerificationJob);
    return blueGreenVerificationJob;
  }

  @Override
  public VerificationJobType getType() {
    return VerificationJobType.BLUE_GREEN;
  }
}
