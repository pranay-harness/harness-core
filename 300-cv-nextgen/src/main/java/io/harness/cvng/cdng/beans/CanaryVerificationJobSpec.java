package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

@Data
@JsonTypeName("Canary")
@OwnedBy(HarnessTeam.CV)
public class CanaryVerificationJobSpec extends VerificationJobSpec {
  @Override
  public String getType() {
    return "Canary";
  }
}
