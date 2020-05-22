package io.harness.yaml.core.auxiliary.intfc;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.yaml.core.intfc.StepInfo;

/**
 * StepWrapper is an internal abstraction that represents steps that are contained
 * inside a graph or parallel section.
 * Instead of using {@link ExecutionSection} this interface represents only step
 * This way we can prevent graph holding another graph and similar unneeded nesting.
 */
@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
@JsonSubTypes({ @JsonSubTypes.Type(value = StepInfo.class, name = "step") })
public interface StepWrapper {}
