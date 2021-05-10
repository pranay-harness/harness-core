package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.cdng.beans.BlueGreenCanaryVerificationJobSpec.BlueGreenCanaryVerificationJobSpecKeys;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@JsonTypeName("Test")
@OwnedBy(HarnessTeam.CV)
@SuperBuilder
@NoArgsConstructor
public class TestVerificationJobSpec extends VerificationJobSpec {
  ParameterField<String> sensitivity;
  @Override
  public String getType() {
    return "Test";
  }

  @Override
  protected void addToRuntimeParams(HashMap<String, String> runtimeParams) {
    if (sensitivity.getValue() != null) {
      runtimeParams.put(BlueGreenCanaryVerificationJobSpecKeys.sensitivity, sensitivity.getValue());
    }
  }
}
