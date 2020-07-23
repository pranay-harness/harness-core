package io.harness.yaml.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Parallel structure is special list of steps that can be executed in parallel.
 */
@Value
@Builder
public class Parallel implements ExecutionWrapper {
  @NotNull List<StepElement> sections;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public Parallel(List<StepElement> parallel) {
    this.sections = parallel;
  }
}
