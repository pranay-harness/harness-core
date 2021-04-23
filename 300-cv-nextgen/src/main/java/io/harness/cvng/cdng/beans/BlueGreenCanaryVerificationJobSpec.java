package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;

import java.util.HashMap;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
@Data
@FieldNameConstants(innerTypeName = "BlueGreenCanaryVerificationJobSpecKeys")
@OwnedBy(HarnessTeam.CV)
@SuperBuilder
@NoArgsConstructor
public abstract class BlueGreenCanaryVerificationJobSpec extends VerificationJobSpec {
  ParameterField<String> sensitivity;
  ParameterField<String> trafficSplitPercentage;
  @Override
  protected void addToRuntimeParams(HashMap<String, String> runtimeParams) {
    if (sensitivity.getValue() != null) {
      runtimeParams.put(BlueGreenCanaryVerificationJobSpecKeys.sensitivity, sensitivity.getValue());
    }
    if (trafficSplitPercentage.getValue() != null) {
      runtimeParams.put(
          BlueGreenCanaryVerificationJobSpecKeys.trafficSplitPercentage, trafficSplitPercentage.getValue());
    }
  }
}
