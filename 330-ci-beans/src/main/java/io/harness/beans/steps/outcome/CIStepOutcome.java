package io.harness.beans.steps.outcome;

import static io.harness.beans.steps.outcome.CIOutcomeNames.CI_STEP_OUTCOME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias(CI_STEP_OUTCOME)
@JsonTypeName(CI_STEP_OUTCOME)
@OwnedBy(HarnessTeam.CI)
public class CIStepOutcome implements Outcome {
  Map<String, String> outputVariables;

  @Override
  public String getType() {
    return CI_STEP_OUTCOME;
  }
}
