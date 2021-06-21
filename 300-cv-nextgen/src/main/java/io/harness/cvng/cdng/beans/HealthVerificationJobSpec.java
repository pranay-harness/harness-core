package io.harness.cvng.cdng.beans;

import io.harness.cvng.verificationjob.entities.HealthVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@JsonTypeName("Health")
@SuperBuilder
@NoArgsConstructor
public class HealthVerificationJobSpec extends VerificationJobSpec {
  @Override
  public String getType() {
    return "Health";
  }

  @Override
  public VerificationJob.VerificationJobBuilder verificationJobBuilder() {
    return HealthVerificationJob.builder();
  }

  @Override
  protected void addToRuntimeParams(HashMap<String, String> runtimeParams) {}
}
