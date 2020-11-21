package io.harness.yaml.core.auxiliary.intfc;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.StepGroupElement;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * ExecutionWrapper is abstraction that represents steps or step collections
 * Steps list can have steps, graphs and parallel section. This interface is
 * mainly used to be able to store those objects into single list of steps.
 */
@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
@JsonSubTypes({
  @JsonSubTypes.Type(value = StepElement.class, name = "step")
  , @JsonSubTypes.Type(value = ParallelStepElement.class, name = "parallel"),
      @JsonSubTypes.Type(value = StepGroupElement.class, name = "stepGroup")
})
public interface ExecutionWrapper {}
