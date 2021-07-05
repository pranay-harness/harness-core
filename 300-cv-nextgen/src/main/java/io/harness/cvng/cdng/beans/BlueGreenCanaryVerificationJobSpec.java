package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.cvng.verificationjob.entities.CanaryBlueGreenVerificationJob.CanaryBlueGreenVerificationJobBuilder;
import io.harness.cvng.verificationjob.entities.CanaryVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob.VerificationJobBuilder;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
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
  @NotNull
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH, value = "Possible values: [Low, Medium, High]")
  ParameterField<String> sensitivity;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH, value = "Example: 50, You can put max upto 50.")
  ParameterField<String> trafficSplitPercentage;

  @Override
  public VerificationJobBuilder verificationJobBuilder() {
    CanaryBlueGreenVerificationJobBuilder canaryVerificationJobBuilder = CanaryVerificationJob.builder().sensitivity(
        VerificationJob.RuntimeParameter.builder().isRuntimeParam(false).value(getSensitivity().getValue()).build());
    if (getTrafficSplitPercentage().getValue() != null) {
      canaryVerificationJobBuilder =
          canaryVerificationJobBuilder.trafficSplitPercentageV2(VerificationJob.RuntimeParameter.builder()
                                                                    .isRuntimeParam(false)
                                                                    .value(getTrafficSplitPercentage().getValue())
                                                                    .build());
    }
    return canaryVerificationJobBuilder;
  }

  @Override
  protected void validateParams() {}
}
