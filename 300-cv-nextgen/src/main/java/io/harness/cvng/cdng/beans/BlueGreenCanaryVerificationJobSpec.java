package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;
@OwnedBy(HarnessTeam.CV)
public abstract class BlueGreenCanaryVerificationJobSpec extends VerificationJobSpec {
  ParameterField<String> sensitivity;
  ParameterField<String> trafficSplitPercentage;
}
