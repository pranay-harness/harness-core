package io.harness.yaml.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Graph structure is special list of steps that can be represented in form of dependencies.
 * Each step will have a list of other step identifiers that it depends on.
 */
@Value
@Builder
public class Graph implements ExecutionWrapper {
  @NotNull List<StepElement> sections;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public Graph(List<StepElement> graph) {
    this.sections = graph;
  }
}
