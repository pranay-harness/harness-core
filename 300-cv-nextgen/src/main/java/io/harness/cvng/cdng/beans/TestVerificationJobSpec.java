package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.cvng.verificationjob.entities.TestVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob.RuntimeParameter;
import io.harness.cvng.verificationjob.entities.VerificationJob.VerificationJobBuilder;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@JsonTypeName("LoadTest")
@OwnedBy(HarnessTeam.CV)
@SuperBuilder
@NoArgsConstructor
public class TestVerificationJobSpec extends VerificationJobSpec {
  @NotNull
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH, value = "Possible values: [Low, Medium, High]")
  ParameterField<String> sensitivity;
  @Override
  public String getType() {
    return "LoadTest";
  }

  @Override
  public VerificationJobBuilder verificationJobBuilder() {
    return TestVerificationJob.builder().sensitivity(
        RuntimeParameter.builder().isRuntimeParam(false).value(sensitivity.getValue()).build());
  }

  @Override
  protected void validateParams() {}
}
