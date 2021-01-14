package io.harness.beans.steps;

import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("ciStepOutcome")
@JsonTypeName("ciStepOutcome")
public class CiStepOutcome implements Outcome {
  Map<String, String> outputVariables;

  @Override
  public String getType() {
    return "ciStepOutcome";
  }
}
