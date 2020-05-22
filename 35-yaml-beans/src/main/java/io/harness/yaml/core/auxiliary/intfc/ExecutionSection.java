package io.harness.yaml.core.auxiliary.intfc;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.yaml.core.Parallel;
import io.harness.yaml.core.intfc.StepInfo;

/**
 * ExecutionSection is abstraction that represents steps or step collections
 * Steps list can have steps, graphs and parallel section. This interface is
 * mainly used to be able to store those objects into single list of steps.
 */
@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
@JsonSubTypes({
  @JsonSubTypes.Type(value = StepInfo.class, name = "step")
  , @JsonSubTypes.Type(value = Parallel.class, name = "parallel")
})
public interface ExecutionSection {}
