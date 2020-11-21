package io.harness.cvng.core.beans;

import io.harness.cvng.verificationjob.beans.AdditionalInfo;
import io.harness.cvng.verificationjob.beans.VerificationJobType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoadTestAdditionalInfo extends AdditionalInfo {
  String baselineDeploymentTag;
  Long baselineStartTime;
  String currentDeploymentTag;
  long currentStartTime;

  public boolean isBaselineRun() {
    if (baselineDeploymentTag == null) {
      return true;
    }
    return false;
  }
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.TEST;
  }
}
