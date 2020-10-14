package io.harness.cvng.core.beans;

import io.harness.cvng.verificationjob.beans.AdditionalInfo;
import io.harness.cvng.verificationjob.beans.VerificationJobType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoadTestAdditionalInfo extends AdditionalInfo {
  String baselineDeploymentTag;
  long baselineStartTime;
  String currentDeploymentTag;
  long currentStartTime;

  @Override
  public VerificationJobType getType() {
    return VerificationJobType.TEST;
  }
}
