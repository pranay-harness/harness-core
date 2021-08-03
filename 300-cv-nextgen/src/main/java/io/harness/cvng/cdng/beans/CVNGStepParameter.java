package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ParameterField;
import io.harness.cvng.verificationjob.entities.VerificationJob.VerificationJobBuilder;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import com.google.common.base.Preconditions;
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
  ParameterField<String> serviceIdentifier;
  ParameterField<String> envIdentifier;
  ParameterField<String> deploymentTag;
  VerificationJobBuilder verificationJobBuilder;

  public String getServiceIdentifier() {
    Preconditions.checkNotNull(serviceIdentifier.getValue());
    return serviceIdentifier.getValue();
  }
  public String getEnvIdentifier() {
    Preconditions.checkNotNull(envIdentifier.getValue());
    return envIdentifier.getValue();
  }
}
