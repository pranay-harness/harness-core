package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@AllArgsConstructor
@TypeAlias("verifyStepParameters")
@OwnedBy(HarnessTeam.CV)
public class CVNGStepParameter implements StepParameters {
  String verificationJobIdentifier;
  ParameterField<String> serviceIdentifier;
  ParameterField<String> envIdentifier;
}
