package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.verificationjob.entities.BlueGreenVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob.RuntimeParameter;
import io.harness.cvng.verificationjob.entities.VerificationJob.VerificationJobBuilder;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@JsonTypeName("Bluegreen")
@OwnedBy(HarnessTeam.CV)
@SuperBuilder
@NoArgsConstructor
public class BlueGreenVerificationJobSpec extends BlueGreenCanaryVerificationJobSpec {
  @Override
  public String getType() {
    return "Bluegreen";
  }

  @Override
  public VerificationJobBuilder verificationJobBuilder() {
    return BlueGreenVerificationJob.builder()
        .sensitivity(RuntimeParameter.builder().isRuntimeParam(false).value(getSensitivity().getValue()).build())
        .trafficSplitPercentageV2(
            RuntimeParameter.builder().isRuntimeParam(false).value(getTrafficSplitPercentage().getValue()).build());
  }
}
