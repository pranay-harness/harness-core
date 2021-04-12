package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

@Data
@JsonTypeName("Test")
@OwnedBy(HarnessTeam.CV)
public class TestVerificationJobSpec extends VerificationJobSpec {
  ParameterField<String> sensitivity;
  @Override
  public String getType() {
    return "Test";
  }
}
