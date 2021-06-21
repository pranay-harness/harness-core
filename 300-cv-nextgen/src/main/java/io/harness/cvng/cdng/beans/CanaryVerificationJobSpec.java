package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.verificationjob.entities.BlueGreenVerificationJob;
import io.harness.cvng.verificationjob.entities.CanaryVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@JsonTypeName("Canary")
@OwnedBy(HarnessTeam.CV)
@SuperBuilder
@NoArgsConstructor
public class CanaryVerificationJobSpec extends BlueGreenCanaryVerificationJobSpec {
  @Override
  public String getType() {
    return "Canary";
  }

  @Override
  public VerificationJob.VerificationJobBuilder verificationJobBuilder() {
    return CanaryVerificationJob.builder()
        .sensitivity(
            VerificationJob.RuntimeParameter.builder().isRuntimeParam(false).value(getSensitivity().getValue()).build())
        .trafficSplitPercentageV2(VerificationJob.RuntimeParameter.builder()
                                      .isRuntimeParam(false)
                                      .value(getTrafficSplitPercentage().getValue())
                                      .build());
  }
}
