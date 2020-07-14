package io.harness.yaml.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Execution represents list of steps that can be used within stage
 * Steps can be also represented in special list of steps called parallel or graph
 * {@link ExecutionElement} is used to represent this dynamic property where object in this list
 * can be another list of steps or single step
 */
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutionElement {
  @NotNull List<ExecutionWrapper> steps;
  List<ExecutionWrapper> rollbackSteps;
}
