package io.harness.yaml.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Execution represents list of steps that can be used within stage
 * Steps can be also represented in special list of steps called parallel or graph
 * {@link ExecutionElement} is used to represent this dynamic property where object in this list
 * can be another list of steps or single step
 */
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ExecutionElement {
  @NotEmpty private final List<ExecutionWrapper> steps;
  private final List<ExecutionWrapper> rollbackSteps;

  @ConstructorProperties({"steps", "rollbackSteps"})
  public ExecutionElement(List<ExecutionWrapper> steps, List<ExecutionWrapper> rollbackSteps) {
    this.steps = Optional.ofNullable(steps).orElse(new ArrayList<>());
    this.rollbackSteps = Optional.ofNullable(rollbackSteps).orElse(new ArrayList<>());
  }
}
