package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.WithIdentifier;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.yaml.core.StepSpecType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;

@ApiModel(subTypes = {CVNGStepInfo.class})
@OwnedBy(HarnessTeam.CV)
public interface CVStepInfoBase extends StepParameters, WithIdentifier, StepSpecType {
  @JsonIgnore String getIdentifier();
}
