package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
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
  protected void addToRuntimeParams(HashMap<String, String> runtimeParams) {
    if (sensitivity.getValue() != null) {
      runtimeParams.put(BlueGreenCanaryVerificationJobSpecKeys.sensitivity, sensitivity.getValue());
    }
    if (trafficSplitPercentage != null && trafficSplitPercentage.getValue() != null) {
      runtimeParams.put(
          BlueGreenCanaryVerificationJobSpecKeys.trafficSplitPercentage, trafficSplitPercentage.getValue());
    }
  }
}
