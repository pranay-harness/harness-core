package io.harness.yaml.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.harness.yaml.core.auxiliary.intfc.ExecutionSection;
import io.harness.yaml.core.auxiliary.intfc.StepWrapper;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Parallel structure is special list of steps that can be executed in parallel.
 */
@Value
@Builder
public class Parallel implements ExecutionSection {
  @NotNull List<StepWrapper> sections;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public Parallel(List<StepWrapper> parallel) {
    this.sections = parallel;
  }
}
