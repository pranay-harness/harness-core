package io.harness.cvng.analysis.beans;

import io.harness.cvng.beans.job.VerificationJobType;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BlueGreenAdditionalInfo extends CanaryBlueGreenAdditionalInfo {
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.BLUE_GREEN;
  }

  @Override
  public void setFieldNames() {
    this.setCanaryInstancesLabel("green");
    this.setPrimaryInstancesLabel("blue");
  }
}
